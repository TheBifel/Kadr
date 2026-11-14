package com.thebifel.kadr.domain.port

import com.thebifel.kadr.domain.model.FrameState
import com.thebifel.kadr.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface WidgetRepository {
    fun changes(): Flow<Long>

    fun all(): List<Project>

    fun get(id: String): Project?

    fun initialize(starter: () -> Project)

    fun ensure(starter: () -> Project): Project

    fun save(project: Project)

    fun saveNextRevision(draft: Project): Project

    fun result(id: String): FrameState

    fun loadFrame(id: String): FrameImage?

    fun publish(project: Project, image: FrameImage, effectiveDark: Boolean = project.dark): Boolean

    fun fail(id: String, message: String, expectedRevision: Long? = null)

    fun markPending(id: String)

    fun bind(widgetId: Int, projectId: String)

    fun binding(widgetId: Int): String?

    fun bindings(): Map<Int, String>

    fun unbind(widgetId: Int)

    fun remap(old: IntArray, new: IntArray)

    fun cloneForBinding(source: Project): Project

    fun delete(id: String): IntArray

    fun signalConfigurationChange()
}
