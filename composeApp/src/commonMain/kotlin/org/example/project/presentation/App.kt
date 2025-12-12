package org.example.project.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.presentation.whiteboard.WhiteBoardScreen
import org.example.project.presentation.whiteboard.WhiteBoardViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

import org.example.project.utils.PlatformImageSaver

@Composable
fun App(imageSaver: PlatformImageSaver) {
    MaterialTheme {

        val viewModel = viewModel<WhiteBoardViewModel>()
        val state by viewModel.state.collectAsState()

        Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
            WhiteBoardScreen(
                modifier = Modifier.Companion.padding(innerPadding),
                state = state,
                onEvent = viewModel::onEvent,
                imageSaver = imageSaver
            )

        }
    }
}