package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.presentation.whiteboard.WhiteBoardState

@Composable
fun DynamicHUD(
    state: WhiteBoardState,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onStrokeWidthClick: () -> Unit,
    onShapeSelected: (DrawingTool) -> Unit,
    onDeleteClick: () -> Unit,
    onFontSizeChange: (Float) -> Unit = {},
    onFontFamilyChange: (androidx.compose.ui.text.font.FontFamily) -> Unit = {},
    onFontWeightChange: (androidx.compose.ui.text.font.FontWeight) -> Unit = {},
    onFontStyleChange: (androidx.compose.ui.text.font.FontStyle) -> Unit = {},
    onCropClick: () -> Unit = {}
) {
    // Determine which HUD to show based on current tool
    val showDrawingHud = when (state.selectedTool) {
        DrawingTool.PEN,
        DrawingTool.HIGHLIGHTER,
        DrawingTool.LASER_PEN -> true
        else -> false
    }
    val showEraserHud = state.selectedTool == DrawingTool.ERASER
    val showSelectorHud = state.selectedTool == DrawingTool.SELECTOR && state.selectedShapeId != null
    val showShapeHud = state.selectedTool.isShape()
    val showTextHud = state.selectedTool == DrawingTool.TEXT && !state.isTextEditing

    // Text Tool HUD — only shown while the user has TEXT selected but is NOT actively editing.
    // While editing, the unified TextEditingLayer (scrim + panel) takes over exclusively.
    if (showTextHud) {
        TextToolHUD(
            state = state,
            visible = showTextHud,
            modifier = modifier,
            onColorClick = onColorClick,
            onFontSizeChange = onFontSizeChange,
            onFontFamilyChange = onFontFamilyChange,
            onFontWeightChange = onFontWeightChange,
            onFontStyleChange = onFontStyleChange
        )
    }

    AnimatedVisibility(
        visible = !state.isTextEditing && (showDrawingHud || showEraserHud || showSelectorHud || showShapeHud),
        enter = fadeIn() + scaleIn() + slideInVertically { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drawing Tools HUD (PEN, HIGHLIGHTER, LASER_PEN)
                if (showDrawingHud) {
                    // Tool icon indicator
                    Icon(
                        painter = org.jetbrains.compose.resources.painterResource(state.selectedTool.res),
                        contentDescription = state.selectedTool.name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    // Color Dot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(state.currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable { onColorClick() }
                    )

                    // Stroke Width (Clickable for adjustment)
                    Text(
                        text = "${state.currentStrokeWidth.toInt()}px",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onStrokeWidthClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Eraser HUD
                if (showEraserHud) {
                    // Eraser icon
                    Icon(
                        painter = org.jetbrains.compose.resources.painterResource(DrawingTool.ERASER.res),
                        contentDescription = "Eraser",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )

                    // Eraser size
                    Text(
                        text = "${state.currentStrokeWidth.toInt()}px",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onStrokeWidthClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Shape Tools HUD
                if (showShapeHud) {
                    val shapeTools = listOf(
                        DrawingTool.RECTANGLE_OUTLINED,
                        DrawingTool.ELLIPSE_OUTLINED,
                        DrawingTool.TRIANGLE_OUTLINED,
                        DrawingTool.DIAMOND,
                        DrawingTool.LINE_PLANE,
                        DrawingTool.ARROW_ONE_SIDED
                    )

                    shapeTools.forEach { tool ->
                        val isSelected = state.selectedTool == tool
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable { onShapeSelected(tool) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = org.jetbrains.compose.resources.painterResource(tool.res),
                                contentDescription = tool.name,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(state.currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable { onColorClick() }
                    )

                    // Stroke width for outlined shapes (hide for filled shapes)
                    val isFilled = state.selectedTool.name.contains("FILLED")
                    if (!isFilled) {
                        Text(
                            text = "${state.currentStrokeWidth.toInt()}px",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onStrokeWidthClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }


                if (showSelectorHud) {
                    val selectedShape = state.shapes.find { it.id == state.selectedShapeId }
                    val isImage = selectedShape?.drawingTool == DrawingTool.IMAGE || selectedShape is com.yasaDevs.drawingthoughts.domain.model.DrawnShape.Image
                    
                    Text(
                        text = "Selected",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )


                    if (isImage) {
                        val cropTint = if (state.isCropModeActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Edit, // Using Edit icon as a placeholder for Crop
                            contentDescription = "Crop",
                            tint = cropTint,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onCropClick() }
                        )
                    }

                    // Delete Action
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onDeleteClick() }
                    )
                }
            }
        }
    }
}
