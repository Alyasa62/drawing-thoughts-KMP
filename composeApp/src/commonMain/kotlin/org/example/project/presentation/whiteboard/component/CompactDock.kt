package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import org.example.project.domain.model.DrawingTool
import org.jetbrains.compose.resources.painterResource

@Composable
fun CompactDock(
    modifier: Modifier = Modifier,
    selectedTool: DrawingTool,
    onToolSelect: (DrawingTool) -> Unit, // Always non-null (pass HAND for view mode)
    onStrokeWidthChange: (Float) -> Unit, 
) {
    // Helper to toggle: If clicked == current, select HAND (null equivalent for drawing)
    fun toggleTool(tool: DrawingTool) {
        if (selectedTool == tool) {
            onToolSelect(DrawingTool.HAND)
        } else {
            onToolSelect(tool)
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50), 
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), 
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SELECTOR
            DockIcon(
                tool = DrawingTool.SELECTOR,
                isSelected = selectedTool == DrawingTool.SELECTOR,
                onClick = { toggleTool(DrawingTool.SELECTOR) }
            )

            // 2. PEN 
            Box(contentAlignment = Alignment.Center) {
                 DockIcon(
                    tool = DrawingTool.PEN,
                    isSelected = selectedTool == DrawingTool.PEN,
                    onClick = { toggleTool(DrawingTool.PEN) },
                    modifier = Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val delta = -dragAmount / 5f 
                            onStrokeWidthChange(delta)
                        }
                    }
                )
            }

            // 3. HIGHLIGHTER
             DockIcon(
                tool = DrawingTool.HIGHLIGHTER,
                isSelected = selectedTool == DrawingTool.HIGHLIGHTER,
                onClick = { toggleTool(DrawingTool.HIGHLIGHTER) }
            )

             // 4. ERASER
             DockIcon(
                tool = DrawingTool.ERASER,
                isSelected = selectedTool == DrawingTool.ERASER,
                onClick = { toggleTool(DrawingTool.ERASER) }
            )
            
            // 5. SHAPES
             DockIcon(
                tool = DrawingTool.RECTANGLE_OUTLINED,
                isSelected = selectedTool.isShape(),
                onClick = { toggleTool(DrawingTool.RECTANGLE_OUTLINED) } 
            )
        }
    }
}

@Composable
private fun DockIcon(
    tool: DrawingTool,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    // Colorful Icons: Use Unspecified when not selected to show original SVG colors
    val iconColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(tool.res),
            contentDescription = tool.name,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
