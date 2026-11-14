package com.thebifel.kadr

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList

/** Follow the device language; unsupported languages fall back to English. */
object LocaleSupport {
    fun language(context: Context): String = if(context.resources.configuration.locales[0].language=="uk")"uk" else "en"
    fun localized(context: Context): Context {
        val configuration=Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(language(context)))
        return context.createConfigurationContext(configuration)
    }
}
object ContractText {
    fun contract(context: Context)=context.assets.open("contracts/sdk-${LocaleSupport.language(context)}.txt").bufferedReader().use { it.readText() }
    fun aiInstructions(context: Context)=context.assets.open("contracts/ai-${LocaleSupport.language(context)}.txt").bufferedReader().use { it.readText() }+"\n\n"+contract(context)
}
