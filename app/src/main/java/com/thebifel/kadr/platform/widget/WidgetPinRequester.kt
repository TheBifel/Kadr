package com.thebifel.kadr.platform.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.thebifel.kadr.KadrWidget
import com.thebifel.kadr.PinReceiver
import com.thebifel.kadr.domain.model.Project

class WidgetPinRequester(private val context: Context) {
    fun request(project: Project): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (!manager.isRequestPinAppWidgetSupported) return false
        val intent =
            Intent(context, PinReceiver::class.java)
                .putExtra("project", project.id)
                .setData(Uri.parse("kadr://pin/${project.id}"))
        val callback =
            PendingIntent.getBroadcast(
                context,
                project.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        return manager.requestPinAppWidget(
            ComponentName(context, KadrWidget::class.java),
            null,
            callback,
        )
    }
}
