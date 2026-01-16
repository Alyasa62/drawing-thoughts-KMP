package org.example.project.presentation.whiteboard

import androidx.compose.ui.graphics.Color
import org.example.project.domain.model.DrawingTool

/**
 * Per-tool settings that each tool remembers independently.
 * Each tool has its own color and stroke width preferences.
 */
data class ToolSettings(
    val color: Color,
    val strokeWidth: Float
)

/**
 * Default settings for each tool category.
 * These are the initial settings when a tool is first selected.
 */
object ToolSettingsDefaults {

    fun getDefaultSettings(tool: DrawingTool): ToolSettings {
        return when (tool) {
            DrawingTool.PEN -> ToolSettings(
                color = Color.Black,
                strokeWidth = 5f
            )

            DrawingTool.HIGHLIGHTER -> ToolSettings(
                color = Color(0xFFFFEB3B), // Yellow
                strokeWidth = 20f
            )

            DrawingTool.ERASER -> ToolSettings(
                color = Color.Black, // Placeholder, not used
                strokeWidth = 15f
            )

            DrawingTool.LASER_PEN -> ToolSettings(
                color = Color.Red,
                strokeWidth = 3f
            )

            DrawingTool.TEXT -> ToolSettings(
                color = Color.Black,
                strokeWidth = 24f // Used as font size
            )

            // Geometric shapes - filled variants
            DrawingTool.CIRCLE_FILLED,
            DrawingTool.RECTANGLE_FILLED,
            DrawingTool.SQUARE_FILLED,
            DrawingTool.TRIANGLE_FILLED,
            DrawingTool.STAR_FILLED,
            DrawingTool.ELLIPSE_FILLED -> ToolSettings(
                color = Color(0xFF2196F3), // Blue
                strokeWidth = 0f // Stroke width not used for fills
            )

            // Geometric shapes - outlined variants
            DrawingTool.CIRCLE_OUTLINED,
            DrawingTool.RECTANGLE_OUTLINED,
            DrawingTool.RECTANGLE_ROUNDED,
            DrawingTool.SQUARE_OUTLINED,
            DrawingTool.TRIANGLE_OUTLINED,
            DrawingTool.STAR_OUTLINED,
            DrawingTool.ELLIPSE_OUTLINED -> ToolSettings(
                color = Color.Black,
                strokeWidth = 4f
            )

            // Lines and arrows
            DrawingTool.LINE_PLANE,
            DrawingTool.LINE_DOTTED -> ToolSettings(
                color = Color.Black,
                strokeWidth = 3f
            )

            DrawingTool.ARROW_ONE_SIDED,
            DrawingTool.ARROW_TWO_SIDED -> ToolSettings(
                color = Color.Black,
                strokeWidth = 4f
            )

            // Polygons
            DrawingTool.PENTAGON,
            DrawingTool.HEXAGON,
            DrawingTool.DIAMOND -> ToolSettings(
                color = Color.Black,
                strokeWidth = 4f
            )

            // Non-drawing tools
            DrawingTool.SELECTOR,
            DrawingTool.HAND -> ToolSettings(
                color = Color.Black,
                strokeWidth = 5f
            )
        }
    }

    /**
     * Initialize settings map for all tools
     */
    fun createDefaultSettingsMap(): Map<DrawingTool, ToolSettings> {
        return DrawingTool.entries.associateWith { tool ->
            getDefaultSettings(tool)
        }
    }
}
