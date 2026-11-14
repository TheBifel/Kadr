package com.thebifel.kadr

import org.json.JSONObject
import java.util.UUID

object Policy {
    val intervals = listOf(0, 15, 30, 60, 180, 360, 1440)
    fun needsNetwork(html: String): Boolean = !Regex("""<meta\b(?=[^>]*\bname\s*=\s*["']kadr-network["'])(?=[^>]*\bcontent\s*=\s*["']none["'])[^>]*>""",RegexOption.IGNORE_CASE).containsMatchIn(html)
    fun validInterval(minutes: Int) = minutes in intervals
    fun renderSize(widthDp: Int, heightDp: Int): Pair<Int, Int> {
        val w = widthDp.coerceIn(140, 640)
        val h = heightDp.coerceIn(70, 640)
        val scale = minOf(2.0, 720.0 / w, 480.0 / h)
        return (w * scale).toInt() to (h * scale).toInt()
    }
    fun canPublish(expected: Long, actual: Long) = expected == actual
}

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New widget",
    val html: String,
    val dark: Boolean = false,
    val radius: Int = 24,
    val interval: Int = 30,
    val wifi: Boolean = false,
    val battery: Boolean = true,
    val revision: Long = 1,
    val autoTheme: Boolean = true
) {
    fun isDark(context: android.content.Context): Boolean = if(autoTheme)(context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES else dark
    fun json() = JSONObject().put("id", id).put("name", name).put("html", html).put("dark", dark)
        .put("radius", radius).put("interval", interval).put("wifi", wifi).put("battery", battery).put("revision", revision).put("autoTheme",autoTheme)
    companion object {
        fun parse(j: JSONObject): Project = Project(
            id = j.getString("id"), name = j.getString("name"), html = j.getString("html"),
            dark = j.optBoolean("dark"), radius = j.optInt("radius",24), interval = j.optInt("interval",30),
            wifi = j.optBoolean("wifi"), battery = j.optBoolean("battery",true), revision = j.optLong("revision",1), autoTheme = j.optBoolean("autoTheme",false)
        ).also {
            require(it.id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
            require(it.html.length <= 500_000 && it.name.length <= 80)
            require(it.radius in listOf(0,16,24) && Policy.validInterval(it.interval))
        }
    }
}

data class FrameState(val file: String = "", val success: Long = 0, val attempt: Long = 0, val error: String = "", val width: Int = 0, val height: Int = 0, val revision: Long = 0, val dark: Boolean = false, val pending: Long = 0) {
    fun json() = JSONObject().put("file",file).put("success",success).put("attempt",attempt).put("error",error).put("width",width).put("height",height).put("revision",revision).put("dark",dark).put("pending",pending)
    companion object { fun parse(j: JSONObject) = FrameState(j.optString("file"),j.optLong("success"),j.optLong("attempt"),j.optString("error"),j.optInt("width"),j.optInt("height"),j.optLong("revision"),j.optBoolean("dark"),j.optLong("pending")) }
}
