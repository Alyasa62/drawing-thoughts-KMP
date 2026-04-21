package com.yasadevs.drawingthoughts.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun ShareHandler(): (String) -> Unit {
    return remember {
        { text: String ->
            try {
                val selection = StringSelection(text)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
                println("Copied to clipboard for sharing: $text")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
