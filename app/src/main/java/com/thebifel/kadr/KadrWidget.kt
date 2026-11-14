package com.thebifel.kadr

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.platform.widget.KadrGlanceWidget

/** Compatibility entry point: changing this name would detach installed home screen widgets. */
class KadrWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = KadrGlanceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Glance owns the broadcast PendingResult. Only enqueue work here; never render.
        with(context.appContainer) {
            appWidgetIds
                .asSequence()
                .mapNotNull { repository.binding(it) }
                .distinct()
                .mapNotNull(repository::get)
                .forEach {
                    scheduler.configure(it)
                    requestRefresh(it.id)
                }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        context.appContainer.repository.binding(appWidgetId)?.let {
            context.appContainer.requestRefresh(it)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        appWidgetIds.forEach(context.appContainer.repository::unbind)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        context.appContainer.repository.remap(oldWidgetIds, newWidgetIds)
        newWidgetIds.forEach { id ->
            AppWidgetManager.getInstance(context)
                .updateAppWidgetOptions(
                    id,
                    Bundle().apply {
                        putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED, true)
                    },
                )
        }
        context.appContainer.reconcile(refresh = true)
    }
}
