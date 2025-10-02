package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnPath

data class WhiteBoardState(
    val paths: List<DrawnPath> = emptyList(),
    val currentPath: DrawnPath? = null,
    val startingOffset: Offset? = Offset.Zero,
    val selectedTool: DrawingTool = DrawingTool.PEN,
    val isDrawingToolCardVisible: Boolean = false
)
