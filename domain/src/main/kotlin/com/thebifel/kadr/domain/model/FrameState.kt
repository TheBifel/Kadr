package com.thebifel.kadr.domain.model

/** Failure and pending state never replace the last successful frame reference. */
data class FrameState(
    val file: String = "",
    val success: Long = 0,
    val attempt: Long = 0,
    val error: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val revision: Long = 0,
    val dark: Boolean = false,
    val pending: Long = 0,
)

data class RenderSize(val width: Int = 320, val height: Int = 160)

data class RenderEnvironment(val dark: Boolean, val locale: String)
