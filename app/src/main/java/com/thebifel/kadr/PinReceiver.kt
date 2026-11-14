package com.thebifel.kadr

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.platform.runAsync

class PinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id =
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
        val live =
            AppWidgetManager.getInstance(context)
                .getAppWidgetIds(ComponentName(context, KadrWidget::class.java))
        if (id !in live) return
        val source = intent.getStringExtra("project") ?: return
        runAsync { context.appContainer.bindWidget(id, source) }
    }
}
