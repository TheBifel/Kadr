package com.thebifel.kadr

import android.content.Context

object Templates {
    fun page(context: Context,name: String): String {
        require(name in listOf("power","weather","blank"))
        return context.assets.open("templates/$name-${LocaleSupport.language(context)}.html").bufferedReader().use { it.readText() }
    }
}
