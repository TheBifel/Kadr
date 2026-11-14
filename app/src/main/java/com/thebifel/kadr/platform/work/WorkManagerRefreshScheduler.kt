package com.thebifel.kadr.platform.work

import android.content.Context
import androidx.work.*
import com.thebifel.kadr.RefreshWorker
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.WidgetPolicy
import com.thebifel.kadr.domain.port.RefreshScheduler
import java.util.concurrent.TimeUnit

class WorkManagerRefreshScheduler(private val context: Context) : RefreshScheduler {
    private val manager: WorkManager
        get() = WorkManager.getInstance(context)

    override fun configure(project: Project) {
        if (project.interval == 0) {
            manager.cancelUniqueWork(periodicName(project.id))
            return
        }
        val request =
            PeriodicWorkRequestBuilder<RefreshWorker>(project.interval.toLong(), TimeUnit.MINUTES)
                .setInputData(workDataOf(PROJECT_ID to project.id))
                .setConstraints(constraints(project, manual = false))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("kadr-periodic")
                .build()
        manager.enqueueUniquePeriodicWork(
            periodicName(project.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun enqueue(project: Project, replace: Boolean) {
        val request =
            OneTimeWorkRequestBuilder<RefreshWorker>()
                .setInputData(workDataOf(PROJECT_ID to project.id))
                .setConstraints(constraints(project, manual = true))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("kadr-manual")
                .build()
        manager.enqueueUniqueWork(
            manualName(project.id),
            if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    override fun cancel(projectId: String) {
        manager.cancelUniqueWork(manualName(projectId))
        manager.cancelUniqueWork(periodicName(projectId))
    }

    private fun constraints(project: Project, manual: Boolean) =
        Constraints.Builder()
            .setRequiredNetworkType(
                when {
                    project.wifi && !manual -> NetworkType.UNMETERED
                    WidgetPolicy.needsNetwork(project.html) -> NetworkType.CONNECTED
                    else -> NetworkType.NOT_REQUIRED
                }
            )
            .setRequiresBatteryNotLow(!manual && project.battery)
            .build()

    companion object {
        const val PROJECT_ID = "project"

        fun manualName(id: String) = "manual.$id"

        fun periodicName(id: String) = "periodic.$id"
    }
}
