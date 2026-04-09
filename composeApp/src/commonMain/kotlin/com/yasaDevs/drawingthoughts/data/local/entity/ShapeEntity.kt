package com.yasaDevs.drawingthoughts.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ShapeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val color: Int,
    val strokeWidth: Float,
    val folderId: String? = null, // References Folder.id, null means "All Drawings"
    // Geometric
    val startX: Float? = null,
    val startY: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    // FreeHand
    val points: String? = null, // Format: "x,y;x,y"
    // Text
    val text: String? = null,
    val textX: Float? = null,
    val textY: Float? = null,
    val fontSize: Float? = null,
    val fontFamily: String? = null, // Serialized font family name
    val fontWeight: Int? = null, // FontWeight.weight value
    val fontStyle: Int? = null, // 0 = Normal, 1 = Italic
    // Image
    val fileName: String? = null,
    val cropRectLeft: Float? = null,
    val cropRectTop: Float? = null,
    val cropRectRight: Float? = null,
    val cropRectBottom: Float? = null
)
