package com.thebifel.kadr.platform.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.thebifel.kadr.domain.model.RenderSize
import com.thebifel.kadr.domain.model.WidgetPolicy
import kotlin.math.roundToInt

/** Display-only alpha mask. The persisted HTML frame stays square so the radius can decrease. */
internal object WidgetFrameShape {
    fun render(source: Bitmap?, size: RenderSize, radiusDp: Int, background: Int): Bitmap {
        // Preserve the host aspect ratio while keeping RemoteViews bitmap memory bounded.
        val scale = minOf(2f, 720f / size.width, 480f / size.height)
        val width = (size.width * scale).roundToInt().coerceAtLeast(1)
        val height = (size.height * scale).roundToInt().coerceAtLeast(1)
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val radius =
            (radiusDp.coerceIn(0, WidgetPolicy.maxCornerRadiusDp) * scale).coerceAtMost(
                minOf(width, height) / 2f
            )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.color = background
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            radius,
            radius,
            paint,
        )
        if (source != null) {
            val fit = minOf(width.toFloat() / source.width, height.toFloat() / source.height)
            val left = (width - source.width * fit) / 2f
            val top = (height - source.height * fit) / 2f
            val target = RectF(left, top, left + source.width * fit, top + source.height * fit)
            // SRC_ATOP preserves the rounded destination alpha, including anti-aliased edges.
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
            canvas.drawBitmap(source, null, target, paint)
        }
        return output
    }
}
