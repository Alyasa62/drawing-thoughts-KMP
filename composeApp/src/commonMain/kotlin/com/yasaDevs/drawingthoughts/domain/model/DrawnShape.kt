package com.yasaDevs.drawingthoughts.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap

sealed class DrawnShape {
    abstract val id: String
    abstract val color: Color
    abstract val strokeWidth: Float
    abstract val drawingTool: DrawingTool
    abstract val folderId: String? // null means "All Drawings"

    data class FreeHand(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float,
        override val drawingTool: DrawingTool,
        override val folderId: String? = null,
        val path: Path,
        val points: List<Offset> // Added for serialization
    ) : DrawnShape()

    data class Geometric(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float,
        override val drawingTool: DrawingTool,
        override val folderId: String? = null,
        val start: Offset,
        val end: Offset
    ) : DrawnShape()

    data class Text(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float, // Not used for text, but required by sealed class
        override val drawingTool: DrawingTool,
        override val folderId: String? = null,
        val position: Offset,
        val text: String,
        val fontSize: Float = 24f,
        val fontFamily: FontFamily = FontFamily.Default,
        val fontWeight: FontWeight = FontWeight.Normal,
        val fontStyle: FontStyle = FontStyle.Normal
    ) : DrawnShape()

    data class Image(
        override val id: String,
        override val color: Color,
        override val strokeWidth: Float,
        override val drawingTool: DrawingTool,
        override val folderId: String? = null,
        val bitmap: ImageBitmap? = null,
        val fileName: String,
        val bounds: Rect,
        val cropRect: Rect? = null
    ) : DrawnShape() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as Image
            if (id != other.id) return false
            if (folderId != other.folderId) return false
            if (bounds != other.bounds) return false
            if (cropRect != other.cropRect) return false
            if (fileName != other.fileName) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + (folderId?.hashCode() ?: 0)
            result = 31 * result + bounds.hashCode()
            result = 31 * result + (cropRect?.hashCode() ?: 0)
            result = 31 * result + fileName.hashCode()
            return result
        }
    }
}
