package com.yasaDevs.drawingthoughts.utils

import androidx.compose.ui.graphics.ImageBitmap

interface PlatformImageSaver {
    suspend fun saveImage(bitmap: ImageBitmap): Result<String>
}
