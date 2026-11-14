package com.thebifel.kadr

import android.app.Application
import android.content.res.Configuration
import android.webkit.WebView
import com.thebifel.kadr.di.AppContainer
import com.thebifel.kadr.platform.LocaleSupport

class KadrApp : Application() {
    val container by lazy { AppContainer(this) }
    private var language = ""
    private var night = 0

    override fun onCreate() {
        super.onCreate()
        WebView.enableSlowWholeDocumentDraw()
        language = LocaleSupport.language(this)
        night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val nextLanguage = LocaleSupport.language(this)
        val nextNight = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (language == nextLanguage && night == nextNight) return
        with(container) {
            repository.signalConfigurationChange()
            repository
                .bindings()
                .values
                .distinct()
                .mapNotNull(repository::get)
                .filter { language != nextLanguage || it.autoTheme }
                .forEach { requestRefresh(it.id, replace = true) }
        }
        language = nextLanguage
        night = nextNight
    }
}
