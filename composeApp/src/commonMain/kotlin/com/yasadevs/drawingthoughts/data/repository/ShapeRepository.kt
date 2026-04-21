package com.yasadevs.drawingthoughts.data.repository

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yasadevs.drawingthoughts.data.local.dao.ShapeDao
import com.yasadevs.drawingthoughts.data.local.entity.ShapeEntity
import com.yasadevs.drawingthoughts.domain.model.DrawingTool
import com.yasadevs.drawingthoughts.domain.model.DrawnShape
import com.yasadevs.drawingthoughts.utils.PathSmoother
import com.yasadevs.drawingthoughts.utils.toImageBitmap

class ShapeRepository(private val dao: ShapeDao) {

    suspend fun saveAllShapes(shapes: List<DrawnShape>) {
        dao.deleteAllShapes()
        dao.insertShapes(shapes.map(::mapToEntity))
    }

    @Deprecated("Use saveAllShapes instead for auto-save to avoid data loss")
    suspend fun saveShapes(shapes: List<DrawnShape>) {
        dao.deleteAllShapes()
        val entities = shapes.map { shape ->
            mapToEntity(shape)
        }
        dao.insertShapes(entities)
    }

    suspend fun saveShapesForFolder(shapes: List<DrawnShape>, folderId: String?) {
        if (folderId == null) {
            dao.deleteShapesWithoutFolder()
        } else {
            dao.deleteShapesByFolder(folderId)
        }
        val shapesForThisFolder = shapes.filter { it.folderId == folderId }
        dao.insertShapes(shapesForThisFolder.map(::mapToEntity))
    }

    suspend fun getShapes(): List<DrawnShape> {
        return dao.getAllShapes().map { entity ->
            mapToDomain(entity)
        }
    }

    suspend fun getShapesByFolder(folderId: String?): List<DrawnShape> {
        val entities = if (folderId == null) {
            dao.getShapesWithoutFolder()
        } else {
            dao.getShapesByFolder(folderId)
        }
        return entities.map(::mapToDomain)
    }

    suspend fun deleteShapesByFolder(folderId: String) {
        dao.deleteShapesByFolder(folderId)
    }

