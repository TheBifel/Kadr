package com.thebifel.kadr

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.WebView
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KadrApp: Application() {
    override fun onCreate() { super.onCreate();WebView.enableSlowWholeDocumentDraw() }
}

object WidgetUi {
    fun dimensions(context: Context,projectId: String): Pair<Int,Int> {
        val mgr=AppWidgetManager.getInstance(context)
        val ids=Repository(context).bindings().filterValues { it==projectId }.keys
        if(ids.isEmpty())return 320 to 160
        val sizes=ids.map { mgr.getAppWidgetOptions(it) }.map { b ->
            val w=b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320)
            val h=b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,220)-48
            w.coerceIn(140,640) to h.coerceIn(70,640)
        }
        return sizes.maxOf { it.first } to sizes.maxOf { it.second }
    }
    fun updateProject(context: Context,id: String) { Repository(context).bindings().filterValues { it==id }.keys.forEach { update(context,it) } }
    fun update(context: Context,widgetId: Int) {
        val repo=Repository(context);val project=repo.binding(widgetId)?.let { repo.get(it) }
        val views=RemoteViews(context.packageName,R.layout.widget)
        if(project==null) {
            views.setTextViewText(R.id.widget_name,"КАДР")
            views.setTextViewText(R.id.widget_time,"Виберіть сторінку")
            val intent=Intent(context,ConfigureActivity::class.java).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId)
            views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,widgetId,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            val result=repo.result(project.id)
            val bitmap=repo.bitmap(project.id)
            if(bitmap!=null)views.setImageViewBitmap(R.id.widget_image,bitmap)
            views.setTextViewText(R.id.widget_name,project.name.uppercase(Locale.getDefault()))
            views.setTextViewText(R.id.widget_time,when { result.success==0L -> "Ще немає кадру"; result.error.isNotEmpty() -> "Збій · "+stamp(result.success); else -> "Оновлено "+stamp(result.success) })
            val color=Color.parseColor(if(project.dark)"#B8C9A4" else "#69765B")
            views.setTextColor(R.id.widget_name,color);views.setTextColor(R.id.widget_time,color)
            val drawable=context.resources.getIdentifier("widget_${if(project.dark)"dark" else "light"}_${project.radius}","drawable",context.packageName)
            views.setInt(R.id.widget_root,"setBackgroundResource",drawable)
            val edit=Intent(context,MainActivity::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://edit/${project.id}"))
            views.setOnClickPendingIntent(R.id.widget_image,PendingIntent.getActivity(context,widgetId,edit,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            val refresh=Intent(context,RefreshReceiver::class.java).putExtra("project",project.id).setData(android.net.Uri.parse("kadr://refresh/$widgetId"))
            views.setOnClickPendingIntent(R.id.widget_refresh,PendingIntent.getBroadcast(context,widgetId,refresh,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            views.setContentDescription(R.id.widget_image,"${project.name}. Останнє оновлення ${if(result.success>0)stamp(result.success) else "відсутнє"}. Натисніть, щоб редагувати.")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId,views)
    }
    fun stamp(time: Long): String = SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(time))
}

class KadrWidget: AppWidgetProvider() {
    override fun onUpdate(context: Context,manager: AppWidgetManager,ids: IntArray) {
        val repo=Repository(context)
        ids.forEach { id -> WidgetUi.update(context,id);repo.binding(id)?.let { repo.get(it) }?.let { Schedule.configure(context,it);Schedule.now(context,it) } }
    }
    override fun onAppWidgetOptionsChanged(context: Context,manager: AppWidgetManager,id: Int,options: Bundle) {
        WidgetUi.update(context,id)
        val repo=Repository(context);repo.binding(id)?.let { repo.get(it) }?.let { Schedule.now(context,it) }
    }
    override fun onDeleted(context: Context,ids: IntArray) { val repo=Repository(context);ids.forEach { repo.unbind(it) } }
    override fun onRestored(context: Context,old: IntArray,new: IntArray) {
        val repo=Repository(context);repo.remap(old,new)
        val mgr=AppWidgetManager.getInstance(context)
        new.forEach { id -> mgr.updateAppWidgetOptions(id,Bundle().apply { putBoolean(AppWidgetManager.OPTION_APPWIDGET_RESTORE_COMPLETED,true) }) }
        Schedule.reconcile(context,true)
    }
}

class RefreshReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) { val repo=Repository(context);intent.getStringExtra("project")?.let { repo.get(it) }?.let { Schedule.now(context,it) } }
}
class RecoveryReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) { if(intent.action in listOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED))Schedule.reconcile(context,true) }
}

/** The launcher skips the configuration Activity for requestPinAppWidget. */
class PinReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context,intent: Intent) {
        val id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID)
        val mgr=AppWidgetManager.getInstance(context)
        if(id !in mgr.getAppWidgetIds(ComponentName(context,KadrWidget::class.java)))return
        val repo=Repository(context)
        val source=intent.getStringExtra("project")?.let { repo.get(it) } ?: return
        val project=repo.cloneForBinding(source)
        repo.bind(id,project.id)
        Schedule.configure(context,project)
        WidgetUi.update(context,id)
        Schedule.now(context,project)
    }
}
