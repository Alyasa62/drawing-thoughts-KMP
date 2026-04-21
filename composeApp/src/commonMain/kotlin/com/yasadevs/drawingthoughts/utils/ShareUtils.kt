package com.yasadevs.drawingthoughts.utils

import androidx.compose.runtime.Composable

@Composable
expect fun ShareHandler(): (String) -> Unit
