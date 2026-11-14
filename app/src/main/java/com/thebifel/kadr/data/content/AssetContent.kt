package com.thebifel.kadr.data.content

import android.content.Context
import com.thebifel.kadr.R
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.port.ProjectTemplates
import com.thebifel.kadr.platform.LocaleSupport

class AssetContent(private val context: Context) : ProjectTemplates {
    private val localized: Context
        get() = LocaleSupport.localized(context)

    fun page(name: String): String {
        require(name in listOf("power", "weather", "blank"))
        return read("templates/$name-${LocaleSupport.language(context)}.html")
    }

    override fun starter() =
        Project(name = localized.getString(R.string.starter_power_name), html = page("power"))

    override fun newProject() =
        Project(name = localized.getString(R.string.new_name), html = page("blank"))

    override fun apply(project: Project, preset: String): Project {
        val title =
            when (preset) {
                "power" -> R.string.preset_power
                "weather" -> R.string.preset_weather
                else -> R.string.preset_blank
            }
        return project.copy(name = localized.getString(title), html = page(preset))
    }

    fun contract(): String = read("contracts/sdk-${LocaleSupport.language(context)}.txt")

    fun aiInstructions(): String =
        read("contracts/ai-en.txt") + "\n\n" + read("contracts/sdk-en.txt")

    private fun read(path: String) =
        context.assets.open(path).bufferedReader().use { it.readText() }
}
