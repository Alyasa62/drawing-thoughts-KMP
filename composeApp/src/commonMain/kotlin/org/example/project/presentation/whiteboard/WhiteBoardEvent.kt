package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.DrawingTool

sealed interface WhiteBoardEvent {
    data class OnDrawingToolSelected(val tool: DrawingTool): WhiteBoardEvent
    data class StartDrawing(val offset: Offset): WhiteBoardEvent
    data class ContinueDrawing(val offset: Offset): WhiteBoardEvent
    data object FinishDrawing: WhiteBoardEvent
    data class OnShapeTransform(val zoom: Float, val pan: Offset, val rotation: Float): WhiteBoardEvent
    data object OnShapeTransformStart: WhiteBoardEvent
    data object OnShapeTransformEnd: WhiteBoardEvent
    data class OnResizeShape(val handle: org.example.project.utils.TransformHandle, val dragAmount: Offset): WhiteBoardEvent
    data object OnDeleteSelectedShape: WhiteBoardEvent
    
    // Viewport
    data class OnViewportChange(val zoom: Float, val pan: Offset) : WhiteBoardEvent
    data object OnFABClick: WhiteBoardEvent
    data object OnCloseDrawingToolsCard: WhiteBoardEvent
    
    // Undo/Redo
    data object OnUndo: WhiteBoardEvent
    data object OnRedo: WhiteBoardEvent

    // Properties
    data class OnStrokeWidthChange(val width: Float): WhiteBoardEvent
    data class OnColorChange(val color: Color): WhiteBoardEvent
    data class OnBackgroundChange(val color: Color): WhiteBoardEvent
}