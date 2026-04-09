package com.yasaDevs.drawingthoughts.utils

import java.io.File

actual object LocalFileStorage {
    actual fun saveImage(bytes: ByteArray, fileName: String): String {
        val appDir = File(System.getProperty("user.home"), ".drawingthoughts")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        val file = File(appDir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun loadImage(fileName: String): ByteArray? {
        val appDir = File(System.getProperty("user.home"), ".drawingthoughts")
        val file = File(appDir, fileName)
        return if (file.exists()) file.readBytes() else null
    }
}
