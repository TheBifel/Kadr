package com.thebifel.kadr.data.rendering

import android.graphics.Bitmap
import com.thebifel.kadr.domain.port.FrameImage
import java.io.OutputStream

class BitmapFrame(val bitmap: Bitmap) : FrameImage {
    override val width: Int
        get() = bitmap.width

    override val height: Int
        get() = bitmap.height

    override fun writePng(output: OutputStream): Boolean =
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)

    override fun close() = bitmap.recycle()
}

fun FrameImage.asBitmap(): Bitmap = (this as BitmapFrame).bitmap
