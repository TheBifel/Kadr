package com.thebifel.kadr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose

class Repository(private val context: Context) {
    private val prefs = context.getSharedPreferences("kadr",Context.MODE_PRIVATE)
    private val frames = File(context.filesDir,"frames").apply { mkdirs() }
    fun signalConfigurationChange() { prefs.edit().putLong("configurationTick",System.nanoTime()).commit() }
    fun changes() = callbackFlow<Long> {
        val listener=android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_ -> trySend(System.nanoTime()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(System.nanoTime())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun all(): List<Project> = synchronized(lock) { prefs.all.keys.filter { it.startsWith("project.") }.sorted().mapNotNull { get(it.removePrefix("project.")) } }
    fun get(id: String): Project? = prefs.getString("project.$id",null)?.let { runCatching { Project.parse(JSONObject(it)) }.getOrNull() }
    fun initialize() = synchronized(lock) {
        if(!prefs.getBoolean("initialized",false)) {
            ensure()
            check(prefs.edit().putBoolean("initialized",true).commit())
        }
    }
    fun ensure(): Project = synchronized(lock) { all().firstOrNull() ?: Project(name=LocaleSupport.localized(context).getString(R.string.starter_power_name),html=Templates.page(context,"power")).also { save(it) } }
    fun save(project: Project) = synchronized(lock) { require(Policy.validInterval(project.interval)); check(prefs.edit().putString("project.${project.id}",project.json().toString()).commit()) }
    fun result(id: String): FrameState = prefs.getString("frame.$id",null)?.let { runCatching { FrameState.parse(JSONObject(it)) }.getOrNull() } ?: FrameState()
    fun bitmap(id: String): Bitmap? { val f=result(id).file; return if(f.isEmpty() || !f.matches(Regex("[a-zA-Z0-9._-]+"))) null else BitmapFactory.decodeFile(File(frames,f).path) }
    fun frameFile(id: String): File? = result(id).file.takeIf { it.matches(Regex("[a-zA-Z0-9._-]+")) }?.let { File(frames,it) }
    fun publish(project: Project, bitmap: Bitmap, effectiveDark: Boolean=project.dark): Boolean = synchronized(lock) {
        if(!Policy.canPublish(project.revision,get(project.id)?.revision ?: -1)) return false
        val old=result(project.id)
        val file=File(frames,"${project.id}-${System.nanoTime()}.png")
        val atomic=AtomicFile(file)
        val stream=atomic.startWrite()
        try { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)); atomic.finishWrite(stream) }
        catch(e: Throwable) { atomic.failWrite(stream); throw e }
        val now=System.currentTimeMillis()
        val result=FrameState(file.name,now,now,"",bitmap.width,bitmap.height,project.revision,effectiveDark)
        if(!prefs.edit().putString("frame.${project.id}",result.json().toString()).commit()) { file.delete(); error(LocaleSupport.localized(context).getString(R.string.frame_save_failed)) }
        if(old.file.isNotEmpty()) File(frames,old.file).delete()
        true
    }
    fun fail(id: String, message: String, expectedRevision: Long?=null) = synchronized(lock) {
        if(get(id)==null)return@synchronized
        if(expectedRevision!=null && get(id)?.revision!=expectedRevision)return@synchronized
        val old=result(id)
        check(prefs.edit().putString("frame.$id",old.copy(attempt=System.currentTimeMillis(),error=message.take(180),pending=0).json().toString()).commit())
    }
    fun markPending(id: String) = synchronized(lock) {
        if(get(id)==null)return@synchronized
        val old=result(id)
        check(prefs.edit().putString("frame.$id",old.copy(pending=System.currentTimeMillis()).json().toString()).commit())
    }
    fun bind(widgetId: Int, projectId: String) = synchronized(lock) { check(get(projectId)!=null); check(prefs.edit().putString("binding.$widgetId",projectId).commit()) }
    fun binding(widgetId: Int): String? = prefs.getString("binding.$widgetId",null)
    fun bindings(): Map<Int,String> = prefs.all.entries.filter { it.key.startsWith("binding.") && it.value is String }.mapNotNull { (k,v) -> k.removePrefix("binding.").toIntOrNull()?.let { it to v as String } }.toMap()
    fun unbind(widgetId: Int) { prefs.edit().remove("binding.$widgetId").commit() }
    fun remap(old: IntArray,new: IntArray) = synchronized(lock) {
        val mappings=old.zip(new.toList()).mapNotNull { (a,b) -> binding(a)?.let { Triple(a,b,it) } }
        val edit=prefs.edit(); mappings.forEach { edit.remove("binding.${it.first}") }; mappings.forEach { edit.putString("binding.${it.second}",it.third) }; check(edit.commit())
    }
    fun cloneForBinding(source: Project): Project = if(bindings().values.contains(source.id)) source.copy(id=java.util.UUID.randomUUID().toString(),name=source.name.take(72)+" · 2",revision=1).also { save(it) } else source
    fun delete(id: String): IntArray = synchronized(lock) {
        val ids=bindings().filterValues { it==id }.keys.toIntArray()
        val old=result(id)
        val edit=prefs.edit().remove("project.$id").remove("frame.$id").putBoolean("initialized",true)
        ids.forEach { edit.remove("binding.$it") }
        check(edit.commit())
        if(old.file.isNotEmpty())File(frames,old.file).delete()
        ids
    }
    companion object { private val lock=Any() }
}
