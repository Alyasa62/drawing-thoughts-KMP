package org.example.project.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting canvas settings including background pattern.
 * Each folder has its own independent grid pattern setting.
 * folderId = null represents "All Drawings" (no folder)
 */
@Entity(tableName = "canvas_settings")
data class CanvasSettingsEntity(
    @PrimaryKey val folderId: String, // Folder ID (use "ALL_DRAWINGS" for null folder)
    val selectedPattern: String, // Stores CanvasPattern enum name
    val updatedAt: Long = System.currentTimeMillis()
)
