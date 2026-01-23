package org.example.project.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

object PathSmoother {
    fun createSmoothedPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        // CRITICAL: To ensure round caps at BOTH ends, we need to ensure the path
        // has actual drawing commands (not just moveTo) for both the start and end.

        if (points.size == 1) {
            // Single point - draw a tiny line to ensure round cap is visible
            val point = points.first()
            path.moveTo(point.x, point.y)
            path.lineTo(point.x + 0.01f, point.y + 0.01f)
            return path
        }

        if (points.size == 2) {
            // Two points - direct line ensures round caps at both ends
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        // For multiple points, use quadratic bezier smoothing
        path.moveTo(points.first().x, points.first().y)

        // Start with a line to the first control point to ensure the starting round cap is visible
        if (points.size > 1) {
            val firstControl = Offset(
                (points[0].x + points[1].x) / 2,
                (points[0].y + points[1].y) / 2
            )
            path.lineTo(firstControl.x, firstControl.y)
        }

        // Smooth the middle segments with quadratic bezier
        for (i in 1 until points.size - 1) {
            val control = points[i]
            val end = Offset(
                (points[i].x + points[i + 1].x) / 2,
                (points[i].y + points[i + 1].y) / 2
            )
            path.quadraticBezierTo(control.x, control.y, end.x, end.y)
        }

        // End with a line to the last point to ensure the ending round cap is visible
        val last = points.last()
        path.lineTo(last.x, last.y)

        return path
    }
}