    suspend fun cleanupImageIfOrphaned(fileName: String) {
        val count = dao.countShapesWithFileName(fileName)
        if (count == 0) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.yasadevs.drawingthoughts.utils.LocalFileStorage.deleteImage(fileName)
            }
        }
    }

    private fun mapToEntity(shape: DrawnShape): ShapeEntity {
        val idLong = shape.id.toLongOrNull() ?: 0L

        return when (shape) {
            is DrawnShape.FreeHand -> {
                val pointsString = shape.points.joinToString(";") { "${it.x},${it.y}" }
                val colorInt = (shape.color.value shr 32).toInt()
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = colorInt,
                    strokeWidth = shape.strokeWidth,
                    folderId = shape.folderId,
                    points = pointsString
                )
            }
            is DrawnShape.Geometric -> {
                val colorInt = (shape.color.value shr 32).toInt()
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = colorInt,
                    strokeWidth = shape.strokeWidth,
                    folderId = shape.folderId,
                    startX = shape.start.x,
                    startY = shape.start.y,
                    endX = shape.end.x,
                    endY = shape.end.y
                )
            }
            is DrawnShape.Text -> {
                // CRITICAL FIX: Color.value is a 64-bit ULong with ARGB in upper 32 bits
                val colorInt = (shape.color.value shr 32).toInt()
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = colorInt,
                    strokeWidth = shape.strokeWidth,
                    folderId = shape.folderId,
                    text = shape.text,
                    textX = shape.position.x,
                    textY = shape.position.y,
                    fontSize = shape.fontSize,
                    fontFamily = serializeFontFamily(shape.fontFamily),
                    fontWeight = shape.fontWeight.weight,
                    fontStyle = if (shape.fontStyle == FontStyle.Italic) 1 else 0,
                    textBoxWidth = shape.boxWidth
                )
            }
            is DrawnShape.Image -> {
                val colorInt = (shape.color.value shr 32).toInt()
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = colorInt,
                    strokeWidth = shape.strokeWidth,
                    folderId = shape.folderId,
                    fileName = shape.fileName,
                    startX = shape.bounds.left,
                    startY = shape.bounds.top,
                    endX = shape.bounds.right,
                    endY = shape.bounds.bottom,
                    cropRectLeft = shape.cropRect?.left,
                    cropRectTop = shape.cropRect?.top,
                    cropRectRight = shape.cropRect?.right,
                    cropRectBottom = shape.cropRect?.bottom
                )
            }
        }
    }

    private fun mapToDomain(entity: ShapeEntity): DrawnShape {
        val colorULong = entity.color.toUInt().toULong() shl 32
        val color = Color(colorULong)
        val idString = entity.id.toString()

        val tool = try {
            DrawingTool.valueOf(entity.type)
        } catch (e: IllegalArgumentException) {
            DrawingTool.PEN
        }

        return when {
            entity.points != null -> {
                val points = entity.points.split(";").mapNotNull {
                    val parts = it.split(",")
                    if (parts.size == 2) Offset(parts[0].toFloat(), parts[1].toFloat()) else null
                }

                // Re-smooth on load
                val path = PathSmoother.createSmoothedPath(points)
                DrawnShape.FreeHand(
                    id = idString,
                    color = color,
                    strokeWidth = entity.strokeWidth,
                    drawingTool = tool,
                    folderId = entity.folderId,
                    path = path,
                    points = points
                )
            }
            entity.text != null -> {
                // Text shape
                DrawnShape.Text(
                    id = idString,
                    color = color,
                    strokeWidth = entity.strokeWidth,
                    drawingTool = tool,
                    folderId = entity.folderId,
                    position = Offset(entity.textX ?: 0f, entity.textY ?: 0f),
                    text = entity.text,
                    fontSize = entity.fontSize ?: 24f,
                    fontFamily = deserializeFontFamily(entity.fontFamily),
                    fontWeight = FontWeight(entity.fontWeight ?: 400),
                    fontStyle = if (entity.fontStyle == 1) FontStyle.Italic else FontStyle.Normal,
                    boxWidth = entity.textBoxWidth
                )
            }
            entity.fileName != null -> {
                // Image shape
                val bounds = androidx.compose.ui.geometry.Rect(
                    left = entity.startX ?: 0f,
                    top = entity.startY ?: 0f,
                    right = entity.endX ?: 0f,
                    bottom = entity.endY ?: 0f
                )
                val cropRect = if (entity.cropRectLeft != null && entity.cropRectTop != null && entity.cropRectRight != null && entity.cropRectBottom != null) {
                    androidx.compose.ui.geometry.Rect(
                        left = entity.cropRectLeft,
                        top = entity.cropRectTop,
                        right = entity.cropRectRight,
                        bottom = entity.cropRectBottom
                    )
                } else null
                
                val fileName = entity.fileName
                val bytes = com.yasadevs.drawingthoughts.utils.LocalFileStorage.loadImage(fileName)
                
                val bitmap = try {
                    bytes?.toImageBitmap()
                } catch (e: Exception) {
                    null
                }
                
                DrawnShape.Image(
                    id = idString,
                    color = color,
                    strokeWidth = entity.strokeWidth,
                    drawingTool = tool,
                    folderId = entity.folderId,
                    bitmap = bitmap,
                    fileName = fileName,
                    bounds = bounds,
                    cropRect = cropRect
                )
            }
            else -> {
                // Geometric shape
                val start = Offset(entity.startX ?: 0f, entity.startY ?: 0f)
                val end = Offset(entity.endX ?: 0f, entity.endY ?: 0f)
                DrawnShape.Geometric(
                    id = idString,
                    color = color,
                    strokeWidth = entity.strokeWidth,
                    drawingTool = tool,
                    folderId = entity.folderId,
                    start = start,
                    end = end
                )
            }
        }
    }

    private fun serializeFontFamily(fontFamily: FontFamily): String {
        return when (fontFamily) {
            FontFamily.Default -> "Default"
            FontFamily.SansSerif -> "SansSerif"
            FontFamily.Serif -> "Serif"
            FontFamily.Monospace -> "Monospace"
            FontFamily.Cursive -> "Cursive"
            else -> "Default"
        }
    }

    private fun deserializeFontFamily(fontFamilyString: String?): FontFamily {
        return when (fontFamilyString) {
            "SansSerif" -> FontFamily.SansSerif
            "Serif" -> FontFamily.Serif
            "Monospace" -> FontFamily.Monospace
            "Cursive" -> FontFamily.Cursive
            else -> FontFamily.Default
        }
    }
}
