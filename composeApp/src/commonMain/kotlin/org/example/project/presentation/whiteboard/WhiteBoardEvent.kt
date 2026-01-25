package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.example.project.domain.model.CanvasPattern
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

    // Clear Canvas
    data object OnClearCanvasRequest: WhiteBoardEvent
    data object OnClearCanvasConfirm: WhiteBoardEvent
    data object OnClearCanvasCancel: WhiteBoardEvent

    // Export
    data object OnExportRequest: WhiteBoardEvent
    data object OnExportWholeCanvas: WhiteBoardEvent
    data object OnExportVisibleScreen: WhiteBoardEvent
    data object OnExportDialogDismiss: WhiteBoardEvent

    // Properties
    data class OnStrokeWidthChange(val width: Float): WhiteBoardEvent
    data class OnColorChange(val color: Color): WhiteBoardEvent
    data class OnBackgroundChange(val color: Color): WhiteBoardEvent
    data class OnToggleEraseMode(val enabled: Boolean): WhiteBoardEvent

    // Text Tool Events
    data class OnTextCreate(val position: Offset): WhiteBoardEvent
    data class OnTextEdit(val textId: String): WhiteBoardEvent
    data class OnTextChange(val text: String): WhiteBoardEvent
    data class OnTextColorChange(val color: Color): WhiteBoardEvent
    data class OnTextFontSizeChange(val fontSize: Float): WhiteBoardEvent
    data class OnTextFontFamilyChange(val fontFamily: FontFamily): WhiteBoardEvent
    data class OnTextFontWeightChange(val fontWeight: FontWeight): WhiteBoardEvent
    data class OnTextFontStyleChange(val fontStyle: FontStyle): WhiteBoardEvent
    data object OnTextCommit: WhiteBoardEvent
    data object OnTextCancel: WhiteBoardEvent

    // Folder System Events
    data class OnFolderSelect(val folderId: String?): WhiteBoardEvent
    data object OnCreateFolderRequest: WhiteBoardEvent
    data class OnCreateFolderConfirm(val name: String, val color: Color): WhiteBoardEvent
    data object OnCreateFolderCancel: WhiteBoardEvent
    data class OnDeleteFolder(val folder: org.example.project.domain.model.Folder): WhiteBoardEvent

    // Grid Settings Events
    data object OnGridSettingsRequest: WhiteBoardEvent
    data class OnCanvasPatternChange(val pattern: CanvasPattern): WhiteBoardEvent
    data object OnGridSettingsConfirm: WhiteBoardEvent
    data object OnGridSettingsCancel: WhiteBoardEvent

    // Style Studio Events
    data object OnStyleStudioRequest: WhiteBoardEvent
    data class OnStyleStudioBackgroundChange(val color: Color): WhiteBoardEvent
    data class OnStyleStudioStrokeChange(val color: Color): WhiteBoardEvent
    data class OnStyleStudioFillChange(val color: Color): WhiteBoardEvent
    data class OnStyleStudioStrokeWidthChange(val width: Float): WhiteBoardEvent
    data class OnStyleStudioAlphaChange(val alpha: Float): WhiteBoardEvent
    data object OnStyleStudioDismiss: WhiteBoardEvent
}