package com.thebifel.kadr.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.port.*
import com.thebifel.kadr.domain.usecase.DeleteProject
import com.thebifel.kadr.domain.usecase.RequestRefresh
import com.thebifel.kadr.domain.usecase.SaveProject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Owns drafts and async operations; Compose only renders state and sends events. */
class EditorViewModel(
    private val repository: WidgetRepository,
    private val templates: ProjectTemplates,
    private val renderer: FrameRenderer,
    private val environment: EnvironmentProvider,
    private val widgets: WidgetHost,
    private val scheduler: RefreshScheduler,
    private val saveProject: SaveProject,
    private val deleteProject: DeleteProject,
    private val requestRefresh: RequestRefresh,
) : ViewModel() {
    private data class Preview(
        val html: String,
        val dark: Boolean,
        val radius: Int,
        val locale: String,
        val image: FrameImage,
        val time: Long,
    )

    private data class DraftSession(
        var project: Project,
        var dirty: Boolean = false,
        var preview: Preview? = null,
        var busy: Boolean = false,
        var error: String = "",
        var generation: Int = 0,
    )

    private val sessions = mutableMapOf<String, DraftSession>()
    private val previewJobs = mutableMapOf<String, Job>()
    private val images = mutableMapOf<String, Pair<String, FrameImage?>>()
    private val frameSources = mutableMapOf<String, Pair<String, Project>>()
    private var selectedId: String? = null
    private val mutableState = MutableStateFlow(EditorUiState())
    val state = mutableState.asStateFlow()
    private val effectChannel = Channel<EditorEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { repository.changes().collect { synchronize() } }
    }

    fun onEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.Select -> {
                if (event.id == selectedId || state.value.actionBusy) return
                selectedId = event.id
                viewModelScope.launch { synchronize() }
            }
            is EditorEvent.Edit -> edit(event.project)
            is EditorEvent.ImportHtml -> {
                state.value.draft?.let { edit(it.copy(html = event.html)) }
                mutableState.value =
                    mutableState.value.copy(importGeneration = state.value.importGeneration + 1)
            }
            is EditorEvent.ReportError -> {
                mutableState.value = mutableState.value.copy(operationError = event.message)
            }
            is EditorEvent.Preset ->
                launchAction {
                    val draft = state.value.draft ?: return@launchAction
                    val next = withContext(Dispatchers.IO) { templates.apply(draft, event.name) }
                    if (selectedId == next.id) {
                        edit(next)
                        preview(next)
                    }
                }
            is EditorEvent.Delete ->
                launchAction {
                    withContext(Dispatchers.IO) { deleteProject(event.id) }
                    previewJobs.remove(event.id)?.cancel()
                    sessions.remove(event.id)
                    images.remove(event.id)
                    synchronize()
                }
            EditorEvent.Create ->
                launchAction {
                    val project =
                        withContext(Dispatchers.IO) {
                            templates.newProject().also {
                                repository.save(it)
                                scheduler.configure(it)
                                requestRefresh(it.id)
                            }
                        }
                    selectedId = project.id
                    sessions[project.id] = DraftSession(project, dirty = true)
                    synchronize()
                }
            EditorEvent.Save -> launchAction { save() }
            EditorEvent.Pin -> launchAction { pinSelected() }
            is EditorEvent.PinProject ->
                launchAction {
                    selectedId = event.id
                    synchronize()
                    pinSelected()
                }
            EditorEvent.Preview -> state.value.draft?.let(::preview)
            EditorEvent.Refresh -> {
                if (state.value.dirty) state.value.draft?.let(::preview)
                else
                    selectedId?.let { id ->
                        launchAction { withContext(Dispatchers.IO) { requestRefresh(id) } }
                    }
            }
            EditorEvent.EnvironmentChanged -> {
                val draft = state.value.draft
                if (draft != null && draft.autoTheme && state.value.dirty) preview(draft)
                else viewModelScope.launch { synchronize() }
            }
        }
    }

    private fun edit(project: Project) {
        if (project.id != selectedId) return
        val session = sessions.getOrPut(project.id) { DraftSession(project) }
        session.project = project
        session.dirty = true
        mutableState.value = mutableState.value.copy(operationError = "")
        session.error = ""
        publishSession()
    }

    private fun preview(project: Project) {
        val session = sessions[project.id] ?: return
        previewJobs.remove(project.id)?.cancel()
        val generation = ++session.generation
        session.busy = true
        session.error = ""
        mutableState.value = mutableState.value.copy(operationError = "")
        publishSession()
        previewJobs[project.id] =
            viewModelScope.launch {
                try {
                    val current = environment.current(project)
                    val size = withContext(Dispatchers.IO) { widgets.size(project.id) }
                    val image = renderer.render(project, size, current)
                    if (
                        withContext(Dispatchers.IO) { repository.get(project.id) } != null &&
                            session.generation == generation
                    ) {
                        session.preview =
                            Preview(
                                project.html,
                                current.dark,
                                project.radius,
                                current.locale,
                                image,
                                System.currentTimeMillis(),
                            )
                    } else image.close()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (session.generation == generation)
                        session.error = error.message ?: error.javaClass.simpleName
                } finally {
                    if (session.generation == generation) session.busy = false
                    publishSession()
                }
            }
    }

    private suspend fun pinSelected() {
        val project = if (state.value.dirty) save() else state.value.draft
        project?.let { effectChannel.send(EditorEffect.Pin(it)) }
    }

    private suspend fun save(): Project? {
        val draft = state.value.draft ?: return null
        val saved = withContext(Dispatchers.IO) { saveProject(draft) }
        sessions[saved.id]?.apply {
            project = saved
            dirty = false
            error = ""
        }
        synchronize()
        return saved
    }

    private suspend fun synchronize() {
        val (projects, boundIds) =
            withContext(Dispatchers.IO) { repository.all() to repository.bindings().values.toSet() }
        val knownIds = projects.map { it.id }.toSet()
        previewJobs.keys.filter { it !in knownIds }.forEach { previewJobs.remove(it)?.cancel() }
        sessions.keys.retainAll(knownIds)
        images.keys.retainAll(knownIds)
        frameSources.keys.retainAll(knownIds)
        if (projects.none { it.id == selectedId }) selectedId = projects.firstOrNull()?.id
        projects.forEach { saved ->
            sessions
                .getOrPut(saved.id) { DraftSession(saved) }
                .let { if (!it.dirty) it.project = saved }
        }
        mutableState.value =
            mutableState.value.copy(projects = projects, boundProjectIds = boundIds)
        val id = selectedId
        if (id == null) {
            mutableState.value = EditorUiState(loading = false)
            return
        }
        val cachedImage = images[id]
        val (frame, image) =
            withContext(Dispatchers.IO) {
                val frame = repository.result(id)
                frame to
                    if (cachedImage?.first == frame.file) cachedImage.second
                    else repository.loadFrame(id)
            }
        if (id != selectedId) return
        images[id] = frame.file to image
        projects
            .firstOrNull { it.id == id && it.revision == frame.revision }
            ?.let { frameSources[id] = frame.file to it }
        mutableState.value = mutableState.value.copy(selectedId = id, frame = frame)
        publishSession()
    }

    private fun publishSession() {
        val session = sessions[selectedId] ?: return
        val current = environment.current(session.project)
        val dark = current.dark
        val preview = session.preview?.takeIf { it.time >= state.value.frame.success }
        val saved = frameSources[selectedId]?.takeIf { it.first == state.value.frame.file }?.second
        val matches =
            if (preview != null) {
                preview.html == session.project.html &&
                    preview.dark == dark &&
                    preview.radius == session.project.radius &&
                    preview.locale == current.locale
            } else {
                saved != null &&
                    state.value.frame.success > 0 &&
                    saved.html == session.project.html &&
                    saved.radius == session.project.radius &&
                    state.value.frame.dark == dark
            }
        mutableState.value =
            mutableState.value.copy(
                loading = false,
                selectedId = selectedId,
                draft = session.project,
                dirty = session.dirty,
                image = preview?.image ?: images[selectedId]?.second,
                showDraft = preview != null,
                previewTime = preview?.time ?: state.value.frame.success,
                previewStale = !matches,
                effectiveDark = dark,
                previewBusy = session.busy,
                previewError = session.error,
            )
    }

    private fun launchAction(action: suspend () -> Unit) {
        if (state.value.actionBusy) return
        mutableState.value = mutableState.value.copy(actionBusy = true, operationError = "")
        viewModelScope.launch {
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value =
                    mutableState.value.copy(
                        operationError = error.message ?: error.javaClass.simpleName
                    )
            } finally {
                mutableState.value = mutableState.value.copy(actionBusy = false)
            }
        }
    }
}
