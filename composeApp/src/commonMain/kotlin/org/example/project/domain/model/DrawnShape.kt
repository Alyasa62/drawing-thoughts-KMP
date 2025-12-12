package org.example.project.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

sealed class DrawnShape {
    abstract val id: String
    abstract val color: Color
    abstract val strokeWidth: Float
    abstract val drawingTool: DrawingTool

    data class FreeHand(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float,
        override val drawingTool: DrawingTool,
        val path: Path,
        val points: List<Offset> // Added for serialization
    ) : DrawnShape()

    data class Geometric(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float,
        override val drawingTool: DrawingTool,
        val start: Offset,
        val end: Offset
    ) : DrawnShape()
}
