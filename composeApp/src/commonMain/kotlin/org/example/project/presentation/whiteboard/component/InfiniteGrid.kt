package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.floor

@Composable
fun InfiniteGrid(
    modifier: Modifier = Modifier,
    zoom: Float,
    pan: Offset,
    dotColor: Color = Color.LightGray.copy(alpha = 0.5f),
    spacing: Float = 50f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val density = density
        val baseSpacingPx = spacing * density
        val scaledSpacing = baseSpacingPx * zoom

        if (scaledSpacing < 10f) return@Canvas

        val dotRadius = 2f * density * (if (zoom < 1f) zoom else 1f) // Scale dot slightly but clamp

        
        val startCol = floor((-pan.x / zoom) / baseSpacingPx).toInt()
        val endCol = floor(((size.width - pan.x) / zoom) / baseSpacingPx).toInt() + 1
        
        val startRow = floor((-pan.y / zoom) / baseSpacingPx).toInt()
        val endRow = floor(((size.height - pan.y) / zoom) / baseSpacingPx).toInt() + 1
        
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
}
