package com.thebifel.kadr.domain.port

import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.RenderSize

interface RefreshScheduler {
    fun configure(project: Project)

    fun enqueue(project: Project, replace: Boolean = false)

    fun cancel(projectId: String)
}

interface WidgetHost {
    fun size(projectId: String): RenderSize

    fun updateProject(projectId: String)

    fun updateWidgets(ids: IntArray)
}

interface ProjectTemplates {
    fun starter(): Project

    fun newProject(): Project

    fun apply(project: Project, preset: String): Project
}
