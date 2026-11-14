package com.thebifel.kadr.presentation.editor

/** One-shot Android actions stay outside the composable and ViewModel state. */
data class EditorPlatformActions(
    val importHtml: () -> Unit,
    val exportHtml: (String) -> Unit,
    val copyAi: () -> Unit,
    val copyContract: () -> Unit,
    val contractText: String,
    val aiText: String,
)
