package com.yasadevs.drawingthoughts.utils

expect object LocalFileStorage {
    fun saveImage(bytes: ByteArray, fileName: String): String
    fun loadImage(fileName: String): ByteArray?
    fun deleteImage(fileName: String): Boolean
}
