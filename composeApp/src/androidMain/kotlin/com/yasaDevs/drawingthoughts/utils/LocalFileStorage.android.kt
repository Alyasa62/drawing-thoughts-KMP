package com.yasadevs.drawingthoughts.utils

import java.io.File
import com.yasadevs.drawingthoughts.data.local.AndroidWrappedContext

actual object LocalFileStorage {
    actual fun saveImage(bytes: ByteArray, fileName: String): String {
        val context = AndroidWrappedContext.context ?: throw IllegalStateException("Context not initialized")
        val file = resolveInternalFile(context.filesDir, fileName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    actual fun loadImage(fileName: String): ByteArray? {
        val context = AndroidWrappedContext.context ?: return null
        val file = resolveInternalFile(context.filesDir, fileName)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun deleteImage(fileName: String): Boolean {
        return try {
            val context = AndroidWrappedContext.context ?: return false
            val file = resolveInternalFile(context.filesDir, fileName)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    private fun resolveInternalFile(filesDir: File, fileName: String): File {
        require(fileName.isNotBlank()) { "File name cannot be blank" }
        val sanitizedName = fileName.substringAfterLast('/').substringAfterLast('\\')
        require(sanitizedName == fileName) { "Invalid file name" }
        return File(filesDir, sanitizedName)
    }
}
