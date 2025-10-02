package org.example.project.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

data class DrawnPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val drawingTool: DrawingTool
)
