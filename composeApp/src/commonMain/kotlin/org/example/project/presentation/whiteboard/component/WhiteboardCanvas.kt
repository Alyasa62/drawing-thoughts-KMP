package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape
import org.example.project.presentation.whiteboard.state.ViewportState
import org.example.project.utils.GeometryHelper.getBounds
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * WhiteboardCanvas: The INK LAYER
 *
 * ARCHITECTURE NOTE:
 * This canvas is wrapped in an offscreen compositing layer at the parent level (WhiteBoardScreen).
 * Therefore, BlendMode.DstOut used by the eraser will subtract alpha from THIS isolated layer only,
 * revealing the grid and white background beneath without punching through to the window.
 *
 * DO NOT apply compositingStrategy here - it's already handled by the parent wrapper.
 */
@Composable
fun WhiteboardCanvas(
    shapes: List<DrawnShape>,
    currentShape: DrawnShape?,
    selectionShapeId: String?,
    viewportState: ViewportState,
    isDragging: Boolean = false,
    dragStartPosition: Offset? = null,
    modifier: Modifier = Modifier
) {
    val zoom = viewportState.zoom
    val pan = viewportState.pan

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Apply viewport transform on the layer for better performance
                // NO compositingStrategy here - parent handles it
                scaleX = zoom
                scaleY = zoom
                translationX = pan.x
                translationY = pan.y
            }
    ) {
        // Draw Committed Shapes
        shapes.forEach { shape ->
            // If dragging, render the selected shape with shadow effect (reduced opacity)
            val isSelected = shape.id == selectionShapeId
            val shapeAlpha = if (isDragging && isSelected) 0.5f else 1f
            drawSingleShape(shape, isSelected, shapeAlpha)
        }

        // Draw trace line from original position to current position while dragging
        if (isDragging && dragStartPosition != null && selectionShapeId != null) {
            val selectedShape = shapes.find { it.id == selectionShapeId }
            selectedShape?.let { shape ->
                val bounds = shape.getBounds()
                val currentCenter = Offset(
                    (bounds.left + bounds.right) / 2f,
                    (bounds.top + bounds.bottom) / 2f
                )

                // Draw dashed line from original to current position
                drawLine(
                    color = Color(0xFF18A0FB).copy(alpha = 0.6f),
                    start = dragStartPosition,
                    end = currentCenter,
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Draw small circle at original position
                drawCircle(
                    color = Color(0xFF18A0FB).copy(alpha = 0.4f),
                    radius = 8f,
                    center = dragStartPosition,
                    style = Fill
                )
                drawCircle(
                    color = Color(0xFF18A0FB),
                    radius = 8f,
                    center = dragStartPosition,
                    style = Stroke(width = 2f)
                )
            }
        }

        // Draw Active Shape (Currently being drawn)
        currentShape?.let {
            drawSingleShape(it, false, 1f)
        }
    }
}

