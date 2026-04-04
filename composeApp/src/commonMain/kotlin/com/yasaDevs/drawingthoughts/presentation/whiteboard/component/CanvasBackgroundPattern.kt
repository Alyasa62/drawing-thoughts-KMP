package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import com.yasaDevs.drawingthoughts.domain.model.CanvasPattern

/**
 * Draws the background pattern on the canvas based on the selected pattern type.
 * This should be called BEFORE drawing user strokes.
 *
 * @param pattern The selected canvas pattern
 * @param patternColor Color of the pattern (defaults to light gray)
 * @param spacing Spacing between pattern elements in dp (defaults to 40dp as per spec)
 */
fun DrawScope.drawCanvasBackgroundPattern(
    pattern: CanvasPattern,
    patternColor: Color = Color.LightGray.copy(alpha = 0.3f),
    spacing: Float = 40.dp.toPx()
) {
    when (pattern) {
        CanvasPattern.DOTS -> {
            drawDotsPattern(patternColor, spacing)
        }

        CanvasPattern.GRID -> {
            drawGridPattern(patternColor, spacing)
        }

        CanvasPattern.LINES -> {
            drawLinesPattern(patternColor, spacing)
        }

        CanvasPattern.NONE -> {
            // Draw nothing - solid background color
        }
    }
}

/**
 * Draws small circles (dots) spaced evenly across the canvas.
 * Pattern: Dots with radius ~2px spaced 40dp apart.
 */
private fun DrawScope.drawDotsPattern(
    color: Color,
    spacing: Float
) {
    val dotRadius = 2.dp.toPx()

    var y = spacing
    while (y < size.height) {
        var x = spacing
        while (x < size.width) {
            drawCircle(
                color = color,
                radius = dotRadius,
                center = Offset(x, y)
            )
            x += spacing
        }
        y += spacing
    }
}

/**
 * Draws thin horizontal and vertical lines to create a grid pattern.
 * Pattern: Thin lines spaced 40dp apart forming squares.
 */
private fun DrawScope.drawGridPattern(
    color: Color,
    spacing: Float
) {
    val strokeWidth = 1.dp.toPx()

    // Draw vertical lines
    var x = spacing
    while (x < size.width) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = strokeWidth
        )
        x += spacing
    }

    // Draw horizontal lines
    var y = spacing
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += spacing
    }
}

/**
 * Draws only horizontal lines like ruled notebook paper.
 * Pattern: Horizontal lines spaced 40dp apart.
 */
private fun DrawScope.drawLinesPattern(
    color: Color,
    spacing: Float
) {
    val strokeWidth = 1.dp.toPx()

    var y = spacing
    while (y < size.height) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth
        )
        y += spacing
    }
}
