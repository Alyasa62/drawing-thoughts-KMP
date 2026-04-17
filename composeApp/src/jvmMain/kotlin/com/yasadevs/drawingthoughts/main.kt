package com.yasadevs.drawingthoughts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.yasadevs.drawingthoughts.presentation.App
import androidx.compose.ui.res.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DrawingThoughts",
        icon = painterResource("app_icon.png")
    ) {
        App(com.yasadevs.drawingthoughts.utils.DesktopImageSaver())
    }
}