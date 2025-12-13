package org.example.project.presentation.whiteboard.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawnShape
import kotlin.math.min
import kotlin.math.abs

fun DrawScope.drawSelectionOverlay(
    shape: DrawnShape,
    zoom: Float
) {
    if (shape !is DrawnShape.Geometric && shape !is DrawnShape.FreeHand) return

    // Calculate Bounds & Center
    val bounds = when (shape) {
        is DrawnShape.Geometric -> {
            val start = shape.start
            val end = shape.end
            androidx.compose.ui.geometry.Rect(
                left = min(start.x, end.x),
                top = min(start.y, end.y),
                right = kotlin.math.max(start.x, end.x),
                bottom = kotlin.math.max(start.y, end.y)
            )
        }
        is DrawnShape.FreeHand -> {
             // For FreeHand, simple bounding box
            var minX = Float.POSITIVE_INFINITY
            var maxX = Float.NEGATIVE_INFINITY
            var minY = Float.POSITIVE_INFINITY
            var maxY = Float.NEGATIVE_INFINITY
            shape.points.forEach { 
                minX = min(minX, it.x)
                maxX = kotlin.math.max(maxX, it.x)
                minY = min(minY, it.y)
                maxY = kotlin.math.max(maxY, it.y)
            }
             androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
        }
    }
    
    val width = bounds.width
    val height = bounds.height
    val center = bounds.center

    // Draw Logic
    withTransform({
        // Rotation removed from model
        // rotate(shape.rotation, pivot = center) 
    }) {
        // 1. Blue Border
        val figmaBlue = Color(0xFF18A0FB)
        val borderStroke = 1.5.dp.toPx() / zoom // Scale stroke with zoom to keep constant screen thickness? 
        // Or usually handles stay constant screen size, but border scales? 
        // User asked for "Blue Bounding Box". Usually border is thin.
        
        drawRect(
            color = figmaBlue,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(width = borderStroke)
        )

        // 2. Handles
        val handleSize = 8.dp.toPx() / zoom // Keep handle size constant on screen? No, simpler to scale.
        // Actually for "Excalidraw" style, usually handles map to screen pixels (constant visual size).
        // But here we are drawing in World Coordinates.
        // So we must divide size by Zoom.
        val handleRadius = 4.dp.toPx() / zoom 
        
        val corners = listOf(
            bounds.topLeft,
            bounds.topRight,
            bounds.bottomLeft,
            bounds.bottomRight
        )
        
        // Corner Handles (Hollow Circles)
        corners.forEach { corner ->
             drawCircle(
                 color = Color.White,
                 radius = handleRadius,
                 center = corner
             )
             drawCircle(
                 color = figmaBlue,
                 radius = handleRadius,
                 center = corner,
                 style = Stroke(width = borderStroke)
             )
        }

        // Side Handles (Small Lines/Rects)
        val sides = listOf(
            bounds.topCenter,
            bounds.centerRight,
            bounds.bottomCenter,
            bounds.centerLeft
        )
        sides.forEach { side -> 
            drawRect(
                color = figmaBlue,
                topLeft = side - Offset(handleRadius/2, handleRadius/2),
                size = Size(handleRadius, handleRadius)
            )
        }
        
        // Rotation Handle (Lollipop)
        val rotationAnchor = bounds.topCenter - Offset(0f, 20.dp.toPx() / zoom)
        drawLine(
            color = figmaBlue,
            start = bounds.topCenter,
            end = rotationAnchor,
            strokeWidth = borderStroke
        )
        drawCircle(
             color = Color.White,
             radius = handleRadius,
             center = rotationAnchor
        )
        drawCircle(
             color = figmaBlue,
             radius = handleRadius,
             center = rotationAnchor,
             style = Stroke(width = borderStroke)
        )
    }
}
