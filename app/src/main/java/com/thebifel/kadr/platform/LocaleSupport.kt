package com.thebifel.kadr.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.RenderEnvironment
import com.thebifel.kadr.domain.port.EnvironmentProvider

object LocaleSupport {
    fun language(context: Context): String =
        if (context.resources.configuration.locales[0].language == "uk") "uk" else "en"

    fun localized(context: Context): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(language(context)))
        return context.createConfigurationContext(configuration)
    }
}

open class LocalizedActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) =
        super.attachBaseContext(LocaleSupport.localized(newBase))
}

class AndroidEnvironment(private val context: Context) : EnvironmentProvider {
    override fun current(project: Project) =
        RenderEnvironment(
            dark =
                project.resolveDark(
                    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                        Configuration.UI_MODE_NIGHT_YES
                ),
            locale = LocaleSupport.language(context),
        )
}
