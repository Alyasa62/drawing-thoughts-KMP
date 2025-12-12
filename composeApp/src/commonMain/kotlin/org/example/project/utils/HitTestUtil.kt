package org.example.project.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import org.example.project.domain.model.DrawnShape
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object HitTestUtil {

    private const val HIT_TOLERANCE = 40f

    fun getShapeAt(shapes: List<DrawnShape>, point: Offset): DrawnShape? {
        // Iterate reversed to select top-most shape first
        return shapes.asReversed().find { shape ->
            // FILTER: Strict visibility check
            // 1. Erasers are "invisible" (background color), so ignore them for selection.
            if (shape.drawingTool == org.example.project.domain.model.DrawingTool.ERASER) return@find false
            
            isPointInShape(shape, point)
        }
    }

    private fun isPointInShape(shape: DrawnShape, point: Offset): Boolean {
        return when (shape) {
            is DrawnShape.Geometric -> isPointInGeometric(shape, point)
            is DrawnShape.FreeHand -> isPointInFreeHand(shape, point)
        }
    }

    private fun isPointInGeometric(shape: DrawnShape.Geometric, point: Offset): Boolean {
        val left = min(shape.start.x, shape.end.x) - HIT_TOLERANCE
        val right = max(shape.start.x, shape.end.x) + HIT_TOLERANCE
        val top = min(shape.start.y, shape.end.y) - HIT_TOLERANCE
        val bottom = max(shape.start.y, shape.end.y) + HIT_TOLERANCE
        
        return point.x in left..right && point.y in top..bottom
    }

    private fun isPointInFreeHand(shape: DrawnShape.FreeHand, point: Offset): Boolean {
        // Simple bounding box check for FreeHand for now (Optimization)
        
        var minX = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY

        shape.points.forEach { 
            if (it.x < minX) minX = it.x
            if (it.x > maxX) maxX = it.x
            if (it.y < minY) minY = it.y
            if (it.y > maxY) maxY = it.y
        }
        
        val expandedBounds = Rect(
            minX - HIT_TOLERANCE, 
            minY - HIT_TOLERANCE, 
            maxX + HIT_TOLERANCE, 
            maxY + HIT_TOLERANCE
        )
        
        return expandedBounds.contains(point)
    }
    
    // Resize Handle logic
    enum class Handle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, NONE }

    fun getResizeHandle(shape: DrawnShape.Geometric, point: Offset): Handle {
         val left = min(shape.start.x, shape.end.x)
        val right = max(shape.start.x, shape.end.x)
        val top = min(shape.start.y, shape.end.y)
        val bottom = max(shape.start.y, shape.end.y)
        
        val tolerance = 50f
        
        if (abs(point.x - left) < tolerance && abs(point.y - top) < tolerance) return Handle.TOP_LEFT
        if (abs(point.x - right) < tolerance && abs(point.y - top) < tolerance) return Handle.TOP_RIGHT
        if (abs(point.x - left) < tolerance && abs(point.y - bottom) < tolerance) return Handle.BOTTOM_LEFT
        if (abs(point.x - right) < tolerance && abs(point.y - bottom) < tolerance) return Handle.BOTTOM_RIGHT
        
        return Handle.NONE
    }
}
