package com.yasadevs.drawingthoughts.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ShareHandler(): (String) -> Unit {
    val context = LocalContext.current
    val logTag = "ShareHandler"
    return remember(context) {
        { text: String ->
            try {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Share App")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
                AppLogger.d(logTag, "Opened Android share sheet")
            } catch (e: Exception) {
                AppLogger.e(logTag, "Failed to open Android share sheet", e)
            }
        }
    }
}
