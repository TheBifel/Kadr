package com.thebifel.kadr.domain.model

object WidgetPolicy {
    const val controlsWidth = 144
    const val controlsHeight = 56
    const val maxHtmlLength = 500_000
    const val maxNameLength = 80
    val intervals = listOf(0, 15, 30, 60, 180, 360, 1440)

    private val localPage =
        Regex(
            """<meta\b(?=[^>]*\bname\s*=\s*["']kadr-network["'])(?=[^>]*\bcontent\s*=\s*["']none["'])[^>]*>""",
            RegexOption.IGNORE_CASE,
        )

    fun needsNetwork(html: String): Boolean = !localPage.containsMatchIn(html)

    fun validInterval(minutes: Int): Boolean = minutes in intervals

    fun canPublish(expected: Long, actual: Long): Boolean = expected == actual

    fun validate(project: Project) {
        require(project.id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        require(project.name.length <= maxNameLength)
        require(project.html.length <= maxHtmlLength)
        require(project.radius in listOf(0, 16, 24))
        require(validInterval(project.interval))
        require(project.revision > 0)
    }

    fun renderSize(widthDp: Int, heightDp: Int): Pair<Int, Int> {
        val width = widthDp.coerceIn(140, 640)
        val height = heightDp.coerceIn(70, 640)
        val scale = minOf(2.0, 720.0 / width, 480.0 / height)
        return (width * scale).toInt() to (height * scale).toInt()
    }
}
