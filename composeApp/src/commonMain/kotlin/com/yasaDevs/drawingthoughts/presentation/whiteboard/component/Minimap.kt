package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Minimap Component - Real-Time Canvas Replica
 *
 * ARCHITECTURE:
 * - Shows the entire 5000x5000 world as a scaled-down replica
 * - Draws ACTUAL shapes (paths, geometric primitives) not just bounding boxes
 * - Properly handles eraser with BlendMode.DstOut using offscreen compositing
 * - Displays a red viewport indicator showing the visible screen area
 * - Tap to jump: Centers the viewport on the tapped world coordinate
 *
 * CRITICAL FIXES:
 * - Uses drawPath() for FreeHand shapes instead of drawRect()
 * - Applies proper scaling to paths and stroke widths
 * - Offscreen compositing layer for eraser transparency support
 * - Renders all shape types (FreeHand, Geometric, Text)
 */
@Composable
fun Minimap(
    modifier: Modifier = Modifier,
    shapes: List<DrawnShape>,
    viewportZoom: Float,
    viewportPan: Offset,
    viewportSize: Size,
    onJumpTo: (Offset) -> Unit
) {
    // FIXED WORLD CONSTANTS (The "Bounded Infinite Canvas")
    val WORLD_WIDTH = 5000f
    val WORLD_HEIGHT = 5000f

    val worldBounds = Rect(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT)

    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.9f))
            .border(1.dp, Color.Black.copy(alpha = 0.2f))
    ) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val mapWidth = size.width.toFloat()
                    val mapHeight = size.height.toFloat()

                    val scaleX = mapWidth / WORLD_WIDTH
                    val scaleY = mapHeight / WORLD_HEIGHT

                    // Convert tap position to world coordinates
                    val worldX = tapOffset.x / scaleX
                    val worldY = tapOffset.y / scaleY

                    val newPanX = (viewportSize.width / 2f) - (worldX * viewportZoom)
                    val newPanY = (viewportSize.height / 2f) - (worldY * viewportZoom)

                    onJumpTo(Offset(newPanX, newPanY))
                }
            }
            .graphicsLayer {
                // CRITICAL: Offscreen compositing for eraser support
                // This allows BlendMode.DstOut to subtract alpha from this layer only
                compositingStrategy = CompositingStrategy.Offscreen
            }
        ) {
            val mapWidth = size.width
            val mapHeight = size.height

            // Minimap scale factors
            val scaleX = mapWidth / WORLD_WIDTH
            val scaleY = mapHeight / WORLD_HEIGHT
            val minimapScale = min(scaleX, scaleY)

            // Draw all shapes as actual replica
            shapes.forEach { shape ->
                drawMinimapShape(shape, scaleX, scaleY, minimapScale)
            }

            // Calculate viewport bounds in world coordinates
            val vpLeftWorld = -viewportPan.x / viewportZoom
            val vpTopWorld = -viewportPan.y / viewportZoom
            val vpRightWorld = (viewportSize.width - viewportPan.x) / viewportZoom
            val vpBottomWorld = (viewportSize.height - viewportPan.y) / viewportZoom

            // Transform viewport bounds to minimap coordinates
            val mapVpLeft = vpLeftWorld * scaleX
            val mapVpTop = vpTopWorld * scaleY
            val mapVpRight = vpRightWorld * scaleX
            val mapVpBottom = vpBottomWorld * scaleY

            // Clamp viewport indicator to minimap bounds
            val clampedLeft = mapVpLeft.coerceIn(0f, mapWidth)
            val clampedTop = mapVpTop.coerceIn(0f, mapHeight)
            val clampedRight = mapVpRight.coerceIn(0f, mapWidth)
            val clampedBottom = mapVpBottom.coerceIn(0f, mapHeight)

            val clampedWidth = (clampedRight - clampedLeft).coerceAtLeast(2f)
            val clampedHeight = (clampedBottom - clampedTop).coerceAtLeast(2f)

            // Draw viewport indicator (Red Box)
            drawRect(
                color = Color.Red,
                topLeft = Offset(clampedLeft, clampedTop),
                size = Size(clampedWidth, clampedHeight),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Draws a single shape on the minimap with proper scaling
 */
private fun DrawScope.drawMinimapShape(
    shape: DrawnShape,
    scaleX: Float,
    scaleY: Float,
    minimapScale: Float
) {
    // 1. Determine Properties
    var color = shape.color
    var blendMode = BlendMode.SrcOver
    var alpha = 1f
    var strokeWidth = shape.strokeWidth * minimapScale // Scale stroke width proportionally
    var pathEffect: PathEffect? = null
    var cap = StrokeCap.Round

    when (shape.drawingTool) {
        DrawingTool.ERASER -> {
            // CRITICAL: Use DstOut to punch transparent holes
            color = Color.Black
            blendMode = BlendMode.DstOut
            strokeWidth *= 1.5f
        }
        DrawingTool.HIGHLIGHTER -> {
            color = shape.color.copy(alpha = 0.4f)
            strokeWidth *= 2.5f
            cap = StrokeCap.Square
        }
        DrawingTool.LINE_DOTTED -> {
            // Scale dash pattern
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f * minimapScale, 20f * minimapScale), 0f)
        }
        else -> {
            // Standard tools
        }
    }

    // Ensure minimum stroke width visibility (0.5px minimum)
    strokeWidth = strokeWidth.coerceAtLeast(0.5f)

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
            // CRITICAL FIX: Draw actual path, not bounding rect
            val scaledPath = Path().apply {
                // Scale the path points
                shape.points.forEachIndexed { index, point ->
                    val scaledX = point.x * scaleX
                    val scaledY = point.y * scaleY
                    if (index == 0) {
                        moveTo(scaledX, scaledY)
                    } else {
                        lineTo(scaledX, scaledY)
                    }
                }
            }

            drawPath(
                path = scaledPath,
                color = color,
                alpha = alpha,
                style = style,
                blendMode = blendMode
            )
        }
        is DrawnShape.Text -> {
            // Text shapes: Draw a small colored box as placeholder
            // (Full text rendering at this scale would be illegible)
            val scaledX = shape.position.x * scaleX
            val scaledY = shape.position.y * scaleY
            val textSize = max(shape.fontSize * minimapScale, 2f)

            drawRect(
                color = color,
                topLeft = Offset(scaledX, scaledY),
                size = Size(textSize * 3f, textSize),
                alpha = alpha
            )
        }
        is DrawnShape.Image -> {
            val scaledX = shape.bounds.left * scaleX
            val scaledY = shape.bounds.top * scaleY
            val scaledW = shape.bounds.width * scaleX
            val scaledH = shape.bounds.height * scaleY
            
            drawRect(
                color = Color.LightGray,
                topLeft = Offset(scaledX, scaledY),
                size = Size(scaledW, scaledH),
                alpha = alpha
            )
            // Draw a tiny border to indicate it's an image block
            drawRect(
                color = Color.Gray,
                topLeft = Offset(scaledX, scaledY),
                size = Size(scaledW, scaledH),
                style = Stroke(width = max(1f, minimapScale)),
                alpha = alpha
            )
        }
        is DrawnShape.Geometric -> {
            // Scale start and end points
            val scaledStart = Offset(shape.start.x * scaleX, shape.start.y * scaleY)
            val scaledEnd = Offset(shape.end.x * scaleX, shape.end.y * scaleY)

            // Calculate scaled bounds
            val left = min(scaledStart.x, scaledEnd.x)
            val top = min(scaledStart.y, scaledEnd.y)
            val width = abs(scaledStart.x - scaledEnd.x)
            val height = abs(scaledStart.y - scaledEnd.y)
            val topLeft = Offset(left, top)
            val scaledSize = Size(width, height)

            when (shape.drawingTool) {
                DrawingTool.RECTANGLE_OUTLINED, DrawingTool.RECTANGLE_FILLED -> {
                    drawRect(color, topLeft, scaledSize, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.SQUARE_OUTLINED, DrawingTool.SQUARE_FILLED -> {
                    val sideLength = min(width, height)
                    val squareSize = Size(sideLength, sideLength)
                    drawRect(color, topLeft, squareSize, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.RECTANGLE_ROUNDED -> {
                    drawRoundRect(
                        color = color,
                        topLeft = topLeft,
                        size = scaledSize,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * minimapScale, 16f * minimapScale),
                        style = style,
                        alpha = alpha,
                        blendMode = blendMode
                    )
                }
                DrawingTool.CIRCLE_OUTLINED, DrawingTool.CIRCLE_FILLED -> {
                    val diameter = min(width, height)
                    drawOval(color, topLeft, Size(diameter, diameter), alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.ELLIPSE_OUTLINED, DrawingTool.ELLIPSE_FILLED -> {
                    drawOval(color, topLeft, scaledSize, alpha, style, colorFilter = null, blendMode)
                }
                DrawingTool.LINE_PLANE -> {
                    drawLine(color, scaledStart, scaledEnd, strokeWidth, cap, pathEffect, alpha, colorFilter = null, blendMode)
                }
                DrawingTool.LINE_DOTTED -> {
                    drawLine(color, scaledStart, scaledEnd, strokeWidth, cap, pathEffect, alpha, colorFilter = null, blendMode)
                }
                DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                    drawMinimapTriangle(color, scaledStart, scaledEnd, alpha, style, blendMode)
                }
                DrawingTool.ARROW_ONE_SIDED -> {
                    drawMinimapArrow(color, scaledStart, scaledEnd, strokeWidth, alpha, blendMode, bothSides = false)
                }
                DrawingTool.ARROW_TWO_SIDED -> {
                    drawMinimapArrow(color, scaledStart, scaledEnd, strokeWidth, alpha, blendMode, bothSides = true)
                }
                DrawingTool.STAR_OUTLINED, DrawingTool.STAR_FILLED -> {
                    drawMinimapStar(color, scaledStart, scaledEnd, alpha, style, blendMode)
                }
                DrawingTool.PENTAGON -> {
                    drawMinimapPolygon(5, color, scaledStart, scaledEnd, alpha, style, blendMode)
                }
                DrawingTool.HEXAGON -> {
                    drawMinimapPolygon(6, color, scaledStart, scaledEnd, alpha, style, blendMode)
                }
                DrawingTool.DIAMOND -> {
                    drawMinimapDiamond(color, scaledStart, scaledEnd, alpha, style, blendMode)
                }
                else -> {
                    // Fallback: draw as rect
                }
            }
        }
    }
}

// Helper drawing functions for minimap geometric shapes

private fun DrawScope.drawMinimapTriangle(
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

    drawPath(path, color, alpha, style, blendMode = blendMode)
}

private fun DrawScope.drawMinimapArrow(
    color: Color,
    start: Offset,
    end: Offset,
    strokeWidth: Float,
    alpha: Float,
    blendMode: BlendMode,
    bothSides: Boolean
) {
    val arrowHeadLength = strokeWidth * 4f
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

    drawLine(color, lineStart, lineEnd, strokeWidth, StrokeCap.Round, alpha = alpha, blendMode = blendMode)

    // Draw arrow heads
    drawMinimapArrowHead(end, angle, arrowHeadLength, color, alpha, blendMode)
    if (bothSides) {
        drawMinimapArrowHead(start, angle + Math.PI.toFloat(), arrowHeadLength, color, alpha, blendMode)
    }
}

private fun DrawScope.drawMinimapArrowHead(
    tip: Offset,
    angle: Float,
    length: Float,
    color: Color,
    alpha: Float,
    blendMode: BlendMode
) {
    val arrowAngle = 0.5f
    val path = Path().apply {
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
    drawPath(path, color, alpha, Fill, blendMode = blendMode)
}

private fun DrawScope.drawMinimapStar(
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

private fun DrawScope.drawMinimapPolygon(
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

private fun DrawScope.drawMinimapDiamond(
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
