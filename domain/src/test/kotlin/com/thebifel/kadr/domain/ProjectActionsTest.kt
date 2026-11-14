package com.thebifel.kadr.domain

import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.domain.port.*
import com.thebifel.kadr.domain.usecase.*
import java.io.OutputStream
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test

class ProjectActionsTest {
    private class Image : FrameImage {
        var closed = false
        override val width = 320
        override val height = 160

        override fun writePng(output: OutputStream) = true

        override fun close() {
            closed = true
        }
    }

    private class MemoryRepository : WidgetRepository {
        val projects = mutableMapOf<String, Project>()
        val frames = mutableMapOf<String, FrameState>()
        val ids = mutableMapOf<Int, String>()

        override fun changes() = MutableStateFlow(0L)

        override fun all() = projects.values.toList()

        override fun get(id: String) = projects[id]

        override fun initialize(starter: () -> Project) {
            ensure(starter)
        }

        override fun ensure(starter: () -> Project) = all().firstOrNull() ?: starter().also(::save)

        override fun save(project: Project) {
            WidgetPolicy.validate(project)
            projects[project.id] = project
        }

        override fun saveNextRevision(draft: Project) =
            draft.copy(revision = checkNotNull(get(draft.id)).revision + 1).also(::save)

        override fun result(id: String) = frames[id] ?: FrameState()

        override fun loadFrame(id: String): FrameImage? = null

        override fun publish(project: Project, image: FrameImage, effectiveDark: Boolean): Boolean {
            if (get(project.id)?.revision != project.revision) return false
            frames[project.id] =
                FrameState(
                    file = "new.png",
                    success = 2,
                    width = image.width,
                    height = image.height,
                    dark = effectiveDark,
                    revision = project.revision,
                )
            return true
        }

        override fun fail(id: String, message: String, expectedRevision: Long?) {
            val project = get(id) ?: return
            if (expectedRevision != null && expectedRevision != project.revision) return
            frames[id] = result(id).copy(error = message, pending = 0)
        }

        override fun markPending(id: String) {
            if (get(id) != null) frames[id] = result(id).copy(pending = 1)
        }

        override fun bind(widgetId: Int, projectId: String) {
            ids[widgetId] = projectId
        }

        override fun binding(widgetId: Int) = ids[widgetId]

        override fun bindings() = ids.toMap()

        override fun unbind(widgetId: Int) {
            ids.remove(widgetId)
        }

        override fun remap(old: IntArray, new: IntArray) {
            old.zip(new.toList()).forEach { (a, b) -> ids.remove(a)?.let { ids[b] = it } }
        }

        override fun cloneForBinding(source: Project) =
            if (source.id in ids.values) source.copy(id = "clone", revision = 1).also(::save)
            else source

        override fun delete(id: String): IntArray {
            val affected = ids.filterValues { it == id }.keys.toIntArray()
            affected.forEach(ids::remove)
            projects.remove(id)
            frames.remove(id)
            return affected
        }

        override fun signalConfigurationChange() = Unit
    }

    private class Scheduler : RefreshScheduler {
        val configured = mutableListOf<String>()
        val requested = mutableListOf<Pair<String, Boolean>>()
        val cancelled = mutableListOf<String>()

        override fun configure(project: Project) {
            configured += project.id
        }

        override fun enqueue(project: Project, replace: Boolean) {
            requested += project.id to replace
        }

        override fun cancel(projectId: String) {
            cancelled += projectId
        }
    }

    private class Host : WidgetHost {
        val updated = mutableListOf<String>()
        var removed = intArrayOf()

        override fun size(projectId: String) = RenderSize()

        override fun updateProject(projectId: String) {
            updated += projectId
        }

        override fun updateWidgets(ids: IntArray) {
            removed = ids
        }
    }

    private val repository = MemoryRepository()
    private val scheduler = Scheduler()
    private val host = Host()
    private val request = RequestRefresh(repository, scheduler, host)
    private val project = Project(id = "first", name = "Example", html = "<p>Original</p>")
    private val cached = FrameState(file = "old.png", success = 1, width = 100, height = 50)
    private val environment = EnvironmentProvider { RenderEnvironment(it.resolveDark(true), "en") }

    @Test
    fun saveKeepsIdentityAdvancesRevisionAndRequestsReplacement() {
        repository.save(project)
        repository.frames[project.id] = cached
        val saved = SaveProject(repository, scheduler, request)(project.copy(html = "<p>New</p>"))
        assertEquals(project.id, saved.id)
        assertEquals(2L, saved.revision)
        assertEquals(listOf(project.id), scheduler.configured)
        assertEquals(listOf(project.id to true), scheduler.requested)
        assertEquals(cached.file, repository.result(project.id).file)
        assertEquals(cached.success, repository.result(project.id).success)
        assertTrue(repository.result(project.id).pending > 0)
    }

