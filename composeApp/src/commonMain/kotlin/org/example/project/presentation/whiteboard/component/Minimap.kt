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

/**
 * Minimap Component
 *
 * ARCHITECTURE:
 * - Shows the entire 5000x5000 world (Black Box)
 * - Displays a red viewport indicator showing the visible screen area (Red Box)
 * - Tap to jump: Centers the viewport on the tapped world coordinate
 *
 * CRITICAL FIX:
 * - viewportSize parameter is now the ACTUAL SCREEN SIZE (e.g., 1920x1080), not world size
 * - Viewport calculation uses strict coordinate mapping: World -> Minimap
 * - Red box is clamped to never exceed the minimap bounds
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
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
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
        }) {
             val mapWidth = size.width.toFloat()
             val mapHeight = size.height.toFloat()

             // Minimap scale factors
             val scaleX = mapWidth / WORLD_WIDTH
             val scaleY = mapHeight / WORLD_HEIGHT

             shapes.forEach { shape ->
                 val bounds = org.example.project.utils.GeometryHelper.run { shape.getBounds() }

                 // Transform shape bounds from world coords to minimap coords
                 val left = bounds.left * scaleX
                 val top = bounds.top * scaleY
                 val right = bounds.right * scaleX
                 val bottom = bounds.bottom * scaleY

                 // Ensure minimum visibility (2px minimum size)
                 val w = max(right - left, 2f)
                 val h = max(bottom - top, 2f)

                 drawRect(
                     color = shape.color,
                     topLeft = Offset(left, top),
                     size = Size(w, h),
                     style = androidx.compose.ui.graphics.drawscope.Fill
                 )
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

             // Calculate viewport size in minimap coords
             val mapVpWidth = mapVpRight - mapVpLeft
             val mapVpHeight = mapVpBottom - mapVpTop

             // Clamp viewport indicator to minimap bounds
             // This prevents the red box from drifting outside the minimap
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
