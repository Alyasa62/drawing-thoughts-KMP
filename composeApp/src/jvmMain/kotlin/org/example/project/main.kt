package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.presentation.App
import androidx.compose.ui.res.painterResource

fun main() = application {
    val icon = try {
        painterResource("app_icon.png")
    } catch (e: Exception) {
        null
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "DrawingThoughts",
        icon = icon
    ) {
        App(org.example.project.utils.DesktopImageSaver())
    }
}