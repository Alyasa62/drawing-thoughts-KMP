package org.example.project.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

object PathSmoother {
    fun createSmoothedPath(points: List<Offset>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points.first().x, points.first().y)

        for (i in 1 until points.size) {
            val start = points[i - 1]
            val end = points[i]
            val control = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
            path.quadraticBezierTo(start.x, start.y, control.x, control.y)
        }
        
        // Connect the last point straight if needed or handle close
        if (points.size > 1) {
             val last = points.last()
             path.lineTo(last.x, last.y)
        }

        return path
    }
}
