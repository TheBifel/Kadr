package com.thebifel.kadr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

class Repository(context: Context) {
    private val prefs = context.getSharedPreferences("kadr",Context.MODE_PRIVATE)
    private val frames = File(context.filesDir,"frames").apply { mkdirs() }
    fun all(): List<Project> = synchronized(lock) { prefs.all.keys.filter { it.startsWith("project.") }.sorted().mapNotNull { get(it.removePrefix("project.")) } }
    fun get(id: String): Project? = prefs.getString("project.$id",null)?.let { runCatching { Project.parse(JSONObject(it)) }.getOrNull() }
    fun ensure(): Project = synchronized(lock) { all().firstOrNull() ?: Project().also { save(it) } }
    fun save(project: Project) = synchronized(lock) { require(Policy.validInterval(project.interval)); check(prefs.edit().putString("project.${project.id}",project.json().toString()).commit()) }
    fun result(id: String): FrameState = prefs.getString("frame.$id",null)?.let { runCatching { FrameState.parse(JSONObject(it)) }.getOrNull() } ?: FrameState()
    fun bitmap(id: String): Bitmap? { val f=result(id).file; return if(f.isEmpty() || !f.matches(Regex("[a-zA-Z0-9._-]+"))) null else BitmapFactory.decodeFile(File(frames,f).path) }
    fun frameFile(id: String): File? = result(id).file.takeIf { it.matches(Regex("[a-zA-Z0-9._-]+")) }?.let { File(frames,it) }
    fun publish(project: Project, bitmap: Bitmap): Boolean = synchronized(lock) {
        if(!Policy.canPublish(project.revision,get(project.id)?.revision ?: -1)) return false
        val old=result(project.id)
        val file=File(frames,"${project.id}-${System.nanoTime()}.png")
        val atomic=AtomicFile(file)
        val stream=atomic.startWrite()
        try { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)); atomic.finishWrite(stream) }
        catch(e: Throwable) { atomic.failWrite(stream); throw e }
        val now=System.currentTimeMillis()
        val result=FrameState(file.name,now,now,"",bitmap.width,bitmap.height,project.revision)
        if(!prefs.edit().putString("frame.${project.id}",result.json().toString()).commit()) { file.delete(); error("Не вдалося зберегти стан кадру") }
        if(old.file.isNotEmpty()) File(frames,old.file).delete()
        true
    }
    fun fail(id: String, message: String, expectedRevision: Long?=null) = synchronized(lock) {
        if(expectedRevision!=null && get(id)?.revision!=expectedRevision)return@synchronized
        val old=result(id)
        check(prefs.edit().putString("frame.$id",old.copy(attempt=System.currentTimeMillis(),error=message.take(180)).json().toString()).commit())
    }
    fun bind(widgetId: Int, projectId: String) = synchronized(lock) { check(get(projectId)!=null); check(prefs.edit().putString("binding.$widgetId",projectId).commit()) }
    fun binding(widgetId: Int): String? = prefs.getString("binding.$widgetId",null)
    fun bindings(): Map<Int,String> = prefs.all.entries.filter { it.key.startsWith("binding.") && it.value is String }.mapNotNull { (k,v) -> k.removePrefix("binding.").toIntOrNull()?.let { it to v as String } }.toMap()
    fun unbind(widgetId: Int) { prefs.edit().remove("binding.$widgetId").commit() }
    fun remap(old: IntArray,new: IntArray) = synchronized(lock) {
        val mappings=old.zip(new.toList()).mapNotNull { (a,b) -> binding(a)?.let { Triple(a,b,it) } }
        val edit=prefs.edit(); mappings.forEach { edit.remove("binding.${it.first}") }; mappings.forEach { edit.putString("binding.${it.second}",it.third) }; check(edit.commit())
    }
    fun cloneForBinding(source: Project): Project = if(bindings().values.contains(source.id)) source.copy(id=java.util.UUID.randomUUID().toString(),name=source.name.take(72)+" · копія",revision=1).also { save(it) } else source
    fun delete(id: String) = synchronized(lock) {
        require(id !in bindings().values) { "Спочатку приберіть віджет із домашнього екрана" }
        val old=result(id);prefs.edit().remove("project.$id").remove("frame.$id").commit();if(old.file.isNotEmpty())File(frames,old.file).delete()
    }
    companion object { private val lock=Any() }
}
