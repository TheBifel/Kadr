package com.thebifel.kadr.platform.widget

import com.thebifel.kadr.domain.model.FrameState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetPresentation {
    fun overlayStamp(frame: FrameState): String =
        when {
            frame.pending > 0 -> "↻ " + stamp(frame.success)
            frame.error.isNotEmpty() -> "! " + stamp(frame.success)
            else -> stamp(frame.success)
        }

    fun stamp(time: Long): String =
        if (time == 0L) "—"
        else SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(time))
}
