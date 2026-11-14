package com.thebifel.kadr.data.local

import com.thebifel.kadr.domain.model.FrameState
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.WidgetPolicy
import org.json.JSONObject

/** Keeps the 0.1.x storage format stable; serialization never enters domain models. */
object ProjectJson {
    fun encode(project: Project): JSONObject =
        with(project) {
            JSONObject()
                .put("id", id)
                .put("name", name)
                .put("html", html)
                .put("dark", dark)
                .put("radius", radius)
                .put("interval", interval)
                .put("wifi", wifi)
                .put("battery", battery)
                .put("revision", revision)
                .put("autoTheme", autoTheme)
        }

    fun decode(json: JSONObject): Project =
        Project(
                id = json.getString("id"),
                name = json.getString("name"),
                html = json.getString("html"),
                dark = json.optBoolean("dark"),
                radius = json.optInt("radius", 24),
                interval = json.optInt("interval", 30),
                wifi = json.optBoolean("wifi"),
                battery = json.optBoolean("battery", true),
                revision = json.optLong("revision", 1),
                // Legacy projects had an explicit light/dark preference.
                autoTheme = json.optBoolean("autoTheme", false),
            )
            .also(WidgetPolicy::validate)

    fun encode(frame: FrameState): JSONObject =
        with(frame) {
            JSONObject()
                .put("file", file)
                .put("success", success)
                .put("attempt", attempt)
                .put("error", error)
                .put("width", width)
                .put("height", height)
                .put("revision", revision)
                .put("dark", dark)
                .put("pending", pending)
        }

    fun decodeFrame(json: JSONObject) =
        FrameState(
            file = json.optString("file"),
            success = json.optLong("success"),
            attempt = json.optLong("attempt"),
            error = json.optString("error"),
            width = json.optInt("width"),
            height = json.optInt("height"),
            revision = json.optLong("revision"),
            dark = json.optBoolean("dark"),
            pending = json.optLong("pending"),
        )
}
