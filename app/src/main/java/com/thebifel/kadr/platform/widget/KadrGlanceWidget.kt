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
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
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
            val surface =
                remember(project?.radius, bg) {
                    WidgetSurface.create(context, project?.radius ?: 24, bg.toArgb())
                }
            val open =
                if (project == null)
                    Intent(context, ConfigureActivity::class.java)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                else
                    Intent(context, MainActivity::class.java)
                        .putExtra("project", project.id)
                        .setData(android.net.Uri.parse("kadr://edit/${project.id}"))
            AndroidRemoteViews(
                remoteViews = surface,
                containerViewId = android.R.id.background,
                modifier = GlanceModifier.fillMaxSize(),
            ) {
                Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    if (bitmap != null)
                        Image(
                            ImageProvider(bitmap),
                            localized.getString(
                                R.string.frame_description,
                                project?.name ?: "Kadr",
                                WidgetPresentation.stamp(frame.success),
                            ),
                            GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)),
                            contentScale = ContentScale.Fit,
                        )
                    else
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
}
