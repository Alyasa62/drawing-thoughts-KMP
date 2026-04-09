package com.yasaDevs.drawingthoughts.utils

import java.io.File
import com.yasaDevs.drawingthoughts.data.local.AndroidWrappedContext

actual object LocalFileStorage {
    actual fun saveImage(bytes: ByteArray, fileName: String): String {
        val context = AndroidWrappedContext.context ?: throw IllegalStateException("Context not initialized")
        val file = File(context.filesDir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun loadImage(fileName: String): ByteArray? {
        val context = AndroidWrappedContext.context ?: return null
        val file = File(context.filesDir, fileName)
        return if (file.exists()) file.readBytes() else null
    }
}
