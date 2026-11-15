package com.thebifel.kadr.platform.widget

import android.content.Context
import android.util.TypedValue
import android.widget.RemoteViews
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.WidgetPolicy

/** Native clipping surface; the cached frame is never transformed for rounded corners. */
internal object WidgetSurface {
    fun create(context: Context, radiusDp: Int, background: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_surface).apply {
            setInt(android.R.id.background, "setBackgroundColor", background)
            // Keep clipping enabled even for zero: this is the launcher's opt-out contract.
            setBoolean(android.R.id.background, "setClipToOutline", true)
            setViewOutlinePreferredRadius(
                android.R.id.background,
                radiusDp.coerceIn(0, WidgetPolicy.maxCornerRadiusDp).toFloat(),
                TypedValue.COMPLEX_UNIT_DIP,
            )
        }
}
