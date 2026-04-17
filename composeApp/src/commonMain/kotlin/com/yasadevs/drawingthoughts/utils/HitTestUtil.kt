package com.yasadevs.drawingthoughts.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.yasadevs.drawingthoughts.domain.model.DrawnShape
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
            if (shape.drawingTool == com.yasadevs.drawingthoughts.domain.model.DrawingTool.ERASER) return@find false
            
            isPointInShape(shape, point)
        }
    }

    private fun isPointInShape(shape: DrawnShape, point: Offset): Boolean {
        return when (shape) {
            is DrawnShape.Geometric -> isPointInGeometric(shape, point)
            is DrawnShape.FreeHand -> isPointInFreeHand(shape, point)
            is DrawnShape.Text -> isPointInText(shape, point)
            is DrawnShape.Image -> {
                val expanded = Rect(
                    shape.bounds.left - HIT_TOLERANCE,
                    shape.bounds.top - HIT_TOLERANCE,
                    shape.bounds.right + HIT_TOLERANCE,
                    shape.bounds.bottom + HIT_TOLERANCE
                )
                expanded.contains(point)
            }
        }
    }

    private fun isPointInText(shape: DrawnShape.Text, point: Offset): Boolean {
        // Use the estimated bounds from GeometryHelper
        val bounds = GeometryHelper.run { shape.getBounds() }
        return bounds.contains(point)
    }
    
    fun getTextBounds(shape: DrawnShape.Text, textMeasurer: TextMeasurer): Rect {
        if (shape.text.isEmpty()) {
            // Empty text - return a small clickable area
            return Rect(
                left = shape.position.x,
                top = shape.position.y,
                right = shape.position.x + shape.fontSize * 2f,
                bottom = shape.position.y + shape.fontSize * 1.5f
            )
        }

        val textStyle = TextStyle(
            fontSize = shape.fontSize.sp,
            fontFamily = shape.fontFamily,
            fontWeight = shape.fontWeight,
            fontStyle = shape.fontStyle,
            color = shape.color
        )

        val textLayoutResult = textMeasurer.measure(
            text = shape.text,
            style = textStyle
        )

        return Rect(
            left = shape.position.x,
            top = shape.position.y,
            right = shape.position.x + textLayoutResult.size.width,
            bottom = shape.position.y + textLayoutResult.size.height
        )
    }

    /**
     * Check if a point is inside a text shape using accurate text measurement.
     * Use this method in Composable context for precise hit testing.
     */
    fun isPointInTextAccurate(shape: DrawnShape.Text, point: Offset, textMeasurer: TextMeasurer): Boolean {
        val bounds = getTextBounds(shape, textMeasurer)
        return bounds.contains(point)
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
