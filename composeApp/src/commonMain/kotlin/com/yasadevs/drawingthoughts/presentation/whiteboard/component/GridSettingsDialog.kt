package com.yasadevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yasadevs.drawingthoughts.domain.model.CanvasPattern

/**
 * Grid Settings Dialog UI Component.
 * Shows a 2x2 grid of circular pattern preview buttons with selection state.
 */
@Composable
fun GridSettingsDialog(
    selectedPattern: CanvasPattern,
    onPatternSelect: (CanvasPattern) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempSelection by remember(selectedPattern) { mutableStateOf(selectedPattern) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Show grid",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 2x2 Grid Layout
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // First Row: Dots and Grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PatternOptionButton(
                            pattern = CanvasPattern.DOTS,
                            isSelected = tempSelection == CanvasPattern.DOTS,
                            onClick = { tempSelection = CanvasPattern.DOTS }
                        )
                        PatternOptionButton(
                            pattern = CanvasPattern.GRID,
                            isSelected = tempSelection == CanvasPattern.GRID,
                            onClick = { tempSelection = CanvasPattern.GRID }
                        )
                    }

                    // Second Row: Lines and None
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PatternOptionButton(
                            pattern = CanvasPattern.LINES,
                            isSelected = tempSelection == CanvasPattern.LINES,
                            onClick = { tempSelection = CanvasPattern.LINES }
                        )
                        PatternOptionButton(
                            pattern = CanvasPattern.NONE,
                            isSelected = tempSelection == CanvasPattern.NONE,
                            onClick = { tempSelection = CanvasPattern.NONE }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onPatternSelect(tempSelection)
                    onConfirm()
                }
            ) {
                Text("Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Circular button showing a mini preview of the pattern with selection state.
 */
@Composable
private fun PatternOptionButton(
    pattern: CanvasPattern,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                shape = CircleShape
            )
            .background(Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Mini Pattern Preview
        androidx.compose.foundation.Canvas(
            modifier = Modifier.size(64.dp)
        ) {
            drawPatternPreview(pattern)
        }

        // Blue Checkmark Overlay for Selected State
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
            )
        }
    }
}

/**
 * Draws a mini preview of the pattern inside the circular button.
 */
private fun DrawScope.drawPatternPreview(pattern: CanvasPattern) {
    val patternColor = Color.Gray.copy(alpha = 0.5f)
    val spacing = 12.dp.toPx() // Smaller spacing for preview

    when (pattern) {
        CanvasPattern.DOTS -> {
            // Draw small dots in a grid pattern
            val dotRadius = 1.5.dp.toPx()
            var y = spacing
            while (y < size.height) {
                var x = spacing
                while (x < size.width) {
                    drawCircle(
                        color = patternColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    x += spacing
                }
                y += spacing
            }
        }

        CanvasPattern.GRID -> {
            // Draw thin horizontal and vertical lines
            val strokeWidth = 0.8.dp.toPx()

            // Vertical lines
            var x = spacing
            while (x < size.width) {
                drawLine(
                    color = patternColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
                x += spacing
            }

            // Horizontal lines
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = patternColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
                y += spacing
            }
        }

        CanvasPattern.LINES -> {
            // Draw only horizontal lines (like notebook paper)
            val strokeWidth = 0.8.dp.toPx()
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = patternColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
                y += spacing
            }
        }

        CanvasPattern.NONE -> {
            // Draw an "X" or diagonal lines to indicate "none"
            val strokeWidth = 1.dp.toPx()
            drawLine(
                color = patternColor,
                start = Offset(size.width * 0.3f, size.height * 0.3f),
                end = Offset(size.width * 0.7f, size.height * 0.7f),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = patternColor,
                start = Offset(size.width * 0.7f, size.height * 0.3f),
                end = Offset(size.width * 0.3f, size.height * 0.7f),
                strokeWidth = strokeWidth
            )
        }
    }
}
