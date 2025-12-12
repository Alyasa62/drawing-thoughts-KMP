package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape

data class WhiteBoardState(
    val selectedTool: DrawingTool = DrawingTool.PEN,
    val isDrawingToolCardVisible: Boolean = false,
    val selectedShapeId: String? = null,
    val currentShape: DrawnShape? = null,
    val shapes: List<DrawnShape> = emptyList(),
    val startingOffset: Offset? = null,
    val currentStrokeWidth: Float = 10f,
    val currentColor: Color = Color.Black,
    val canvasBackgroundColor: Color = Color.White,
    val transientScale: Float = 1f,
    val transientOffset: Offset = Offset.Zero,
    val transientRotation: Float = 0f)
