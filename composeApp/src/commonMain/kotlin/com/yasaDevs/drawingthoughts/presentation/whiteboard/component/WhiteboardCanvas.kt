package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import com.yasaDevs.drawingthoughts.presentation.whiteboard.state.ViewportState
import com.yasaDevs.drawingthoughts.utils.GeometryHelper.getBounds
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
    isCropModeActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val zoom = viewportState.zoom
    val pan = viewportState.pan
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Apply viewport transform on the layer for better performance
                // NO compositingStrategy here - parent handles it
                // CRITICAL: Set transform origin to top-left (0,0) for correct coordinate mapping
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
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
            if (shape is DrawnShape.Text) {
                drawTextShape(shape, isSelected, shapeAlpha, textMeasurer)
            } else {
                drawSingleShape(shape, isSelected, shapeAlpha, isCropModeActive)
            }
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

        currentShape?.let {
            if (it is DrawnShape.Text) {
                drawTextShape(it, false, 1f, textMeasurer)
            } else {
                drawSingleShape(it, false, 1f, isCropModeActive)
            }
        }
    }
}

private fun DrawScope.drawSingleShape(shape: DrawnShape, isSelected: Boolean, baseAlpha: Float = 1f, isCropModeActive: Boolean = false) {
    // 1. Determine Properties
    var color = shape.color
    var blendMode = BlendMode.SrcOver
    var alpha = baseAlpha
    var strokeWidth = shape.strokeWidth
    var pathEffect: PathEffect? = null
    var cap = StrokeCap.Round

    when (shape.drawingTool) {
        DrawingTool.ERASER -> {

            color = Color.Black
            blendMode = BlendMode.DstOut
            strokeWidth *= 1.5f
        }
        DrawingTool.HIGHLIGHTER -> {
            // Semi-transparent additive tint
            color = shape.color.copy(alpha = 0.4f)
            strokeWidth *= 2.5f
            // Use Round cap to ensure capsule-like appearance at both ends
            cap = StrokeCap.Round
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
             val isSingleTapDot = if (shape.points.isEmpty()) {
                false
            } else if (shape.points.size <= 2) {
                true // Definitely a tap
            } else {
                 val firstPoint = shape.points[0]
                shape.points.all { point ->
                    val dx = point.x - firstPoint.x
                    val dy = point.y - firstPoint.y
                    kotlin.math.sqrt(dx * dx + dy * dy) < 2f
                }
            }

            if (isSingleTapDot && (shape.drawingTool == DrawingTool.PEN ||
                                   shape.drawingTool == DrawingTool.HIGHLIGHTER)) {
                // Draw as a filled circle dot
                val center = if (shape.points.isNotEmpty()) shape.points[0] else Offset.Zero
                val radius = shape.strokeWidth / 2f
                drawCircle(
                    color = color,
                    radius = radius,
                    center = center,
                    alpha = alpha,
                    style = Fill,
                    blendMode = blendMode
                )
            } else {
                // Draw as normal path with stroke
                drawPath(
                    path = shape.path,
                    color = color,
                    alpha = alpha,
                    style = style,
                    blendMode = blendMode
                )
            }
        }
        is DrawnShape.Text -> {
            // Text shapes are not drawn here - they're handled separately by drawTextShape
        }
        is DrawnShape.Image -> {
            val validCropRect = shape.cropRect ?: shape.bounds
            if (shape.bitmap != null) {
                withTransform({
                    clipRect(
                        left = validCropRect.left,
                        top = validCropRect.top,
                        right = validCropRect.right,
                        bottom = validCropRect.bottom
                    )
                }) {
                    drawImage(
                        image = shape.bitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(shape.bounds.left.toInt(), shape.bounds.top.toInt()),
                        dstSize = androidx.compose.ui.unit.IntSize(shape.bounds.width.toInt(), shape.bounds.height.toInt()),
                        alpha = alpha,
                        blendMode = blendMode
                    )
                }
            }
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

    if (isSelected && shape.drawingTool != DrawingTool.ERASER) {
        drawSelectionHighlight(shape, isCropModeActive)
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
        moveTo((left + right) / 2f, top)
        lineTo(right, bottom)
        lineTo(left, bottom)
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
    val arrowHeadLength = strokeWidth * 4f
    val arrowHeadWidth = strokeWidth * 2.5f

    val angle = atan2(end.y - start.y, end.x - start.x)

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

    drawLine(
        color = color,
        start = lineStart,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
        alpha = alpha,
        blendMode = blendMode
    )

    drawArrowHead(end, angle, arrowHeadLength, arrowHeadWidth, color, alpha, blendMode)

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
    val arrowAngle = 0.5f

    val path = Path().apply {
        // Arrow tip point
        moveTo(tip.x, tip.y)

        lineTo(
            tip.x - length * cos(angle - arrowAngle),
            tip.y - length * sin(angle - arrowAngle)
        )

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

private fun DrawScope.drawSelectionHighlight(shape: DrawnShape, isCropModeActive: Boolean = false) {

    val bounds = if (shape is DrawnShape.Image && isCropModeActive) {
        shape.cropRect ?: shape.bounds
    } else {
        shape.getBounds()
    }
    
    val padding = 10f
    val handleRadius = 6f

    val rectLeft = bounds.left - padding
    val rectTop = bounds.top - padding
    val rectRight = bounds.right + padding
    val rectBottom = bounds.bottom + padding

    val rectTopLeft = Offset(rectLeft, rectTop)
    val rectSize = Size(rectRight - rectLeft, rectBottom - rectTop)

    val primaryColor = if (isCropModeActive) Color(0xFF4CAF50) else Color(0xFF18A0FB)

    drawRect(
        color = primaryColor,
        topLeft = rectTopLeft,
        size = rectSize,
        style = Stroke(width = 2f)
    )

    val handleColor = Color.White
    val handleStrokeColor = primaryColor

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

private fun DrawScope.drawTextShape(
    shape: DrawnShape.Text,
    isSelected: Boolean,
    baseAlpha: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val textLayoutResult = textMeasurer.measure(
        text = shape.text,
        style = TextStyle(
            color = shape.color.copy(alpha = baseAlpha),
            fontSize = shape.fontSize.sp,
            fontFamily = shape.fontFamily,
            fontWeight = shape.fontWeight,
            fontStyle = shape.fontStyle
        )
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = shape.position
    )

    if (isSelected) {
        // Draw selection highlight around text
        val textSize = Size(textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat())
        drawRect(
            color = Color(0xFF18A0FB).copy(alpha = 0.3f),
            topLeft = shape.position - Offset(4f, 4f),
            size = Size(textSize.width + 8f, textSize.height + 8f),
            style = Stroke(width = 2f)
        )
    }
}
