package com.yasadevs.drawingthoughts.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AndroidImageSaver(private val context: Context) : PlatformImageSaver {
    override suspend fun saveImage(bitmap: ImageBitmap): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val androidBitmap = bitmap.asAndroidBitmap()
                val filename = "drawing_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(androidBitmap, filename)
                } else {
                    saveToAppExternalStorage(androidBitmap, filename)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun saveToMediaStore(bitmap: Bitmap, filename: String): Result<String> {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Drawing Thoughts")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return Result.failure(IllegalStateException("Failed to create MediaStore entry"))

        return try {
            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Bitmap compression failed")
                }
            } ?: throw IllegalStateException("Failed to open output stream")

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            Result.success("Saved to Gallery: $filename")
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            Result.failure(e)
        }
    }

    private fun saveToAppExternalStorage(bitmap: Bitmap, filename: String): Result<String> {
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return Result.failure(IllegalStateException("External storage is unavailable"))
        if (!directory.exists() && !directory.mkdirs()) {
            return Result.failure(IllegalStateException("Failed to create export directory"))
        }

        val outputFile = File(directory, filename)
        FileOutputStream(outputFile).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                throw IllegalStateException("Bitmap compression failed")
            }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(outputFile.absolutePath),
            arrayOf("image/png"),
            null
        )
        return Result.success("Saved image: ${outputFile.absolutePath}")
    }
}
