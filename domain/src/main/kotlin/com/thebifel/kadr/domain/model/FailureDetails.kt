package com.thebifel.kadr.domain.model

/** Keep actionable stacks while bounding persistence and the clipboard payload. */
object FailureDetails {
    private const val MAX_LENGTH = 65_536
    private const val TRUNCATED = "\n[Diagnostic text truncated at 65,536 characters.]"

    fun limit(message: String): String =
        if (message.length <= MAX_LENGTH) message
        else message.take(MAX_LENGTH - TRUNCATED.length) + TRUNCATED

    fun from(error: Throwable): String = limit(error.stackTraceToString())
}
