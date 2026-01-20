package org.example.project.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing folders in the database
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorValue: Long, // Store Color as Long (Color.value)
    val createdAt: Long
)
