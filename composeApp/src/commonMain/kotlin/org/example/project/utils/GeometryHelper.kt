package org.example.project.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object GeometryHelper {

    fun calculateTrianglePath(start: Offset, end: Offset): Path {
        val path = Path()
        // Top Center
        val topX = (start.x + end.x) / 2
        val topY = start.y
        
        // Bottom Left
        val bottomLeftX = start.x
        val bottomLeftY = end.y
        
        // Bottom Right
        val bottomRightX = end.x
        val bottomRightY = end.y

        path.moveTo(topX, topY)
        path.lineTo(bottomRightX, bottomRightY)
        path.lineTo(bottomLeftX, bottomLeftY)
        path.close()
        
        return path
    }

    fun calculateArrowPath(start: Offset, end: Offset, isDoubleSided: Boolean): Path {
        val path = Path()
        
        // Main Line
        path.moveTo(start.x, start.y)
        path.lineTo(end.x, end.y)

        // Arrow Head at End
        addArrowHead(path, start, end)

        // Arrow Head at Start (if double sided)
        if (isDoubleSided) {
            addArrowHead(path, end, start)
        }

        return path
    }

    private fun addArrowHead(path: Path, from: Offset, to: Offset) {
        val angle = atan2(to.y - from.y, to.x - from.x)
        val arrowLength = 50f 
        val arrowAngle = Math.PI / 6 // 30 degrees

        // Right side of the arrowhead
        val x1 = to.x - arrowLength * cos(angle - arrowAngle)
        val y1 = to.y - arrowLength * sin(angle - arrowAngle)
        
        // Left side of the arrowhead
        val x2 = to.x - arrowLength * cos(angle + arrowAngle)
        val y2 = to.y - arrowLength * sin(angle + arrowAngle)

        path.moveTo(to.x, to.y)
        path.lineTo(x1.toFloat(), y1.toFloat())
        path.moveTo(to.x, to.y)
        path.lineTo(x2.toFloat(), y2.toFloat())
    }
}
