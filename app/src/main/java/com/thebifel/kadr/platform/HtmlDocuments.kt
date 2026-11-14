package com.thebifel.kadr.platform

import android.content.ContentResolver
import android.net.Uri
import com.thebifel.kadr.domain.model.WidgetPolicy

class HtmlDocuments(private val resolver: ContentResolver) {
    fun read(uri: Uri): String =
        checkNotNull(resolver.openInputStream(uri)).use { stream ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size() + count <= WidgetPolicy.maxHtmlLength) {
                    "The page is too large"
                }
                output.write(buffer, 0, count)
            }
            output.toString("UTF-8")
        }

    fun write(uri: Uri, html: String) {
        checkNotNull(resolver.openOutputStream(uri)).bufferedWriter().use { it.write(html) }
    }
}
