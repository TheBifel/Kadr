package com.thebifel.kadr

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RefreshWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val id=inputData.getString("project") ?: return Result.failure()
        return mutexes.getOrPut(id){Mutex()}.withLock {
            val repo=Repository(applicationContext)
            val project=repo.get(id) ?: return@withLock Result.success()
            val size=WidgetUi.dimensions(applicationContext,id)
            try {
                val renderDark=project.isDark(applicationContext)
                val bitmap=HtmlRenderer(applicationContext).render(project,size.first,size.second)
                try { repo.publish(project,bitmap,renderDark) } finally { bitmap.recycle() }
                WidgetUi.updateProject(applicationContext,id)
                Result.success()
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                repo.fail(id,e.message ?: LocaleSupport.localized(applicationContext).getString(R.string.render_failed),project.revision)
                WidgetUi.updateProject(applicationContext,id)
                if(runAttemptCount<2 && inputData.getBoolean("retry",false))Result.retry() else Result.failure()
            }
        }
    }
    companion object { private val mutexes=ConcurrentHashMap<String,Mutex>() }
}

object Schedule {
    fun delete(context: Context,id: String) {
        val ids=Repository(context).delete(id)
        val wm=WorkManager.getInstance(context)
        wm.cancelUniqueWork("manual.$id")
        wm.cancelUniqueWork("periodic.$id")
        ids.forEach { WidgetUi.update(context,it) }
    }
    private fun constraints(p: Project,manual: Boolean) = Constraints.Builder()
        .setRequiredNetworkType(when { p.wifi && !manual -> NetworkType.UNMETERED;Policy.needsNetwork(p.html)->NetworkType.CONNECTED;else->NetworkType.NOT_REQUIRED })
        .setRequiresBatteryNotLow(!manual && p.battery).build()
    fun configure(context: Context,p: Project) {
        val wm=WorkManager.getInstance(context)
        if(p.interval==0)wm.cancelUniqueWork("periodic.${p.id}")
        else wm.enqueueUniquePeriodicWork("periodic.${p.id}",ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<RefreshWorker>(p.interval.toLong(),TimeUnit.MINUTES)
                .setInputData(workDataOf("project" to p.id)).setConstraints(constraints(p,false))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).addTag("kadr-periodic").build())
    }
    fun now(context: Context,p: Project,replace: Boolean=false): Operation {
        Repository(context).markPending(p.id)
        WidgetUi.updateProject(context,p.id)
        return WorkManager.getInstance(context).enqueueUniqueWork("manual.${p.id}",if(replace)ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RefreshWorker>().setInputData(workDataOf("project" to p.id,"retry" to false)).setConstraints(constraints(p,true))
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).addTag("kadr-manual").build())
    }
    fun refreshAutomatic(context: Context) {
        val repo=Repository(context)
        repo.bindings().values.distinct().mapNotNull { repo.get(it) }.filter { it.autoTheme }.forEach { now(context,it,replace=true) }
    }
    fun reconcile(context: Context,refresh: Boolean) {
        val repo=Repository(context)
        repo.all().forEach { configure(context,it) }
        repo.bindings().values.distinct().forEach { id -> WidgetUi.updateProject(context,id);if(refresh)repo.get(id)?.let { now(context,it) } }
    }
}