    @Test
    fun deleteRemovesOnlyItsBindingsAndCancelsBothSchedulingPathsThroughPort() {
        repository.save(project)
        repository.save(project.copy(id = "second"))
        repository.bind(10, "first")
        repository.bind(20, "second")
        repository.frames["first"] = cached
        DeleteProject(repository, scheduler, host)("first")
        assertNull(repository.get("first"))
        assertNotNull(repository.get("second"))
        assertEquals(mapOf(20 to "second"), repository.bindings())
        assertArrayEquals(intArrayOf(10), host.removed)
        assertEquals(listOf("first"), scheduler.cancelled)
        assertEquals(FrameState(), repository.result("first"))
    }

    @Test
    fun missingProjectDoesNotQueueWork() {
        request("deleted")
        assertTrue(scheduler.requested.isEmpty())
        assertTrue(host.updated.isEmpty())
    }

    @Test
    fun bindingAnExistingProjectCreatesAnIndependentCopy() {
        repository.save(project)
        repository.bind(10, project.id)
        val bound = BindWidget(repository, scheduler, request)(20, project.id)!!
        assertNotEquals(project.id, bound.id)
        assertEquals(project.id, repository.binding(10))
        assertEquals(bound.id, repository.binding(20))
        assertEquals(project.html, bound.html)
    }

    @Test
    fun reconcileRestoresSchedulesAndOnlyRefreshesBoundProjects() {
        repository.save(project)
        repository.save(project.copy(id = "unbound"))
        repository.bind(10, project.id)
        repository.bind(11, project.id)
        ReconcileWidgets(repository, scheduler, host, request)(true)
        assertEquals(setOf("first", "unbound"), scheduler.configured.toSet())
        assertEquals(listOf("first" to false), scheduler.requested)
    }

    @Test
    fun successfulRefreshPublishesThemeAndClosesTheImage() = runBlocking {
        repository.save(project)
        val image = Image()
        val outcome =
            RefreshProject(repository, FrameRenderer { _, _, _ -> image }, environment, host)(
                project.id
            )
        assertEquals(RefreshOutcome.Updated, outcome)
        assertTrue(image.closed)
        assertTrue(repository.result(project.id).dark)
        assertEquals(listOf(project.id), host.updated)
    }

    @Test
    fun failureRetainsTheLastSuccessfulFrameAndTime() = runBlocking {
        repository.save(project)
        repository.frames[project.id] = cached
        val outcome =
            RefreshProject(
                repository,
                FrameRenderer { _, _, _ -> error("Offline") },
                environment,
                host,
            )(project.id)
        assertEquals(RefreshOutcome.Failed("Offline"), outcome)
        assertEquals(cached.file, repository.result(project.id).file)
        assertEquals(cached.success, repository.result(project.id).success)
    }

    @Test
    fun cancellationIsNotConvertedToRenderFailure() = runBlocking {
        repository.save(project)
        repository.frames[project.id] = cached
        try {
            RefreshProject(
                repository,
                FrameRenderer { _, _, _ -> throw CancellationException("Stopped") },
                environment,
                host,
            )(project.id)
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertEquals(cached, repository.result(project.id))
            assertTrue(host.updated.isEmpty())
        }
    }

    @Test
    fun staleRenderCannotOverwriteNewerSource() = runBlocking {
        repository.save(project)
        repository.frames[project.id] = cached
        val image = Image()
        val renderer = FrameRenderer { _, _, _ ->
            repository.save(project.copy(revision = 2))
            image
        }
        assertEquals(
            RefreshOutcome.Superseded,
            RefreshProject(repository, renderer, environment, host)(project.id),
        )
        assertEquals(cached, repository.result(project.id))
        assertTrue(image.closed)
    }

    @Test
    fun deletionDuringRenderCannotResurrectTheProject() = runBlocking {
        repository.save(project)
        repository.frames[project.id] = cached
        val image = Image()
        val renderer = FrameRenderer { _, _, _ ->
            repository.delete(project.id)
            image
        }
        assertEquals(
            RefreshOutcome.Superseded,
            RefreshProject(repository, renderer, environment, host)(project.id),
        )
        assertNull(repository.get(project.id))
        assertEquals(FrameState(), repository.result(project.id))
        assertTrue(image.closed)
    }

    @Test
    fun missingProjectSkipsRendering() = runBlocking {
        val renderer = FrameRenderer { _, _, _ -> error("Must not render") }
        assertEquals(
            RefreshOutcome.Missing,
            RefreshProject(repository, renderer, environment, host)("gone"),
        )
    }

    @Test
    fun concurrentRefreshesOfOneProjectAreSerialized() = runBlocking {
        repository.save(project)
        var active = 0
        var maximum = 0
        val firstStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val renderer = FrameRenderer { _, _, _ ->
            active++
            maximum = maxOf(maximum, active)
            firstStarted.complete(Unit)
            release.await()
            yield()
            active--
            Image()
        }
        val refresh = RefreshProject(repository, renderer, environment, host)
        withTimeout(3_000) {
            val first = async { refresh(project.id) }
            firstStarted.await()
            val second = async { refresh(project.id) }
            yield()
            release.complete(Unit)
            first.await()
            second.await()
        }
        assertEquals(1, maximum)
    }
}
