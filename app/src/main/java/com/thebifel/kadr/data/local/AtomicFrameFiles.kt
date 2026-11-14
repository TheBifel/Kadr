package com.thebifel.kadr.data.local

import android.graphics.BitmapFactory
import android.util.AtomicFile
import com.thebifel.kadr.data.rendering.BitmapFrame
import com.thebifel.kadr.domain.port.FrameImage
import java.io.File

/** Files are committed before metadata is switched to the new filename. */
class AtomicFrameFiles(directory: File) {
    private val directory = directory.apply { mkdirs() }

    fun file(name: String): File? =
        name
            .takeIf { it.endsWith(".png") && it.matches(Regex("[a-zA-Z0-9._-]+")) }
            ?.let { File(directory, it) }

    fun read(name: String): FrameImage? =
        file(name)?.let { BitmapFactory.decodeFile(it.path) }?.let(::BitmapFrame)

    fun write(projectId: String, image: FrameImage): String {
        val target = File(directory, "$projectId-${System.nanoTime()}.png")
        val atomic = AtomicFile(target)
        val stream = atomic.startWrite()
        try {
            check(image.writePng(stream)) { "Could not encode frame" }
            atomic.finishWrite(stream)
        } catch (error: Throwable) {
            atomic.failWrite(stream)
            throw error
        }
        return target.name
    }

    fun delete(name: String) {
        file(name)?.delete()
    }
}