private fun DrawScope.drawSingleShape(shape: DrawnShape, isSelected: Boolean, baseAlpha: Float = 1f) {
    // 1. Determine Properties
    var color = shape.color
    var blendMode = BlendMode.SrcOver
    var alpha = baseAlpha
    var strokeWidth = shape.strokeWidth
    var pathEffect: PathEffect? = null
    var cap = StrokeCap.Round

    when (shape.drawingTool) {
        DrawingTool.ERASER -> {
            // TRUE TRANSPARENCY ERASER
            // DstOut = "Destination Out" Porter-Duff mode
            // Formula: [Da * (1 - Sa), Dc * (1 - Sa)]
            // Effect: Uses source alpha as a "mask" to subtract from destination alpha.
            // Since we're in an offscreen layer, this creates transparent holes in the ink,
            // revealing the layers beneath (grid + white paper), NOT the window background.
            color = Color.Black // Opaque mask (alpha = 1.0)
            blendMode = BlendMode.DstOut
            strokeWidth *= 1.5f
        }
        DrawingTool.HIGHLIGHTER -> {
            // Semi-transparent additive tint
            color = shape.color.copy(alpha = 0.4f)
            strokeWidth *= 2.5f
            cap = StrokeCap.Square
        }
        DrawingTool.LINE_DOTTED -> {
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
        }
        else -> {
            // Standard tools (PEN, shapes, etc.)
        }
    }

    // 2. Determine Style (Fill vs Stroke)
    val isFilled = shape.drawingTool == DrawingTool.CIRCLE_FILLED ||
                   shape.drawingTool == DrawingTool.RECTANGLE_FILLED ||
                   shape.drawingTool == DrawingTool.SQUARE_FILLED ||
                   shape.drawingTool == DrawingTool.TRIANGLE_FILLED ||
                   shape.drawingTool == DrawingTool.STAR_FILLED ||
                   shape.drawingTool == DrawingTool.ELLIPSE_FILLED

    val style = if (isFilled) {
        Fill
    } else {
        Stroke(
            width = strokeWidth,
            cap = cap,
            pathEffect = pathEffect
        )
    }

    // 3. Draw
    when (shape) {
        is DrawnShape.FreeHand -> {
            drawPath(
                path = shape.path,
                color = color,
                alpha = alpha,
                style = style,
                blendMode = blendMode
            )
        }
        is DrawnShape.Geometric -> {
            // Calculate Bounds
            val left = min(shape.start.x, shape.end.x)
            val top = min(shape.start.y, shape.end.y)
            val width = abs(shape.start.x - shape.end.x)
            val height = abs(shape.start.y - shape.end.y)
            val topLeft = Offset(left, top)
            val size = Size(width, height)

            when (shape.drawingTool) {
                DrawingTool.RECTANGLE_OUTLINED, DrawingTool.RECTANGLE_FILLED -> {
                    drawRect(color, topLeft, size, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.SQUARE_OUTLINED, DrawingTool.SQUARE_FILLED -> {
                    // For square, use the smaller dimension for both width and height
                    val sideLength = min(width, height)
                    val squareSize = Size(sideLength, sideLength)
                    drawRect(color, topLeft, squareSize, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.RECTANGLE_ROUNDED -> {
                    drawRoundRect(
                        color = color,
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                        style = style,
                        alpha = alpha,
                        colorFilter = null,
                        blendMode = blendMode
                    )
                }
                DrawingTool.CIRCLE_OUTLINED, DrawingTool.CIRCLE_FILLED -> {
                    val diameter = min(width, height)
                    drawOval(color, topLeft, Size(diameter, diameter), alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.ELLIPSE_OUTLINED, DrawingTool.ELLIPSE_FILLED -> {
                    drawOval(color, topLeft, size, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.LINE_PLANE -> {
                     drawLine(color, shape.start, shape.end, strokeWidth, cap, pathEffect, alpha, colorFilter = null, blendMode)
                }
                DrawingTool.LINE_DOTTED -> {
                     drawLine(color, shape.start, shape.end, strokeWidth, cap, pathEffect, alpha, colorFilter = null, blendMode)
                }
                DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                    drawTriangle(color, shape.start, shape.end, alpha, style, blendMode)
                }
                DrawingTool.ARROW_ONE_SIDED -> {
                    drawArrow(color, shape.start, shape.end, strokeWidth, alpha, blendMode, bothSides = false)
                }
                DrawingTool.ARROW_TWO_SIDED -> {
                    drawArrow(color, shape.start, shape.end, strokeWidth, alpha, blendMode, bothSides = true)
                }
                DrawingTool.STAR_OUTLINED, DrawingTool.STAR_FILLED -> {
                    drawStar(color, shape.start, shape.end, alpha, style, blendMode)
                }
                DrawingTool.PENTAGON -> {
                    drawPolygon(5, color, shape.start, shape.end, alpha, style, blendMode)
                }
                DrawingTool.HEXAGON -> {
                    drawPolygon(6, color, shape.start, shape.end, alpha, style, blendMode)
                }
                DrawingTool.DIAMOND -> {
                    drawDiamond(color, shape.start, shape.end, alpha, style, blendMode)
                }
                else -> {
                    // Fallback
                }
            }
        }
    }

    // 4. Selection Overlay (Don't outline erasers or invisible strokes)
    if (isSelected && shape.drawingTool != DrawingTool.ERASER) {
        drawSelectionHighlight(shape)
    }
}

private fun DrawScope.drawTriangle(
    color: Color,
    start: Offset,
    end: Offset,
    alpha: Float,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle,
    blendMode: BlendMode
) {
    val left = min(start.x, end.x)
    val right = max(start.x, end.x)
    val top = min(start.y, end.y)
    val bottom = max(start.y, end.y)

    val path = Path().apply {
        // Top point (center-top)
        moveTo((left + right) / 2f, top)
        // Bottom-right
        lineTo(right, bottom)
        // Bottom-left
        lineTo(left, bottom)
        // Close path
        close()
    }

    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = style,
        blendMode = blendMode
    )
}

private fun DrawScope.drawArrow(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    alpha: Float,
    blendMode: BlendMode,
    bothSides: Boolean
) {
    // Calculate arrow head size based on stroke width
    val arrowHeadLength = strokeWidth * 4f
    val arrowHeadWidth = strokeWidth * 2.5f

    // Calculate angle of the line
    val angle = atan2(end.y - start.y, end.x - start.x)

    // Shorten the line to end where arrow head starts
    val lineEnd = Offset(
        end.x - arrowHeadLength * 0.7f * cos(angle),
        end.y - arrowHeadLength * 0.7f * sin(angle)
    )

    val lineStart = if (bothSides) {
        Offset(
            start.x + arrowHeadLength * 0.7f * cos(angle),
            start.y + arrowHeadLength * 0.7f * sin(angle)
        )
    } else start

    // Draw main line (shortened to accommodate arrow heads)
    drawLine(
        color = color,
        start = lineStart,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        alpha = alpha,
        blendMode = blendMode
    )

    // Draw arrow head at end (at the actual end point)
    drawArrowHead(end, angle, arrowHeadLength, arrowHeadWidth, color, alpha, blendMode)

    // Draw arrow head at start if two-sided (at the actual start point)
    if (bothSides) {
        drawArrowHead(start, angle + Math.PI.toFloat(), arrowHeadLength, arrowHeadWidth, color, alpha, blendMode)
    }
}

private fun DrawScope.drawArrowHead(
    tip: Offset,
    angle: Float,
    length: Float,
    width: Float,
    color: Color,
    alpha: Float,
    blendMode: BlendMode
) {
    // Create a proper arrow head pointing in the direction of the line
    val arrowAngle = 0.5f // ~30 degrees for each wing

    val path = Path().apply {
        // Arrow tip point
        moveTo(tip.x, tip.y)

        // Left wing - back from tip at angle
        lineTo(
            tip.x - length * cos(angle - arrowAngle),
            tip.y - length * sin(angle - arrowAngle)
        )

        // Right wing - back from tip at angle
        lineTo(
            tip.x - length * cos(angle + arrowAngle),
            tip.y - length * sin(angle + arrowAngle)
        )

        close()
    }

    drawPath(
        path = path,
        color = color,
        alpha = alpha,
        style = Fill,
        blendMode = blendMode
    )
}

private fun DrawScope.drawSelectionHighlight(shape: DrawnShape) {
    // Figma-style selection with corner handles
    val bounds = shape.getBounds()
    val padding = 10f
    val handleRadius = 6f

    val rectLeft = bounds.left - padding
    val rectTop = bounds.top - padding
    val rectRight = bounds.right + padding
    val rectBottom = bounds.bottom + padding

    val rectTopLeft = Offset(rectLeft, rectTop)
    val rectSize = Size(rectRight - rectLeft, rectBottom - rectTop)

    // Draw selection rectangle
    drawRect(
        color = Color(0xFF18A0FB), // Figma blue
        topLeft = rectTopLeft,
        size = rectSize,
        style = Stroke(width = 2f)
    )

    // Draw corner handles (4 small circles)
    val handleColor = Color.White
    val handleStrokeColor = Color(0xFF18A0FB)

    // Top-left handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = Offset(rectLeft, rectTop),
        style = Fill
    )
    drawCircle(
        color = handleStrokeColor,
        radius = handleRadius,
        center = Offset(rectLeft, rectTop),
        style = Stroke(width = 2f)
    )

    // Top-right handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = Offset(rectRight, rectTop),
        style = Fill
    )
    drawCircle(
        color = handleStrokeColor,
        radius = handleRadius,
        center = Offset(rectRight, rectTop),
        style = Stroke(width = 2f)
    )

    // Bottom-left handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = Offset(rectLeft, rectBottom),
        style = Fill
    )
    drawCircle(
        color = handleStrokeColor,
        radius = handleRadius,
        center = Offset(rectLeft, rectBottom),
        style = Stroke(width = 2f)
    )

    // Bottom-right handle
    drawCircle(
        color = handleColor,
        radius = handleRadius,
        center = Offset(rectRight, rectBottom),
        style = Fill
    )
    drawCircle(
        color = handleStrokeColor,
        radius = handleRadius,
        center = Offset(rectRight, rectBottom),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawStar(
    color: Color,
    start: Offset,
    end: Offset,
    alpha: Float,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle,
    blendMode: BlendMode
) {
    val centerX = (start.x + end.x) / 2f
    val centerY = (start.y + end.y) / 2f
    val radiusX = abs(end.x - start.x) / 2f
    val radiusY = abs(end.y - start.y) / 2f

    val path = Path().apply {
        val points = 5
        val outerRadius = max(radiusX, radiusY)
        val innerRadius = outerRadius * 0.4f

        for (i in 0 until points * 2) {
            val angle = (i * Math.PI / points).toFloat() - (Math.PI / 2).toFloat()
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            if (i == 0) moveTo(x, y)
            else lineTo(x, y)
        }
        close()
    }

    drawPath(path, color, alpha, style, blendMode = blendMode)
}

private fun DrawScope.drawPolygon(
    sides: Int,
    color: Color,
    start: Offset,
    end: Offset,
    alpha: Float,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle,
    blendMode: BlendMode
) {
    val centerX = (start.x + end.x) / 2f
    val centerY = (start.y + end.y) / 2f
    val radius = min(abs(end.x - start.x), abs(end.y - start.y)) / 2f

    val path = Path().apply {
        for (i in 0 until sides) {
            val angle = (i * 2 * Math.PI / sides).toFloat() - (Math.PI / 2).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)

            if (i == 0) moveTo(x, y)
            else lineTo(x, y)
        }
        close()
    }

    drawPath(path, color, alpha, style, blendMode = blendMode)
}

private fun DrawScope.drawDiamond(
    color: Color,
    start: Offset,
    end: Offset,
    alpha: Float,
    style: androidx.compose.ui.graphics.drawscope.DrawStyle,
    blendMode: BlendMode
) {
    val centerX = (start.x + end.x) / 2f
    val centerY = (start.y + end.y) / 2f
    val width = abs(end.x - start.x) / 2f
    val height = abs(end.y - start.y) / 2f

    val path = Path().apply {
        moveTo(centerX, centerY - height)  // Top
        lineTo(centerX + width, centerY)   // Right
        lineTo(centerX, centerY + height)  // Bottom
        lineTo(centerX - width, centerY)   // Left
        close()
    }

    drawPath(path, color, alpha, style, blendMode = blendMode)
}
