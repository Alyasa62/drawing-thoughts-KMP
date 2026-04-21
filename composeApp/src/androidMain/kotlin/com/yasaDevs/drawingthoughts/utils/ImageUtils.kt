package com.yasadevs.drawingthoughts.utils

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

private const val MAX_IMAGE_BYTES = 20 * 1024 * 1024
private const val MAX_IMAGE_DIMENSION = 4096

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    require(isNotEmpty()) { "Image is empty" }
    require(size <= MAX_IMAGE_BYTES) { "Image is too large" }

    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)

    val sourceWidth = bounds.outWidth
    val sourceHeight = bounds.outHeight
    require(sourceWidth > 0 && sourceHeight > 0) { "Unsupported or corrupt image" }

    var sampleSize = 1
    while (sourceWidth / sampleSize > MAX_IMAGE_DIMENSION || sourceHeight / sampleSize > MAX_IMAGE_DIMENSION) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    }
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size, options)
        ?: throw IllegalArgumentException("Unsupported or corrupt image")
    return bitmap.asImageBitmap()
}
