package org.example.project.presentation.whiteboard.component

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
import org.example.project.domain.model.DrawingTool
import org.example.project.presentation.whiteboard.WhiteBoardState

@Composable
fun DynamicHUD(
    state: WhiteBoardState,
    modifier: Modifier = Modifier,
    onColorClick: () -> Unit,
    onStrokeWidthClick: () -> Unit,
    onShapeSelected: (DrawingTool) -> Unit,
    onDeleteClick: () -> Unit
) {
    val showPenHud = state.selectedTool == DrawingTool.PEN || state.selectedTool == DrawingTool.HIGHLIGHTER
    val showSelectorHud = state.selectedTool == DrawingTool.SELECTOR
    val showShapeHud = state.selectedTool.isShape()

    AnimatedVisibility(
        visible = showPenHud || showSelectorHud || showShapeHud,
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
                if (showPenHud) {
                    // 1. Color Dot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(state.currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable { onColorClick() }
                    )
                    
                    // 2. Stroke Size Text (Clickable for Slider/Popup)
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

                if (showShapeHud) {
                    // Shape Gallery
                    val shapeTools = listOf(
                        DrawingTool.RECTANGLE_OUTLINED,
                        DrawingTool.CIRCLE_OUTLINED,
                        DrawingTool.TRIANGLE_OUTLINED,
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
                    
                    // Add Color Dot for shapes too (Use Fill or Stroke Color?)
                    // Assuming same 'currentColor' applies for now
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(state.currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                            .clickable { onColorClick() }
                    )
                }

                if (showSelectorHud) {
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
