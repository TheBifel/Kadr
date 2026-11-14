package com.thebifel.kadr

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
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
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.TextAlign
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KadrApp: Application() {
    private var language=""
    private var night=0
    override fun onCreate() {
        super.onCreate();WebView.enableSlowWholeDocumentDraw()
        language=LocaleSupport.language(this)
        night=resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        val nextLanguage=LocaleSupport.language(this)
        val nextNight=newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        if(nextLanguage!=language || nextNight!=night) {
            val repo=Repository(this)
            repo.signalConfigurationChange()
            repo.bindings().values.distinct().mapNotNull { repo.get(it) }
                .filter { nextLanguage!=language || it.autoTheme }.forEach { Schedule.now(this,it,replace=true) }
            language=nextLanguage;night=nextNight
        }
    }
}

/** UI is Glance/Compose; HTML is already a persisted, inert bitmap. */
class KadrGlanceWidget: GlanceAppWidget() {
    override val sizeMode = SizeMode.Single
    override suspend fun provideGlance(context: Context,id: GlanceId) {
        val widgetId=GlanceAppWidgetManager(context).getAppWidgetId(id)
        val repo=Repository(context)
        provideContent {
            val changes=remember { repo.changes() }
            val tick by changes.collectAsState(initial=0L)
            val localized=LocaleSupport.localized(context)
            val project=remember(tick) { repo.binding(widgetId)?.let { repo.get(it) } }
            val frame=remember(tick) { project?.let { repo.result(it.id) } ?: FrameState() }
            val bitmap=remember(frame.file) { project?.let { repo.bitmap(it.id) } }
            val bg=Color(if(project?.isDark(context)==true)0xFF222B1B else 0xFFF2F4E9)
            val muted=Color(if(project?.isDark(context)==true)0xFFB8C9A4 else 0xFF69765B)
            val open=if(project==null)Intent(context,ConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId)
                else Intent(context,MainActivity::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://edit/${project.id}"))
            Box(GlanceModifier.fillMaxSize().background(bg).cornerRadius((project?.radius ?: 24).dp).appWidgetBackground(),contentAlignment=Alignment.BottomEnd) {
                if(bitmap!=null)Image(ImageProvider(bitmap),localized.getString(R.string.frame_description,project?.name ?: "Kadr",WidgetUi.stamp(frame.success)),GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)),contentScale=ContentScale.Fit)
                else Box(GlanceModifier.fillMaxSize().clickable(actionStartActivity(open)),contentAlignment=Alignment.Center) {
                    Text(if(project==null)localized.getString(R.string.choose_page) else localized.getString(R.string.waiting_first),style=TextStyle(color=ColorProvider(muted),fontSize=13.sp))
                }
                Row(GlanceModifier.width(Policy.controlsWidth.dp).height(Policy.controlsHeight.dp).padding(end=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(WidgetUi.overlayStamp(frame),GlanceModifier.defaultWeight(),style=TextStyle(color=ColorProvider(muted),fontSize=10.sp,textAlign=TextAlign.End),maxLines=1)
                    if(project!=null) {
                        val refresh=Intent(context,RefreshReceiver::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://refresh/$widgetId")).addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        Box(GlanceModifier.size(48.dp).clickable(actionSendBroadcast(refresh)),contentAlignment=Alignment.Center) {
                            Image(ImageProvider(R.drawable.ic_refresh),localized.getString(R.string.update_widget),GlanceModifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

object WidgetUi {
    fun dimensions(context: Context,projectId: String): Pair<Int,Int> {
        val mgr=AppWidgetManager.getInstance(context)
        val ids=Repository(context).bindings().filterValues { it==projectId }.keys
        if(ids.isEmpty())return 320 to 160
        val sizes=ids.map { mgr.getAppWidgetOptions(it) }.map { b ->
            b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320).coerceIn(140,640) to
                b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,220).coerceIn(70,640)
        }
        return sizes.maxOf { it.first } to sizes.maxOf { it.second }
    }
    fun updateProject(context: Context,id: String) {
        val ids=Repository(context).bindings().filterValues { it==id }.keys.toIntArray()
        if(ids.isNotEmpty())context.sendBroadcast(Intent(context,FrameUpdateReceiver::class.java).putExtra("ids",ids))
    }
    fun update(context: Context,id: Int) { context.sendBroadcast(Intent(context,FrameUpdateReceiver::class.java).putExtra("ids",intArrayOf(id))) }
    fun overlayStamp(frame: FrameState): String = when {
        frame.pending>0L -> "↻ "+stamp(frame.success)
        frame.error.isNotEmpty() -> "! "+stamp(frame.success)
        else -> stamp(frame.success)
    }
    fun stamp(time: Long): String = if(time==0L)"—" else SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(time))
}

/** Keep broadcast lifetime through the Glance update request; never enqueue another HTML render. */
class FrameUpdateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val ids=intent.getIntArrayExtra("ids") ?: return
        val pending=goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { withTimeout(8000) {
                val live=AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context,KadrWidget::class.java)).toSet()
                ids.filter { it in live }.forEach { KadrGlanceWidget().update(context,GlanceAppWidgetManager(context).getGlanceIdBy(it)) }
            } }finally{pending.finish()}
        }
    }
}

class KadrWidget: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KadrGlanceWidget()
    override fun onUpdate(context: Context,manager: AppWidgetManager,ids: IntArray) {
        super.onUpdate(context,manager,ids)
        val repo=Repository(context)
        ids.forEach { id -> repo.binding(id)?.let { repo.get(it) }?.let { Schedule.configure(context,it);Schedule.now(context,it) } }
    }
    override fun onAppWidgetOptionsChanged(context: Context,manager: AppWidgetManager,id: Int,options: Bundle) {
        super.onAppWidgetOptionsChanged(context,manager,id,options)
        val repo=Repository(context);repo.binding(id)?.let { repo.get(it) }?.let { Schedule.now(context,it) }
    }
    override fun onDeleted(context: Context,ids: IntArray) { super.onDeleted(context,ids);val repo=Repository(context);ids.forEach { repo.unbind(it) } }
    override fun onRestored(context: Context,old: IntArray,new: IntArray) {
        super.onRestored(context,old,new)
        val repo=Repository(context);repo.remap(old,new)
        val mgr=AppWidgetManager.getInstance(context)
        new.forEach { id -> mgr.updateAppWidgetOptions(id,Bundle().apply { putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED,true) }) }
        Schedule.reconcile(context,true)
    }
}
class RefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) { val repo=Repository(context);intent.getStringExtra("project")?.let { repo.get(it) }?.let { android.widget.Toast.makeText(context,LocaleSupport.localized(context).getString(R.string.update_requested),android.widget.Toast.LENGTH_SHORT).show();Schedule.now(context,it) } }
}
class RecoveryReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) { if(intent.action in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED))Schedule.reconcile(context,true) }
}
class PinReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
        if(id !in AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context,KadrWidget::class.java)))return
        val repo=Repository(context)
        val source=intent.getStringExtra("project")?.let { repo.get(it) } ?: return
        val project=repo.cloneForBinding(source)
        repo.bind(id,project.id);Schedule.configure(context,project);WidgetUi.update(context,id);Schedule.now(context,project)
    }
}
