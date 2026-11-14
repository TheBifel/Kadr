package com.thebifel.kadr

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.thebifel.kadr.platform.runAsync
import com.thebifel.kadr.platform.widget.KadrGlanceWidget

class FrameUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ids = intent.getIntArrayExtra("ids") ?: return
        runAsync {
            val live =
                AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(ComponentName(context, KadrWidget::class.java))
                    .toSet()
            val manager = GlanceAppWidgetManager(context)
            ids.filter { it in live }
                .forEach { KadrGlanceWidget().update(context, manager.getGlanceIdBy(it)) }
        }
    }
}
