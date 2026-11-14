package com.thebifel.kadr.presentation.editor

import com.thebifel.kadr.domain.model.FrameState
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.port.FrameImage

data class EditorUiState(
    val loading: Boolean = true,
    val boundProjectIds: Set<String> = emptySet(),
    val projects: List<Project> = emptyList(),
    val selectedId: String? = null,
    val draft: Project? = null,
    val dirty: Boolean = false,
    val frame: FrameState = FrameState(),
    val image: FrameImage? = null,
    val showDraft: Boolean = false,
    val effectiveDark: Boolean = false,
    val previewBusy: Boolean = false,
    val previewError: String = "",
    val previewTime: Long = 0,
    val previewStale: Boolean = false,
    val actionBusy: Boolean = false,
    val operationError: String = "",
    val importGeneration: Int = 0,
)

sealed interface EditorEvent {
    data class Select(val id: String) : EditorEvent

    data class Edit(val project: Project) : EditorEvent

    data class Preset(val name: String) : EditorEvent

    data class ImportHtml(val html: String) : EditorEvent

    data class Delete(val id: String) : EditorEvent

    data class ReportError(val message: String) : EditorEvent

    data object Create : EditorEvent

    data object Save : EditorEvent

    data object Preview : EditorEvent

    data object Refresh : EditorEvent

    data object Pin : EditorEvent

    data class PinProject(val id: String) : EditorEvent

    data object EnvironmentChanged : EditorEvent
}

sealed interface EditorEffect {
    data class Message(val text: String) : EditorEffect

    data class Pin(val project: Project) : EditorEffect
}
