package com.yasadevs.drawingthoughts.utils

actual object AppLogger {
    actual fun d(tag: String, message: String) {
        println("DEBUG [$tag] $message")
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        System.err.println("ERROR [$tag] $message")
        throwable?.printStackTrace()
    }
}
