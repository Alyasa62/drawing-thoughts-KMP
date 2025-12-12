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
    spacing: Float = 50f // Default spacing in dp (will be scaled)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val density = density
        val baseSpacingPx = spacing * density
        val scaledSpacing = baseSpacingPx * zoom
        
        // Don't draw if too small (performance/visual noise)
        if (scaledSpacing < 10f) return@Canvas

        val dotRadius = 2f * density * (if (zoom < 1f) zoom else 1f) // Scale dot slightly but clamp
        
        // Calculate visible range
        // The Viewport acts as a window. The dots are at World(x,y) = (col * spacing, row * spacing)
        // Screen(x,y) = World(x,y) * zoom + pan
        // Inverse: World(x,y) = (Screen(x,y) - pan) / zoom
        
        // Calculate offset for the grid "window"
        // We only draw dots that are visible on screen [0, size.width] x [0, size.height]
        
        // Optimization: We can just use modulo to draw a static grid that slides?
        // No, for "Infinite" feel with Zoom, we want the dots to be anchored to World Coordinates.
        
        // Find the first visible column/row index
        // screenX = 0 => worldX = -pan.x / zoom
        // col = floor(worldX / baseSpacingPx)
        
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
