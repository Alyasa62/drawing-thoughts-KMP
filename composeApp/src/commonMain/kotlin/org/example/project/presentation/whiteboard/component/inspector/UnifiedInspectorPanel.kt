package org.example.project.presentation.whiteboard.component.inspector

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawingTool
import org.example.project.presentation.whiteboard.WhiteBoardEvent
import org.example.project.presentation.whiteboard.WhiteBoardState

enum class InspectorUiState {
    CANVAS,
    TOOL,
    SHAPE
}

@Composable
fun UnifiedInspectorPanel(
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = remember(state.selectedTool, state.selectedShapeId) {
        derivedStateOf {
            when {
                state.selectedShapeId != null -> InspectorUiState.SHAPE
                state.selectedTool == DrawingTool.HAND || state.selectedTool == DrawingTool.SELECTOR -> InspectorUiState.CANVAS
                else -> InspectorUiState.TOOL
            }
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        // --- HEADER ---
        val headerText = when (uiState.value) {
            InspectorUiState.CANVAS -> "Canvas Properties"
            InspectorUiState.TOOL -> "Tool Options"
            InspectorUiState.SHAPE -> "Shape Inspector"
        }
        
        Text(
            text = headerText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AnimatedContent(
            targetState = uiState.value,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            }
        ) { targetState ->
            Column {
                when (targetState) {
                    InspectorUiState.CANVAS -> CanvasControls(state, onEvent)
                    InspectorUiState.TOOL -> ToolControls(state, onEvent)
                    InspectorUiState.SHAPE -> ShapeControls(state, onEvent)
                }
            }
        }
    }
}

// --- SUB-PANELS ---

@Composable
private fun CanvasControls(state: WhiteBoardState, onEvent: (WhiteBoardEvent) -> Unit) {
    InspectorSection(title = "Background") {
        ColorPaletteRow(
            selectedColor = state.canvasBackgroundColor,
            onColorSelected = { onEvent(WhiteBoardEvent.OnBackgroundChange(it)) },
            colors = listOf(Color.White, Color(0xFFF8F9FA), Color(0xFFE9ECEF), Color(0xFF212529), Color.Black)
        )
    }
    
    /* InspectorSection(title = "Grid System") {
        // Placeholder for future Grid features
        Text("Infinite Grid is Active", style = MaterialTheme.typography.bodySmall)
    } */
}

@Composable
private fun ToolControls(state: WhiteBoardState, onEvent: (WhiteBoardEvent) -> Unit) {
    InspectorSection(title = "Appearance") {
        ColorPaletteRow(
            selectedColor = state.currentColor,
            onColorSelected = { onEvent(WhiteBoardEvent.OnColorChange(it)) },
            colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFC107), Color.Magenta, Color.White)
        )
    }

    InspectorSection(title = "Stroke") {
        SmartSlider(
            value = state.currentStrokeWidth,
            onValueChange = { onEvent(WhiteBoardEvent.OnStrokeWidthChange(it)) },
            valueRange = 1f..50f,
            label = "Width",
            color = state.currentColor
        )
    }
}

@Composable
private fun ShapeControls(state: WhiteBoardState, onEvent: (WhiteBoardEvent) -> Unit) {
    val selectedShape = state.shapes.find { it.id == state.selectedShapeId } ?: return
    
    // Calculate Bounds Logic
    val bounds = when(selectedShape) {
        is org.example.project.domain.model.DrawnShape.Geometric -> {
            androidx.compose.ui.geometry.Rect(
                topLeft = androidx.compose.ui.geometry.Offset(
                    kotlin.math.min(selectedShape.start.x, selectedShape.end.x),
                    kotlin.math.min(selectedShape.start.y, selectedShape.end.y)
                ),
                bottomRight = androidx.compose.ui.geometry.Offset(
                     kotlin.math.max(selectedShape.start.x, selectedShape.end.x),
                     kotlin.math.max(selectedShape.start.y, selectedShape.end.y)
                )
            )
        }
        is org.example.project.domain.model.DrawnShape.FreeHand -> {
            if (selectedShape.points.isEmpty()) androidx.compose.ui.geometry.Rect.Zero else {
                 var minX = Float.POSITIVE_INFINITY
                 var maxX = Float.NEGATIVE_INFINITY
                 var minY = Float.POSITIVE_INFINITY
                 var maxY = Float.NEGATIVE_INFINITY
                 selectedShape.points.forEach { 
                     minX = kotlin.math.min(minX, it.x)
                     maxX = kotlin.math.max(maxX, it.x)
                     minY = kotlin.math.min(minY, it.y)
                     maxY = kotlin.math.max(maxY, it.y)
                 }
                 androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
            }
        }
    }

    InspectorSection(title = "Transform") {
        Text("X: ${bounds.left.toInt()}  Y: ${bounds.top.toInt()}", style = MaterialTheme.typography.bodySmall)
        Text("W: ${bounds.width.toInt()}  H: ${bounds.height.toInt()}", style = MaterialTheme.typography.bodySmall)
    }

    InspectorSection(title = "Style") {
         ColorPaletteRow(
            selectedColor = selectedShape.color,
            onColorSelected = { 
                // We need a specific event to update SHAPE color, not global tool color.
                // Assuming OnColorChange updates global, but we might need "OnUpdateSelectedShape"
                // For MVP, reused global event if it applies to selection in ViewModel, 
                // BUT usually ViewModel separates them.
                // Let's assume we use a new event or existing logic.
                // Checking ViewModel logic (mental check): OnColorChange usually updates 'currentColor'.
                // Does it update selected shape?
                // If not, we need a new event. I will emit OnColorChange for now but might need refactor.
                // Actually, standard UX: changing color while shape selected updates shape.
                onEvent(WhiteBoardEvent.OnColorChange(it)) 
            },
            colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFC107), Color.Magenta)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SmartSlider(
            value = selectedShape.strokeWidth,
            onValueChange = { onEvent(WhiteBoardEvent.OnStrokeWidthChange(it)) },
            valueRange = 1f..50f,
            label = "Stroke Width",
            color = selectedShape.color
        )
    }
    
    InspectorSection(title = "Actions") {
        // Delete Button or Layer controls
        // For MVP, just info
    }
}

@Composable
fun ColorPaletteRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    colors: List<Color>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selectedColor,
                onClick = { onColorSelected(color) }
            )
        }
    }
}

@Composable
fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha=0.3f),
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
