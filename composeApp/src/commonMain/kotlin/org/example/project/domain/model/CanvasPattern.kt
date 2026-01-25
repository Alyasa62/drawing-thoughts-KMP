package org.example.project.domain.model

/**
 * Represents the background pattern options for the canvas.
 * Based on the Grid Settings feature specification.
 */
enum class CanvasPattern(val displayName: String) {
    /** Small dots pattern spaced 40dp apart (default) */
    DOTS("Dots"),

    /** Square grid pattern with thin lines spaced 40dp apart */
    GRID("Grid"),

    /** Horizontal lines pattern like ruled paper */
    LINES("Lines"),

    /** No pattern - blank background */
    NONE("None");

    companion object {
        /** Default pattern shown on app startup */
        val DEFAULT = DOTS

        /**
         * Converts a string representation to CanvasPattern enum.
         * Returns DEFAULT if the string doesn't match any pattern.
         */
        fun fromString(value: String): CanvasPattern {
            return entries.find { it.name == value } ?: DEFAULT
        }
    }
}
