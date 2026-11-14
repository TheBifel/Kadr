package com.thebifel.kadr.domain.port

import com.thebifel.kadr.domain.model.Project
import com.thebifel.kadr.domain.model.RenderEnvironment
import com.thebifel.kadr.domain.model.RenderSize
import java.io.OutputStream

/** The refresh use case closes an image after publishing it. UI images are retained by the UI. */
interface FrameImage : AutoCloseable {
    val width: Int
    val height: Int

    fun writePng(output: OutputStream): Boolean
}

fun interface FrameRenderer {
    suspend fun render(
        project: Project,
        size: RenderSize,
        environment: RenderEnvironment,
    ): FrameImage
}

fun interface EnvironmentProvider {
    fun current(project: Project): RenderEnvironment
}
