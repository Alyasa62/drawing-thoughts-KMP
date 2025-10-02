package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import org.example.project.domain.model.DrawingTool

sealed class WhiteBoardEvent {
    data class StartDrawing(val offset: Offset) : WhiteBoardEvent()
    data class ContinueDrawing(val offset: Offset) : WhiteBoardEvent()
    data object FinishDrawing : WhiteBoardEvent()
    data class OnDrawingToolSelected(val tool: DrawingTool) : WhiteBoardEvent()
    data object OnCloseDrawingToolsCard : WhiteBoardEvent()
    data object OnFABClick : WhiteBoardEvent()


}