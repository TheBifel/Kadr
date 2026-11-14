package com.thebifel.kadr.platform.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import com.thebifel.kadr.FrameUpdateReceiver
import com.thebifel.kadr.domain.model.RenderSize
import com.thebifel.kadr.domain.port.WidgetHost
import com.thebifel.kadr.domain.port.WidgetRepository

class AndroidWidgetHost(private val context: Context, private val repository: WidgetRepository) :
    WidgetHost {
    override fun size(projectId: String): RenderSize {
        val manager = AppWidgetManager.getInstance(context)
        val ids = repository.bindings().filterValues { it == projectId }.keys
        if (ids.isEmpty()) return RenderSize()
        val sizes =
            ids.map { manager.getAppWidgetOptions(it) }
                .map {
                    RenderSize(
                        it.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 320)
                            .coerceIn(140, 640),
                        it.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 220)
                            .coerceIn(70, 640),
                    )
                }
        return RenderSize(sizes.maxOf { it.width }, sizes.maxOf { it.height })
    }

    override fun updateProject(projectId: String) =
        updateWidgets(repository.bindings().filterValues { it == projectId }.keys.toIntArray())

    override fun updateWidgets(ids: IntArray) {
        if (ids.isNotEmpty())
            context.sendBroadcast(
                Intent(context, FrameUpdateReceiver::class.java).putExtra("ids", ids)
            )
    }
}
