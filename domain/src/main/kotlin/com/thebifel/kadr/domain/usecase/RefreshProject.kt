package com.thebifel.kadr.domain.usecase

import com.thebifel.kadr.domain.port.EnvironmentProvider
import com.thebifel.kadr.domain.port.FrameRenderer
import com.thebifel.kadr.domain.port.WidgetHost
import com.thebifel.kadr.domain.port.WidgetRepository
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface RefreshOutcome {
    data object Updated : RefreshOutcome

    data object Superseded : RefreshOutcome

    data object Missing : RefreshOutcome

    data class Failed(val message: String) : RefreshOutcome
}

/** Shared by all Worker instances; serializes publication per project, not across projects. */
class RefreshProject(
    private val repository: WidgetRepository,
    private val renderer: FrameRenderer,
    private val environment: EnvironmentProvider,
    private val widgets: WidgetHost,
) {
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend operator fun invoke(id: String): RefreshOutcome =
        locks
            .getOrPut(id) { Mutex() }
            .withLock {
                val project = repository.get(id) ?: return@withLock RefreshOutcome.Missing
                try {
                    val currentEnvironment = environment.current(project)
                    val published =
                        renderer.render(project, widgets.size(id), currentEnvironment).use { image
                            ->
                            repository.publish(project, image, currentEnvironment.dark)
                        }
                    widgets.updateProject(id)
                    if (published) RefreshOutcome.Updated else RefreshOutcome.Superseded
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    val message = error.message ?: error.javaClass.simpleName
                    repository.fail(id, message, project.revision)
                    widgets.updateProject(id)
                    RefreshOutcome.Failed(message)
                }
            }
}
