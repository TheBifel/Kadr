package com.thebifel.kadr

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.domain.usecase.RefreshOutcome
import com.thebifel.kadr.platform.work.WorkManagerRefreshScheduler

/** Keep this class name stable: existing WorkManager rows store its fully qualified name. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id =
            inputData.getString(WorkManagerRefreshScheduler.PROJECT_ID) ?: return Result.failure()
        return when (applicationContext.appContainer.refreshProject(id)) {
            is RefreshOutcome.Failed -> Result.failure()
            else -> Result.success()
        }
    }
}
