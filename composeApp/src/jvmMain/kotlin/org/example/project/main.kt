package org.example.project

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.example.project.presentation.App
import androidx.compose.ui.res.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DrawingThoughts",
        icon = painterResource("app_icon.png")
    ) {
        App(org.example.project.utils.DesktopImageSaver())
    }
}