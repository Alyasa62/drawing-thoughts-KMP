package com.yasadevs.drawingthoughts.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun ShareHandler(): (String) -> Unit {
    val logTag = "ShareHandler"
    return remember {
        { text: String ->
            try {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                AppLogger.d(logTag, "Copied text to clipboard for sharing")
            } catch (e: Exception) {
                AppLogger.e(logTag, "Failed to copy shared text to clipboard", e)
            }
        }
    }
}
