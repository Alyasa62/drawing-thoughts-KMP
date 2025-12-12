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
    initialPan: Offset = Offset.Zero
) {
    var zoom by mutableStateOf(initialZoom)
        private set
    
    var pan by mutableStateOf(initialPan)
        private set

    fun transform(zoomChange: Float, panChange: Offset) {
        // Apply changes
        // Note: Real "Google Maps" style zooming usually pivots around the centroid.
        // For now, adhering to the "Simple 1:1" request.
        val newZoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
        val newPan = pan + panChange
        
        zoom = newZoom
        pan = newPan
    }
    
    fun snapTo(targetZoom: Float, targetPan: Offset) {
        if (targetZoom != zoom) zoom = targetZoom
        if (targetPan != pan) pan = targetPan
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
