package com.yasaDevs.drawingthoughts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yasaDevs.drawingthoughts.presentation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.yasaDevs.drawingthoughts.data.local.AndroidWrappedContext.context = applicationContext

        val imageSaver = com.yasaDevs.drawingthoughts.utils.AndroidImageSaver(applicationContext)
        setContent {
            App(imageSaver)
        }
    }
}

