package com.yasadevs.drawingthoughts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yasadevs.drawingthoughts.presentation.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.yasadevs.drawingthoughts.data.local.AndroidWrappedContext.context = applicationContext

        val imageSaver = com.yasadevs.drawingthoughts.utils.AndroidImageSaver(applicationContext)
        setContent {
            App(imageSaver)
        }
    }
}

