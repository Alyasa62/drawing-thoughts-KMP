package com.yasaDevs.drawingthoughts.data.repository

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yasaDevs.drawingthoughts.data.local.dao.ShapeDao
import com.yasaDevs.drawingthoughts.data.local.entity.ShapeEntity
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import com.yasaDevs.drawingthoughts.utils.PathSmoother
import com.yasaDevs.drawingthoughts.utils.toImageBitmap

class ShapeRepository(private val dao: ShapeDao) {

    suspend fun saveAllShapes(shapes: List<DrawnShape>) {
        try {
            println("ShapeRepository: Saving ALL ${shapes.size} shapes (replacing entire database)")
            shapes.forEachIndexed { index, shape ->
                println("  Shape $index: ${shape::class.simpleName}, color=${shape.color}, folderId=${shape.folderId}")
            }

            // Delete ALL shapes and replace with current state
            println("ShapeRepository: Deleting all existing shapes")
            dao.deleteAllShapes()
            println("ShapeRepository: Delete completed")

            // Save all shapes
            val entities = shapes.map { shape ->
                mapToEntity(shape)
            }
            println("ShapeRepository: Inserting ${entities.size} entities into database")
            dao.insertShapes(entities)
            println("ShapeRepository: ✓ Successfully saved ALL ${entities.size} entities to database")
        } catch (e: Exception) {
            println("ShapeRepository: ✗ ERROR during save all: ${e.message}")
            e.printStackTrace()
            throw e
        }
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
        try {
            println("ShapeRepository: Saving ${shapes.size} shapes for folder: $folderId")
            shapes.forEachIndexed { index, shape ->
                println("  Shape $index: ${shape::class.simpleName}, color=${shape.color}, folderId=${shape.folderId}")
            }

            // Delete only shapes from the current folder
            println("ShapeRepository: Deleting existing shapes for folder: $folderId")
            if (folderId == null) {
                dao.deleteShapesWithoutFolder()
            } else {
                dao.deleteShapesByFolder(folderId)
            }
            println("ShapeRepository: Delete completed")

            // Filter shapes to only save those belonging to this folder
            val shapesForThisFolder = shapes.filter { it.folderId == folderId }

            println("ShapeRepository: Filtered to ${shapesForThisFolder.size} shapes matching folderId: $folderId")

            // Insert the new shapes for this folder
            val entities = shapesForThisFolder.map { shape ->
                mapToEntity(shape)
            }
            println("ShapeRepository: Inserting ${entities.size} entities into database")
            dao.insertShapes(entities)
            println("ShapeRepository: ✓ Successfully saved ${entities.size} entities to database")
        } catch (e: Exception) {
            println("ShapeRepository: ✗ ERROR during save: ${e.message}")
            e.printStackTrace()
            throw e
        }
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
        println("ShapeRepository: Loading ${entities.size} entities for folder: $folderId")
        val shapes = entities.map { entity -> mapToDomain(entity) }
        shapes.forEachIndexed { index, shape ->
            println("  Loaded Shape $index: ${shape::class.simpleName}, color=${shape.color}, folderId=${shape.folderId}")
        }
        return shapes
    }

    suspend fun deleteShapesByFolder(folderId: String) {
        dao.deleteShapesByFolder(folderId)
    }

    private fun mapToEntity(shape: DrawnShape): ShapeEntity {
        val idLong = shape.id.toLongOrNull() ?: 0L

        return when (shape) {
            is DrawnShape.FreeHand -> {
                val pointsString = shape.points.joinToString(";") { "${it.x},${it.y}" }
                // CRITICAL FIX: Color.value is a 64-bit ULong with ARGB in upper 32 bits
                // We need to shift right by 32 bits to get the actual color value
                val colorInt = (shape.color.value shr 32).toInt()
                println("  mapToEntity FreeHand: color=${shape.color} (ULong=${shape.color.value}) -> colorInt=$colorInt (0x${colorInt.toUInt().toString(16)})")
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
                // CRITICAL FIX: Color.value is a 64-bit ULong with ARGB in upper 32 bits
                val colorInt = (shape.color.value shr 32).toInt()
                println("  mapToEntity Geometric: color=${shape.color} (ULong=${shape.color.value}) -> colorInt=$colorInt (0x${colorInt.toUInt().toString(16)})")
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
                    fontStyle = if (shape.fontStyle == FontStyle.Italic) 1 else 0
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
                    imageBytes = shape.bytes,
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
        // CRITICAL FIX: entity.color is a signed Int from database containing ARGB
        // Color constructor expects ULong with ARGB in upper 32 bits
        // So we need to: Int -> UInt (reinterpret bits) -> shift left 32 -> ULong
        val colorULong = entity.color.toUInt().toULong() shl 32
        val color = Color(colorULong)
        println("  mapToDomain: entity.color=${entity.color} (0x${entity.color.toUInt().toString(16)}) -> colorULong=0x${colorULong.toString(16)} -> Color=$color")
        val idString = entity.id.toString()

        // Parse tool type - will throw exception for legacy "FREEHAND" entries
        // This forces users to clear old database after update
        val tool = try {
            DrawingTool.valueOf(entity.type)
        } catch (e: IllegalArgumentException) {
            // Legacy "FREEHAND" type detected - database needs to be cleared
            // Return a placeholder PEN stroke to avoid crash
            println("Warning: Legacy database format detected. Please clear the database.")
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
                    fontStyle = if (entity.fontStyle == 1) FontStyle.Italic else FontStyle.Normal
                )
            }
            entity.imageBytes != null -> {
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
                
                val bitmap = try {
                    entity.imageBytes.toImageBitmap()
                } catch (e: Exception) {
                    println("Failed to parse image bitmap from bytes.")
                    null
                }
                
                DrawnShape.Image(
                    id = idString,
                    color = color,
                    strokeWidth = entity.strokeWidth,
                    drawingTool = tool,
                    folderId = entity.folderId,
                    bitmap = bitmap,
                    bytes = entity.imageBytes,
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
