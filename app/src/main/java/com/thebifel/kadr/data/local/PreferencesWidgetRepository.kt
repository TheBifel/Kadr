package com.thebifel.kadr.data.local

import android.content.Context
import android.content.SharedPreferences
import com.thebifel.kadr.domain.model.FrameState
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.WidgetPolicy
import com.thebifel.kadr.domain.port.FrameImage
import com.thebifel.kadr.domain.port.WidgetRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

class PreferencesWidgetRepository(context: Context) : WidgetRepository {
    private val preferences = context.getSharedPreferences("kadr", Context.MODE_PRIVATE)
    private val frames = AtomicFrameFiles(File(context.filesDir, "frames"))

    override fun changes() = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                trySend(System.nanoTime())
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(System.nanoTime())
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun all(): List<Project> =
        synchronized(lock) {
            preferences.all.keys
                .filter { it.startsWith("project.") }
                .sorted()
                .mapNotNull { get(it.removePrefix("project.")) }
        }

    override fun get(id: String): Project? =
        preferences.getString("project.$id", null)?.let {
            runCatching { ProjectJson.decode(JSONObject(it)) }.getOrNull()
        }

    override fun initialize(starter: () -> Project) =
        synchronized(lock) {
            if (!preferences.getBoolean("initialized", false)) {
                ensure(starter)
                check(preferences.edit().putBoolean("initialized", true).commit())
            }
        }

    override fun ensure(starter: () -> Project): Project =
        synchronized(lock) { all().firstOrNull() ?: starter().also(::save) }

    override fun save(project: Project) =
        synchronized(lock) {
            WidgetPolicy.validate(project)
            check(
                preferences
                    .edit()
                    .putString("project.${project.id}", ProjectJson.encode(project).toString())
                    .commit()
            )
        }

    override fun saveNextRevision(draft: Project): Project =
        synchronized(lock) {
            checkNotNull(get(draft.id)) { "Project was deleted" }
            draft.copy(revision = get(draft.id)!!.revision + 1).also(::save)
        }

    override fun result(id: String): FrameState =
        preferences.getString("frame.$id", null)?.let {
            runCatching { ProjectJson.decodeFrame(JSONObject(it)) }.getOrNull()
        } ?: FrameState()

    override fun loadFrame(id: String): FrameImage? = frames.read(result(id).file)

    fun frameFile(id: String): File? = frames.file(result(id).file)

    override fun publish(project: Project, image: FrameImage, effectiveDark: Boolean): Boolean =
        synchronized(lock) {
            if (!WidgetPolicy.canPublish(project.revision, get(project.id)?.revision ?: -1))
                return false
            val old = result(project.id)
            val filename = frames.write(project.id, image)
            val now = System.currentTimeMillis()
            val frame =
                FrameState(
                    filename,
                    now,
                    now,
                    "",
                    image.width,
                    image.height,
                    project.revision,
                    effectiveDark,
                )
            if (
                !preferences
                    .edit()
                    .putString("frame.${project.id}", ProjectJson.encode(frame).toString())
                    .commit()
            ) {
                frames.delete(filename)
                error("Could not save frame state")
            }
            frames.delete(old.file)
            true
        }

    override fun fail(id: String, message: String, expectedRevision: Long?) =
        synchronized(lock) {
            val project = get(id) ?: return@synchronized
            if (expectedRevision != null && project.revision != expectedRevision)
                return@synchronized
            val frame =
                result(id)
                    .copy(
                        attempt = System.currentTimeMillis(),
                        error = message.take(180),
                        pending = 0,
                    )
            check(
                preferences
                    .edit()
                    .putString("frame.$id", ProjectJson.encode(frame).toString())
                    .commit()
            )
        }

    override fun markPending(id: String) =
        synchronized(lock) {
            if (get(id) == null) return@synchronized
            val frame = result(id).copy(pending = System.currentTimeMillis())
            check(
                preferences
                    .edit()
                    .putString("frame.$id", ProjectJson.encode(frame).toString())
                    .commit()
            )
        }

    override fun bind(widgetId: Int, projectId: String) =
        synchronized(lock) {
            check(get(projectId) != null)
            check(preferences.edit().putString("binding.$widgetId", projectId).commit())
        }

    override fun binding(widgetId: Int): String? = preferences.getString("binding.$widgetId", null)

    override fun bindings(): Map<Int, String> =
        preferences.all.entries
            .filter { it.key.startsWith("binding.") && it.value is String }
            .mapNotNull { (key, value) ->
                key.removePrefix("binding.").toIntOrNull()?.let { it to value as String }
            }
            .toMap()

    override fun unbind(widgetId: Int) {
        check(preferences.edit().remove("binding.$widgetId").commit())
    }

    override fun remap(old: IntArray, new: IntArray) =
        synchronized(lock) {
            val mappings =
                old.zip(new.toList()).mapNotNull { (a, b) -> binding(a)?.let { Triple(a, b, it) } }
            val edit = preferences.edit()
            mappings.forEach { edit.remove("binding.${it.first}") }
            mappings.forEach { edit.putString("binding.${it.second}", it.third) }
            check(edit.commit())
        }

    override fun cloneForBinding(source: Project): Project =
        synchronized(lock) {
            if (source.id !in bindings().values) source
            else
                source
                    .copy(
                        id = UUID.randomUUID().toString(),
                        name = source.name.take(72) + " · 2",
                        revision = 1,
                    )
                    .also(::save)
        }

    override fun delete(id: String): IntArray =
        synchronized(lock) {
            val ids = bindings().filterValues { it == id }.keys.toIntArray()
            val old = result(id)
            val edit =
                preferences
                    .edit()
                    .remove("project.$id")
                    .remove("frame.$id")
                    .putBoolean("initialized", true)
            ids.forEach { edit.remove("binding.$it") }
            check(edit.commit())
            frames.delete(old.file)
            ids
        }

    override fun signalConfigurationChange() {
        check(preferences.edit().putLong("configurationTick", System.nanoTime()).commit())
    }

    companion object {
        private val lock = Any()
    }
}
