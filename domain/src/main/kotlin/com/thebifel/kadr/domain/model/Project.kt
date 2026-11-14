package com.thebifel.kadr.domain.model

import java.util.UUID

/** User-authored source and refresh preferences, independent of Android. */
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
    val autoTheme: Boolean = true,
) {
    fun resolveDark(systemDark: Boolean): Boolean = if (autoTheme) systemDark else dark
}
