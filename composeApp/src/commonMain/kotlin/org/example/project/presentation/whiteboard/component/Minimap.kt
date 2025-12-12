package org.example.project.presentation.whiteboard.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawnShape
import kotlin.math.max
import kotlin.math.min

@Composable
fun Minimap(
    modifier: Modifier = Modifier,
    shapes: List<DrawnShape>,
    viewportZoom: Float,
    viewportPan: Offset,
    viewportSize: Size,
    onJumpTo: (Offset) -> Unit
) {
    if (shapes.isEmpty()) return

    // 1. Calculate World Bounds
    // We cache this calculation or assume it's fast enough for ~100 shapes. For thousands, use derivedStateOf in VM.
    val worldBounds = remember(shapes) {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        
        shapes.forEach { shape ->
            val bounds = getShapeBounds(shape)
            minX = min(minX, bounds.left)
            minY = min(minY, bounds.top)
            maxX = max(maxX, bounds.right)
            maxY = max(maxY, bounds.bottom)
        }
        
        // Add padding
        val padding = 1000f // World units padding
        Rect(minX - padding, minY - padding, maxX + padding, maxY + padding)
    }

    // Aspect Ratio Logic
    // Minimap fits into the box, preserving aspect ratio of the world? 
    // Usually Minimap is a fixed square/rect implies non-uniform scale OR we toggle mapping.
    // Let's assume Uniform fill for simplicity.
    
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, Color.Gray)
            .pointerInput(worldBounds) {
                detectTapGestures { tapOffset ->
                    // Convert Tap(Minimap) -> World -> Viewport Pan
                    // ... Logic implemented inside Canvas commonly
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                // Reverse Mapping: Minimap -> World
                val mapWidth = size.width
                val mapHeight = size.height
                
                val worldWidth = worldBounds.width
                val worldHeight = worldBounds.height
                
                // Scale factors
                val scaleX = mapWidth / worldWidth
                val scaleY = mapHeight / worldHeight
                val scale = min(scaleX, scaleY) // Uniform scale to fit
                
                // Centering the world in the minimap
                val mapContentWidth = worldWidth * scale
                val mapContentHeight = worldHeight * scale
                val offsetX = (mapWidth - mapContentWidth) / 2
                val offsetY = (mapHeight - mapContentHeight) / 2
                
                // (Tap - Offset) / Scale + WorldMin = WorldTarget
                val worldTargetX = (tapOffset.x - offsetX) / scale + worldBounds.left
                val worldTargetY = (tapOffset.y - offsetY) / scale + worldBounds.top
                
                // We want this WorldTarget to be at the CENTER of the Viewport
                // World(center) = (Screen(center) - pan) / zoom
                // pan = Screen(center) - World(center) * zoom
                val screenCenterX = viewportSize.width / 2
                val screenCenterY = viewportSize.height / 2
                
                val newPanX = screenCenterX - worldTargetX * viewportZoom
                val newPanY = screenCenterY - worldTargetY * viewportZoom
                
                onJumpTo(Offset(newPanX, newPanY))
            }
        }) {
             val mapWidth = size.width
             val mapHeight = size.height
             val worldWidth = worldBounds.width
             val worldHeight = worldBounds.height
             
             val scaleX = mapWidth / worldWidth
             val scaleY = mapHeight / worldHeight
             val scale = min(scaleX, scaleY)
             
             val mapContentWidth = worldWidth * scale
             val mapContentHeight = worldHeight * scale
             val offsetX = (mapWidth - mapContentWidth) / 2
             val offsetY = (mapHeight - mapContentHeight) / 2
             
             // Helper transform function (World -> Minimap)
             fun toMap(worldPt: Offset): Offset {
                 val relX = worldPt.x - worldBounds.left
                 val relY = worldPt.y - worldBounds.top
                 return Offset(
                     offsetX + relX * scale,
                     offsetY + relY * scale
                 )
             }
             
             // 2. Draw Shapes (Simplified)
             shapes.forEach { shape ->
                 val bounds = getShapeBounds(shape)
                 val topLeft = toMap(bounds.topLeft)
                 val bottomRight = toMap(bounds.bottomRight)
                 val size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
                 
                 // Minimum size for visibility
                 val visSize = Size(max(size.width, 2f), max(size.height, 2f))
                 
                 drawRect(
                     color = shape.color,
                     topLeft = topLeft,
                     size = visSize,
                     style = androidx.compose.ui.graphics.drawscope.Fill
                 )
             }
             
             // 3. Draw Viewport Rect
             // Current Viewport in World Coords:
             // Left = -pan.x / zoom
             // Top = -pan.y / zoom
             // Right = (width - pan.x) / zoom
             val vpLeft = (-viewportPan.x / viewportZoom)
             val vpTop = (-viewportPan.y / viewportZoom)
             val vpRight = ((viewportSize.width - viewportPan.x) / viewportZoom)
             val vpBottom = ((viewportSize.height - viewportPan.y) / viewportZoom)
             
             val mapVpTopLeft = toMap(Offset(vpLeft, vpTop))
             val mapVpBottomRight = toMap(Offset(vpRight, vpBottom))
             val mapVpSize = Size(mapVpBottomRight.x - mapVpTopLeft.x, mapVpBottomRight.y - mapVpTopLeft.y)

             drawRect(
                 color = Color.Red,
                 topLeft = mapVpTopLeft,
                 size = mapVpSize,
                 style = Stroke(width = 2.dp.toPx())
             )
        }
    }
}

private fun getShapeBounds(shape: DrawnShape): Rect {
    return when (shape) {
        is DrawnShape.Geometric -> {
             Rect(
                 min(shape.start.x, shape.end.x),
                 min(shape.start.y, shape.end.y),
                 max(shape.start.x, shape.end.x),
                 max(shape.start.y, shape.end.y)
             )
        }
        is DrawnShape.FreeHand -> {
             var minX = Float.POSITIVE_INFINITY; var maxX = Float.NEGATIVE_INFINITY
             var minY = Float.POSITIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
             if (shape.points.isEmpty()) return Rect.Zero
             shape.points.forEach { minX = min(minX, it.x); maxX = max(maxX, it.x); minY = min(minY, it.y); maxY = max(maxY, it.y) }
             Rect(minX, minY, maxX, maxY)
        }
    }
}
