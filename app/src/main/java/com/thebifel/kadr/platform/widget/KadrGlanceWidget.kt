package com.thebifel.kadr.platform.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.thebifel.kadr.*
import com.thebifel.kadr.R
import com.thebifel.kadr.data.rendering.asBitmap
import com.thebifel.kadr.di.appContainer
import com.thebifel.kadr.domain.model.*
import com.thebifel.kadr.platform.LocaleSupport
import kotlin.math.roundToInt

/** UI is Glance/Compose; HTML is already a persisted, inert bitmap. */
class KadrGlanceWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repo = context.appContainer.repository
        provideContent {
            val changes = remember { repo.changes() }
            val tick by changes.collectAsState(initial = 0L)
            val localized = LocaleSupport.localized(context)
            val project = remember(tick) { repo.binding(widgetId)?.let { repo.get(it) } }
            val frame = remember(tick) { project?.let { repo.result(it.id) } ?: FrameState() }
            val bitmap = remember(frame.file) { project?.let { repo.loadFrame(it.id)?.asBitmap() } }
            val bg =
                Color(
                    if (project?.let { context.appContainer.environment.current(it).dark } == true)
                        0xFF222B1B
                    else 0xFFF2F4E9
                )
            val muted =
                Color(
                    if (project?.let { context.appContainer.environment.current(it).dark } == true)
                        0xFFB8C9A4
                    else 0xFF69765B
                )
            val bounds = LocalSize.current
            val size =
                RenderSize(
                    bounds.width.value.roundToInt().coerceAtLeast(1),
                    bounds.height.value.roundToInt().coerceAtLeast(1),
                )
            val radius = project?.radius ?: 24
            val display =
                remember(frame.file, project?.id, radius, size, bg) {
                    WidgetFrameShape.render(bitmap, size, radius, bg.toArgb())
                }
            val open =
                if (project == null)
                    Intent(context, ConfigureActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                else
                    Intent(context, MainActivity::class.java)
                        .putExtra("project", project.id)
                        .setData(android.net.Uri.parse("kadr://edit/${project.id}"))
            Box(
                GlanceModifier.fillMaxSize()
                    .background(Color.Transparent)
                    // The image owns its rounded alpha; opt out of an additional host outline.
                    .cornerRadius(0.dp)
                    .appWidgetBackground(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                Image(
                    ImageProvider(display),
                    if (bitmap != null)
                        localized.getString(
                            R.string.frame_description,
                            project?.name ?: "Kadr",
                            WidgetPresentation.stamp(frame.success),
                        )
                    else null,
                    GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)),
                    contentScale = ContentScale.FillBounds,
                )
                if (bitmap == null)
                    Box(
                        GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (project == null) localized.getString(R.string.choose_page)
                            else localized.getString(R.string.waiting_first),
                            style = TextStyle(color = ColorProvider(muted), fontSize = 13.sp),
                        )
                    }
                Row(
                    GlanceModifier.width(WidgetPolicy.controlsWidth.dp)
                        .height(WidgetPolicy.controlsHeight.dp)
                        .padding(end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        WidgetPresentation.overlayStamp(frame),
                        GlanceModifier.defaultWeight(),
                        style =
                            TextStyle(
                                color = ColorProvider(muted),
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                            ),
                        maxLines = 1,
                    )
                    if (project != null) {
                        val refresh =
                            Intent(context, RefreshReceiver::class.java)
                                .putExtra("project", project.id)
                                .setData(android.net.Uri.parse("kadr://refresh/$widgetId"))
                                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        Box(
                            GlanceModifier.size(48.dp).clickable(actionSendBroadcast(refresh)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                ImageProvider(R.drawable.ic_refresh),
                                localized.getString(R.string.update_widget),
                                GlanceModifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
