package org.example.project.data.repository

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.example.project.data.local.dao.ShapeDao
import org.example.project.data.local.entity.ShapeEntity
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape
import org.example.project.utils.PathSmoother

class ShapeRepository(private val dao: ShapeDao) {

    suspend fun saveShapes(shapes: List<DrawnShape>) {
        dao.deleteAllShapes()
        val entities = shapes.map { shape -> 
            mapToEntity(shape) 
        }
        dao.insertShapes(entities)
    }

    suspend fun getShapes(): List<DrawnShape> {
        return dao.getAllShapes().map { entity ->
            mapToDomain(entity)
        }
    }

    private fun mapToEntity(shape: DrawnShape): ShapeEntity {
        val idLong = shape.id.toLongOrNull() ?: 0L 
        
        return when (shape) {
            is DrawnShape.FreeHand -> {
                val pointsString = shape.points.joinToString(";") { "${it.x},${it.y}" }
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name, // Store actual tool (PEN, ERASER, etc.)
                    color = shape.color.value.toInt(), // Store as Int
                    strokeWidth = shape.strokeWidth,
                    points = pointsString
                )
            }
            is DrawnShape.Geometric -> {
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = shape.color.value.toInt(),
                    strokeWidth = shape.strokeWidth,
                    startX = shape.start.x,
                    startY = shape.start.y,
                    endX = shape.end.x,
                    endY = shape.end.y
                )
            }
            is DrawnShape.Text -> {
                ShapeEntity(
                    id = idLong,
                    type = shape.drawingTool.name,
                    color = shape.color.value.toInt(),
                    strokeWidth = shape.strokeWidth,
                    text = shape.text,
                    textX = shape.position.x,
                    textY = shape.position.y,
                    fontSize = shape.fontSize,
                    fontFamily = serializeFontFamily(shape.fontFamily),
                    fontWeight = shape.fontWeight.weight,
                    fontStyle = if (shape.fontStyle == FontStyle.Italic) 1 else 0
                )
            }
        }
    }

    private fun mapToDomain(entity: ShapeEntity): DrawnShape {
        val color = Color(entity.color)
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

        // Determine shape type based on available fields
        return when {
            entity.points != null -> {
                // FreeHand shape
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
                    drawingTool = tool, // Use actual tool from database (PEN, ERASER, HIGHLIGHTER, etc.)
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
                    position = Offset(entity.textX ?: 0f, entity.textY ?: 0f),
                    text = entity.text,
                    fontSize = entity.fontSize ?: 24f,
                    fontFamily = deserializeFontFamily(entity.fontFamily),
                    fontWeight = FontWeight(entity.fontWeight ?: 400),
                    fontStyle = if (entity.fontStyle == 1) FontStyle.Italic else FontStyle.Normal
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
