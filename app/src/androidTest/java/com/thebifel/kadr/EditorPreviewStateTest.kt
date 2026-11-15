package com.thebifel.kadr

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thebifel.kadr.data.local.PreferencesWidgetRepository
import com.thebifel.kadr.data.rendering.BitmapFrame
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.domain.port.*
import com.thebifel.kadr.domain.usecase.*
import com.thebifel.kadr.presentation.editor.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorPreviewStateTest {
    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    private val repo
        get() = PreferencesWidgetRepository(instrumentation.targetContext)

    private val fail = AtomicBoolean(false)
    private val gate = AtomicReference<CompletableDeferred<Unit>?>(null)
    private val schedulingGate = AtomicReference<CompletableDeferred<Unit>?>(null)

    private fun waitUntil(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + 15000
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "Timed out" }
            Thread.sleep(30)
        }
    }

    private fun withModel(test: (EditorViewModel, Project) -> Unit) {
        val p =
            Project(
                id = "test-preview-" + UUID.randomUUID(),
                name = "Preview state",
                html = "original",
                interval = 0,
                autoTheme = false,
            )
        repo.save(p)
        val cached = Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888)
        repo.publish(p, BitmapFrame(cached))
        cached.recycle()
        val scheduler =
            object : RefreshScheduler {
                override fun configure(project: Project) {
                    schedulingGate.get()?.let { runBlocking { it.await() } }
                }

                override fun enqueue(project: Project, replace: Boolean) {}

                override fun cancel(projectId: String) {}
            }
        val widgets =
            object : WidgetHost {
                override fun size(projectId: String) = RenderSize(20, 10)

                override fun updateProject(projectId: String) {}

                override fun updateWidgets(ids: IntArray) {}
            }
        val content =
            object : ProjectTemplates {
                override fun starter() = p

                override fun newProject() = p.copy(id = UUID.randomUUID().toString())

                override fun apply(project: Project, preset: String) = project.copy(html = preset)
            }
        val request = RequestRefresh(repo, scheduler, widgets)
        lateinit var model: EditorViewModel
        instrumentation.runOnMainSync {
            model =
                EditorViewModel(
                    repo,
                    content,
                    FrameRenderer { _, _, _ ->
                        gate.get()?.await()
                        if (fail.get()) error("TEST_NETWORK_FAILURE")
                        BitmapFrame(Bitmap.createBitmap(20, 10, Bitmap.Config.ARGB_8888))
                    },
                    EnvironmentProvider { RenderEnvironment(false, "en") },
                    widgets,
                    scheduler,
                    SaveProject(repo, scheduler, request),
                    DeleteProject(repo, scheduler, widgets),
                    request,
                )
            model.onEvent(EditorEvent.Select(p.id))
        }
        waitUntil { model.state.value.selectedId == p.id && !model.state.value.loading }
        try {
            test(model, p)
        } finally {
            gate.get()?.complete(Unit)
            repo.delete(p.id)
            instrumentation.runOnMainSync {
                androidx.lifecycle.ViewModelStore().apply {
                    put("test", model)
                    clear()
                }
            }
        }
    }

    private fun event(model: EditorViewModel, event: EditorEvent) =
        instrumentation.runOnMainSync { model.onEvent(event) }

    @Test
    fun previewWithoutEditsDoesNotSaveOrPublishAndFailureKeepsItsTime() = withModel { model, p ->
        val frame = repo.result(p.id)
        event(model, EditorEvent.Preview)
        waitUntil {
            model.state.value.previewTime > 0 &&
                model.state.value.showDraft &&
                !model.state.value.previewBusy
        }
        assertFalse(model.state.value.dirty)
        assertFalse(model.state.value.previewStale)
        assertEquals(p, repo.get(p.id))
        assertEquals(frame, repo.result(p.id))
        val image = model.state.value.image
        val time = model.state.value.previewTime
        fail.set(true)
        event(model, EditorEvent.Preview)
        waitUntil { model.state.value.previewError.isNotEmpty() }
        assertSame(image, model.state.value.image)
        assertEquals(time, model.state.value.previewTime)
        assertEquals(frame, repo.result(p.id))
    }

    @Test
    fun latePreviewCannotClearStalenessForNewerCodeAndErrorsKeepTheGoodImage() =
        withModel { model, p ->
            event(model, EditorEvent.Edit(p.copy(html = "first edit")))
            assertTrue(model.state.value.previewStale)
            gate.set(CompletableDeferred())
            event(model, EditorEvent.Preview)
            waitUntil { model.state.value.previewBusy }
            event(model, EditorEvent.Edit(p.copy(html = "second edit")))
            gate.get()!!.complete(Unit)
            waitUntil { !model.state.value.previewBusy }
            assertTrue(model.state.value.previewStale)
            gate.set(null)
            event(model, EditorEvent.Preview)
            waitUntil { !model.state.value.previewBusy && !model.state.value.previewStale }
            val image = model.state.value.image
            val time = model.state.value.previewTime
            event(model, EditorEvent.Edit(p.copy(html = "third edit")))
            fail.set(true)
            event(model, EditorEvent.Preview)
            waitUntil { model.state.value.previewError.isNotEmpty() }
            assertTrue(model.state.value.previewStale)
            assertSame(image, model.state.value.image)
            assertEquals(time, model.state.value.previewTime)
            assertEquals(p, repo.get(p.id))
        }

    @Test
    fun renamingScheduleAndNativeCornerChangesDoNotInvalidateThePicture() = withModel { model, p ->
        assertFalse(model.state.value.previewStale)
        event(model, EditorEvent.Edit(p.copy(name = "Renamed", interval = 60)))
        assertFalse(model.state.value.previewStale)
        event(model, EditorEvent.Save)
        waitUntil { !model.state.value.actionBusy && !model.state.value.dirty }
        assertFalse(model.state.value.previewStale)
        assertEquals("Renamed", repo.get(p.id)!!.name)
        event(model, EditorEvent.Edit(model.state.value.draft!!.copy(radius = 17)))
        assertFalse(model.state.value.previewStale)
    }

    @Test
    fun projectOpenedFromLauncherDuringSaveIsSelectedAfterTheSave() = withModel { model, p ->
        val other = p.copy(id = "test-open-" + UUID.randomUUID(), name = "Other widget")
        repo.save(other)
        val pending = CompletableDeferred<Unit>()
        schedulingGate.set(pending)
        try {
            event(model, EditorEvent.Edit(p.copy(name = "Saving widget")))
            event(model, EditorEvent.Save)
            waitUntil { model.state.value.actionBusy }
            event(model, EditorEvent.Select(other.id))
            assertEquals(p.id, model.state.value.selectedId)
            pending.complete(Unit)
            waitUntil { !model.state.value.actionBusy && model.state.value.selectedId == other.id }
            assertEquals(other.id, model.state.value.draft!!.id)
            assertEquals("Saving widget", repo.get(p.id)!!.name)
        } finally {
            pending.complete(Unit)
            schedulingGate.set(null)
            repo.delete(other.id)
        }
    }
}
