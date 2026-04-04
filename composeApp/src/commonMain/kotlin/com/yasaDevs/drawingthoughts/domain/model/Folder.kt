package com.yasaDevs.drawingthoughts.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Folder domain model for organizing drawings
 *
 * Used to categorize and filter drawings in a Google Drive-style interface
 */
data class Folder(
    val id: String,
    val name: String,
    val color: Color,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        // Default folder that contains all uncategorized drawings
        val DEFAULT = Folder(
            id = "default",
            name = "All Drawings",
            color = Color.Gray
        )

        // Preset folder colors for quick selection
        val PRESET_COLORS = listOf(
            Color(0xFFEF5350), // Red
            Color(0xFF42A5F5), // Blue
            Color(0xFF66BB6A), // Green
            Color(0xFFFFCA28), // Yellow
            Color(0xFFAB47BC), // Purple
            Color(0xFFFF7043)  // Orange
        )
    }
}
