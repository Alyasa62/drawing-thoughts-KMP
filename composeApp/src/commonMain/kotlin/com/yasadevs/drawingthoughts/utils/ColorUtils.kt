package com.yasadevs.drawingthoughts.utils

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * HSV Color representation and conversion utilities for the custom color picker.
 */
data class HSVColor(
    val hue: Float,        // 0-360
    val saturation: Float, // 0-1
    val value: Float,      // 0-1
    val alpha: Float = 1f  // 0-1
) {
    /**
     * Converts HSV to RGB Color
     */
    fun toColor(): Color {
        val h = hue / 60f
        val c = value * saturation
        val x = c * (1 - abs((h % 2) - 1))
        val m = value - c

        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            5 -> Triple(c, 0f, x)
            else -> Triple(c, x, 0f)
        }

        return Color(
            red = (r + m),
            green = (g + m),
            blue = (b + m),
            alpha = alpha
        )
    }

    companion object {
        /**
         * Converts RGB Color to HSV
         */
        fun fromColor(color: Color): HSVColor {
            val r = color.red
            val g = color.green
            val b = color.blue

            val cMax = max(r, max(g, b))
            val cMin = min(r, min(g, b))
            val delta = cMax - cMin

            val hue = when {
                delta == 0f -> 0f
                cMax == r -> 60f * (((g - b) / delta) % 6)
                cMax == g -> 60f * (((b - r) / delta) + 2)
                else -> 60f * (((r - g) / delta) + 4)
            }.let { if (it < 0) it + 360f else it }

            val saturation = if (cMax == 0f) 0f else delta / cMax
            val value = cMax

            return HSVColor(hue, saturation, value, color.alpha)
        }
    }
}

/**
 * Predefined color palettes for the Style Studio
 */
object ColorPalettes {
    val backgroundColors = listOf(
        Color(0xFFFFF8DC), // Cream
        Color(0xFFADD8E6), // Light Blue
        Color(0xFFE6B3E6), // Light Pink
        Color(0xFFFFFACD), // Lemon
        Color(0xFFFFFFFF), // White
        Color(0xFFF5F5F5), // Off-white
        Color(0xFFE8E8E8)  // Light gray
    )

    val strokeColors = listOf(
        Color.Black,
        Color(0xFFE74C3C), // Red
        Color(0xFF3498DB), // Blue
        Color(0xFF2ECC71), // Green
        Color(0xFFF39C12), // Orange
        Color(0xFF9B59B6), // Purple
        Color(0xFF1ABC9C), // Teal
        Color(0xFFE91E63)  // Pink
    )

    val fillColors = listOf(
        Color(0xFFE74C3C), // Red
        Color(0xFF3498DB), // Blue
        Color(0xFF2ECC71), // Green
        Color(0xFFF39C12), // Orange
        Color(0xFF9B59B6), // Purple
        Color(0xFF1ABC9C), // Teal
        Color(0xFFE91E63), // Pink
        Color(0xFFFFEB3B)  // Yellow
    )
}
