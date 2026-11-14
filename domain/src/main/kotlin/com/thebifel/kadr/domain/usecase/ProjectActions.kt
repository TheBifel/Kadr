package com.thebifel.kadr.domain.usecase

import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.port.RefreshScheduler
import com.thebifel.kadr.domain.port.WidgetHost
import com.thebifel.kadr.domain.port.WidgetRepository

class RequestRefresh(
    private val repository: WidgetRepository,
    private val scheduler: RefreshScheduler,
    private val widgets: WidgetHost,
) {
    operator fun invoke(projectId: String, replace: Boolean = false) {
        val project = repository.get(projectId) ?: return
        repository.markPending(projectId)
        widgets.updateProject(projectId)
        scheduler.enqueue(project, replace)
    }
}

class SaveProject(
    private val repository: WidgetRepository,
    private val scheduler: RefreshScheduler,
    private val requestRefresh: RequestRefresh,
) {
    operator fun invoke(draft: Project): Project {
        val saved = repository.saveNextRevision(draft)
        scheduler.configure(saved)
        requestRefresh(saved.id, replace = true)
        return saved
    }
}

class DeleteProject(
    private val repository: WidgetRepository,
    private val scheduler: RefreshScheduler,
    private val widgets: WidgetHost,
) {
    operator fun invoke(id: String) {
        val ids = repository.delete(id)
        scheduler.cancel(id)
        widgets.updateWidgets(ids)
    }
}

class BindWidget(
    private val repository: WidgetRepository,
    private val scheduler: RefreshScheduler,
    private val requestRefresh: RequestRefresh,
) {
    operator fun invoke(widgetId: Int, projectId: String): Project? {
        val source = repository.get(projectId) ?: return null
        val project = repository.cloneForBinding(source)
        repository.bind(widgetId, project.id)
        scheduler.configure(project)
        requestRefresh(project.id)
        return project
    }
}

class ReconcileWidgets(
    private val repository: WidgetRepository,
    private val scheduler: RefreshScheduler,
    private val widgets: WidgetHost,
    private val requestRefresh: RequestRefresh,
) {
    operator fun invoke(refresh: Boolean) {
        repository.all().forEach(scheduler::configure)
        repository.bindings().values.distinct().forEach { id ->
            widgets.updateProject(id)
            if (refresh) requestRefresh(id)
        }
    }
}
