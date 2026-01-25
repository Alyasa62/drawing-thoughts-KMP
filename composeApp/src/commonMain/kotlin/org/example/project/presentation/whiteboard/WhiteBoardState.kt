package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.example.project.domain.model.CanvasPattern
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape

data class WhiteBoardState(
    val selectedTool: DrawingTool = DrawingTool.HAND,
    val isDrawingToolCardVisible: Boolean = false,
    val selectedShapeId: String? = null,
    val currentShape: DrawnShape? = null,
    val shapes: List<DrawnShape> = emptyList(),
    val startingOffset: Offset? = null,

    // Viewport State (Camera)
    val zoom: Float = 1f,
    val pan: Offset = Offset.Zero,

    // Per-Tool Settings (Each tool remembers its own color and stroke width)
    val toolSettings: Map<DrawingTool, ToolSettings> = ToolSettingsDefaults.createDefaultSettingsMap(),

    // Canvas Settings
    val canvasBackgroundColor: Color = Color.White,
    val selectedPattern: CanvasPattern = CanvasPattern.DEFAULT,

    // Shape Transform State
    val transientScale: Float = 1f,
    val transientOffset: Offset = Offset.Zero,
    val transientRotation: Float = 0f,

    // Eraser Mode
    val isObjectEraserEnabled: Boolean = false,

    // Drag State
    val isDragging: Boolean = false,
    val dragStartPosition: Offset? = null,

    // Text Tool State
    val isTextEditing: Boolean = false,
    val editingTextId: String? = null,
    val currentTextContent: String = "",
    val textFontSize: Float = 24f,
    val textFontFamily: FontFamily = FontFamily.Default,
    val textFontWeight: FontWeight = FontWeight.Normal,
    val textFontStyle: FontStyle = FontStyle.Normal,

    // Clear Canvas Dialog State
    val showClearConfirmDialog: Boolean = false,

    // Export Dialog State
    val showExportDialog: Boolean = false,

    // Folder System State
    val folders: List<org.example.project.domain.model.Folder> = emptyList(),
    val selectedFolderId: String? = null, // null means "All Drawings"
    val showCreateFolderDialog: Boolean = false,

    // Grid Settings Dialog State
    val showGridSettingsDialog: Boolean = false
) {
    /**
     * Get the current tool's color setting
     */
    val currentColor: Color
        get() = toolSettings[selectedTool]?.color ?: Color.Black

    /**
     * Get the current tool's stroke width setting
     */
    val currentStrokeWidth: Float
        get() = toolSettings[selectedTool]?.strokeWidth ?: 10f
}
