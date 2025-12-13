package org.example.project.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object GeometryHelper {

    fun calculateTrianglePath(start: Offset, end: Offset): Path {
        val path = Path()
        // Top Center
        val topX = (start.x + end.x) / 2
        val topY = start.y
        
        // Bottom Left
        val bottomLeftX = start.x
        val bottomLeftY = end.y
        
        // Bottom Right
        val bottomRightX = end.x
        val bottomRightY = end.y

        path.moveTo(topX, topY)
        path.lineTo(bottomRightX, bottomRightY)
        path.lineTo(bottomLeftX, bottomLeftY)
        path.close()
        
        return path
    }

    fun calculateArrowPath(start: Offset, end: Offset, isDoubleSided: Boolean): Path {
        val path = Path()
        
        // Main Line
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)

        // Arrow Head at End
        addArrowHead(path, start, end)

        // Arrow Head at Start (if double sided)
        if (isDoubleSided) {
            addArrowHead(path, end, start)
        }

        return path
    }

    private fun addArrowHead(path: Path, from: Offset, to: Offset) {
        val angle = atan2(to.y - from.y, to.x - from.x)
        val arrowLength = 50f 
        val arrowAngle = Math.PI / 6 // 30 degrees

        // Right side of the arrowhead
        val x1 = to.x - arrowLength * cos(angle - arrowAngle)
        val y1 = to.y - arrowLength * sin(angle - arrowAngle)
        
        // Left side of the arrowhead
        val x2 = to.x - arrowLength * cos(angle + arrowAngle)
        val y2 = to.y - arrowLength * sin(angle + arrowAngle)

        path.moveTo(to.x, to.y)
        path.lineTo(x1.toFloat(), y1.toFloat())
        path.moveTo(to.x, to.y)
        path.lineTo(x2.toFloat(), y2.toFloat())
    }

    // --- HIT TESTING & HANDLES (Restored & Simplified) ---

    fun getHandleAtPoint(
        touchPoint: Offset, 
        bounds: androidx.compose.ui.geometry.Rect, 
        rotation: Float, // Kept in signature for compatibility, but ignored
        zoom: Float,
        density: androidx.compose.ui.unit.Density
    ): TransformHandle {
        val center = bounds.center
        // Rotation ignored as per new requirements
        val localTouch = touchPoint
        
        // Handle Size (World Coordinates)
        val handleHitRadius = with(density) { (25.dp.toPx() / zoom) } 
        
        // 1. Rotation Handle (Lollipop) - Ignored or kept? 
        // User removed rotation from model, so 'rotation' arg is likely 0.
        // We can disable rotation handle logic or keep it if model supports it.
        // User removed `rotation` field from ShapeEntity/DrawnShape?
        // Yes, checked Step 1488. 
        // So we should NOT return ROTATE handle.
        
        // 2. Corners
        if ((localTouch - bounds.topLeft).getDistance() <= handleHitRadius) return TransformHandle.TOP_LEFT
        if ((localTouch - bounds.topRight).getDistance() <= handleHitRadius) return TransformHandle.TOP_RIGHT
        if ((localTouch - bounds.bottomLeft).getDistance() <= handleHitRadius) return TransformHandle.BOTTOM_LEFT
        if ((localTouch - bounds.bottomRight).getDistance() <= handleHitRadius) return TransformHandle.BOTTOM_RIGHT
        
        // 3. Sides
        if ((localTouch - bounds.topCenter).getDistance() <= handleHitRadius) return TransformHandle.TOP
        if ((localTouch - bounds.bottomCenter).getDistance() <= handleHitRadius) return TransformHandle.BOTTOM
        if ((localTouch - bounds.centerLeft).getDistance() <= handleHitRadius) return TransformHandle.LEFT
        if ((localTouch - bounds.centerRight).getDistance() <= handleHitRadius) return TransformHandle.RIGHT
        
        // 4. Body (Move)
        if (bounds.contains(localTouch)) {
            return TransformHandle.BODY
        }
        
        return TransformHandle.NONE
    }

    // Extension method to get bounds from a shape
    fun org.example.project.domain.model.DrawnShape.getBounds(): androidx.compose.ui.geometry.Rect {
        return when (this) {
            is org.example.project.domain.model.DrawnShape.Geometric -> {
                val start = this.start
                val end = this.end
                androidx.compose.ui.geometry.Rect(
                    left = kotlin.math.min(start.x, end.x),
                    top = kotlin.math.min(start.y, end.y),
                    right = kotlin.math.max(start.x, end.x),
                    bottom = kotlin.math.max(start.y, end.y)
                )
            }
            is org.example.project.domain.model.DrawnShape.FreeHand -> {
                var minX = Float.POSITIVE_INFINITY
                var maxX = Float.NEGATIVE_INFINITY
                var minY = Float.POSITIVE_INFINITY
                var maxY = Float.NEGATIVE_INFINITY
                if (this.points.isEmpty()) return androidx.compose.ui.geometry.Rect.Zero
                
                this.points.forEach { 
                    minX = kotlin.math.min(minX, it.x)
                    maxX = kotlin.math.max(maxX, it.x)
                    minY = kotlin.math.min(minY, it.y)
                    maxY = kotlin.math.max(maxY, it.y)
                }
                androidx.compose.ui.geometry.Rect(minX, minY, maxX, maxY)
            }
        }
    }
}

enum class TransformHandle {
    TOP_LEFT, TOP, TOP_RIGHT,
    LEFT, RIGHT,
    BOTTOM_LEFT, BOTTOM, BOTTOM_RIGHT,
    ROTATE, // Kept for compat, but unused
    BODY,
    NONE
}
