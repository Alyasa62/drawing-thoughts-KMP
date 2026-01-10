package org.example.project.presentation.whiteboard.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class ViewportState(
    initialZoom: Float = 1f,
    initialPan: Offset = Offset.Zero,
    private val viewportSize: androidx.compose.ui.geometry.Size = androidx.compose.ui.geometry.Size(1000f, 1000f) // Default fallback
) {
    // MASSIVE WORLD CONSTANTS
    val WORLD_WIDTH = 5000f
    val WORLD_HEIGHT = 5000f
    
    var zoom by mutableStateOf(initialZoom)
        private set
    
    var pan by mutableStateOf(initialPan)
        private set

    // Updates the known viewport size for clamping (called from UI)
    var currentViewportSize by mutableStateOf(viewportSize)

    fun updateViewportSize(size: androidx.compose.ui.geometry.Size) {
        currentViewportSize = size
    }

    private fun clampPan(proposedPan: Offset, currentZoom: Float): Offset {
        // Calculate the maximum allowed scroll (negative values)
        // Logic: The Viewport (Red Box) is 'currentViewportSize'. 
        // The World (Black Box) is (WORLD_WIDTH * zoom, WORLD_HEIGHT * zoom).
        // Pan.x goes from 0 (Left edge) to -(WorldWidth*Zoom - ViewportWidth) (Right edge)
        
        val maxScrollX = -(WORLD_WIDTH * currentZoom - currentViewportSize.width).coerceAtLeast(0f)
        val maxScrollY = -(WORLD_HEIGHT * currentZoom - currentViewportSize.height).coerceAtLeast(0f)
        
        return Offset(
            x = proposedPan.x.coerceIn(maxScrollX, 0f),
            y = proposedPan.y.coerceIn(maxScrollY, 0f)
        )
    }

    fun transform(zoomChange: Float, panChange: Offset) {
        // Apply changes
        val newZoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
        val proposedPan = pan + panChange
        
        // Apply Clamping
        val clampedPan = clampPan(proposedPan, newZoom)
        
        zoom = newZoom
        pan = clampedPan
    }
    
    fun snapTo(targetZoom: Float, targetPan: Offset) {
        if (targetZoom != zoom) zoom = targetZoom
        if (targetPan != pan) pan = targetPan
    }

    fun zoomRelativeTo(zoomFactor: Float, pivot: Offset) {
        // 1. Calculate the World Point under the Pivot
        //    World = (Screen - Pan) / Zoom
        val worldPoint = (pivot - pan) / zoom
        
        // 2. Apply new Zoom
        //    Zoom = Current * Factor
        val newZoom = (zoom * zoomFactor).coerceIn(0.1f, 5f)
        
        // 3. Recalculate Pan to keep World Point under Pivot
        //    NewPan = Pivot - (WorldPoint * NewZoom)
        val proposedPan = pivot - (worldPoint * newZoom)

        // 4. Clamp
        val clampedPan = clampPan(proposedPan, newZoom)

        zoom = newZoom
        pan = clampedPan
    }

    /**
     * Converts a Screen Coordinate (pixel) to a World Coordinate (drawing unit).
     * Formula: World = (Screen - Pan) / Zoom
     */
    fun screenToWorld(screenOffset: Offset): Offset {
        return (screenOffset - pan) / zoom
    }

    companion object {
        val Saver: Saver<ViewportState, Any> = listSaver(
            save = { listOf(it.zoom, it.pan.x, it.pan.y) },
            restore = {
                ViewportState(
                    initialZoom = it[0] as Float,
                    initialPan = Offset(it[1] as Float, it[2] as Float)
                )
            }
        )
    }
}

@Composable
fun rememberViewportState(
    initialZoom: Float = 1f,
    initialPan: Offset = Offset.Zero
): ViewportState {
    return rememberSaveable(saver = ViewportState.Saver) {
        ViewportState(initialZoom, initialPan)
    }
}
