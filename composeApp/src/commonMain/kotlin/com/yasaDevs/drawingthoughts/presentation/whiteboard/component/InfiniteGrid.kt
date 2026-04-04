package com.yasaDevs.drawingthoughts.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.yasaDevs.drawingthoughts.domain.model.CanvasPattern
import kotlin.math.floor

@Composable
fun InfiniteGrid(
    modifier: Modifier = Modifier,
    zoom: Float,
    pan: Offset,
    pattern: CanvasPattern = CanvasPattern.DOTS,
    dotColor: Color = Color.LightGray.copy(alpha = 0.5f),
    spacing: Float = 50f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (pattern == CanvasPattern.NONE) return@Canvas

        val density = density
        val baseSpacingPx = spacing * density
        val scaledSpacing = baseSpacingPx * zoom

        if (scaledSpacing < 10f) return@Canvas

        val startCol = floor((-pan.x / zoom) / baseSpacingPx).toInt()
        val endCol = floor(((size.width - pan.x) / zoom) / baseSpacingPx).toInt() + 1

        val startRow = floor((-pan.y / zoom) / baseSpacingPx).toInt()
        val endRow = floor(((size.height - pan.y) / zoom) / baseSpacingPx).toInt() + 1

        when (pattern) {
            CanvasPattern.DOTS -> {
                val dotRadius = 2f * density * (if (zoom < 1f) zoom else 1f)

                for (col in startCol..endCol) {
                    for (row in startRow..endRow) {
                        val worldX = col * baseSpacingPx
                        val worldY = row * baseSpacingPx

                        val screenX = worldX * zoom + pan.x
                        val screenY = worldY * zoom + pan.y

                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(screenX, screenY)
                        )
                    }
                }
            }

            CanvasPattern.GRID -> {
                val strokeWidth = 1f * density

                // Draw vertical lines
                for (col in startCol..endCol) {
                    val worldX = col * baseSpacingPx
                    val screenX = worldX * zoom + pan.x

                    drawLine(
                        color = dotColor,
                        start = Offset(screenX, 0f),
                        end = Offset(screenX, size.height),
                        strokeWidth = strokeWidth
                    )
                }

                // Draw horizontal lines
                for (row in startRow..endRow) {
                    val worldY = row * baseSpacingPx
                    val screenY = worldY * zoom + pan.y

                    drawLine(
                        color = dotColor,
                        start = Offset(0f, screenY),
                        end = Offset(size.width, screenY),
                        strokeWidth = strokeWidth
                    )
                }
            }

            CanvasPattern.LINES -> {
                val strokeWidth = 1f * density

                // Draw only horizontal lines (like notebook paper)
                for (row in startRow..endRow) {
                    val worldY = row * baseSpacingPx
                    val screenY = worldY * zoom + pan.y

                    drawLine(
                        color = dotColor,
                        start = Offset(0f, screenY),
                        end = Offset(size.width, screenY),
                        strokeWidth = strokeWidth
                    )
                }
            }

            CanvasPattern.NONE -> {
                // Already handled above, no-op
            }
        }
    }
}
