package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnPath
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class WhiteBoardViewModel: ViewModel() {

    private val _state = MutableStateFlow(WhiteBoardState())
    val state = _state.asStateFlow()

    fun onEvent(event: WhiteBoardEvent) {
        when(event) {
            is WhiteBoardEvent.StartDrawing -> {
                _state.update {
                    it.copy(
                        startingOffset = event.offset
                    )
                }
            }

            is WhiteBoardEvent.ContinueDrawing -> {
                updateContinuingOffset(event.offset)

            }
            WhiteBoardEvent.FinishDrawing -> {
                state.value.currentPath?.let { path ->
                    _state.update {
                        it.copy(
                            paths = it.paths + path,
                            currentPath = null,
                            startingOffset = null
                        )
                    }
                }
            }
            WhiteBoardEvent.OnCloseDrawingToolsCard -> {}
            is WhiteBoardEvent.OnDrawingToolSelected -> {
                _state.update {
                    it.copy(
                        selectedTool = event.tool,
                        isDrawingToolCardVisible = false
                    )
                }
            }
            WhiteBoardEvent.OnFABClick -> {
                _state.update {
                    it.copy(isDrawingToolCardVisible = true)
                }
            }
        }

    }

    private fun updateContinuingOffset(offset: Offset) {

        val startOffset = state.value.startingOffset
        val updatedPath: Path? = when(state.value.selectedTool){
            DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.LASER_PEN, DrawingTool.ERASER -> {

                createFreeHandPath(start = startOffset, end = offset)

            }

            DrawingTool.LINE_PLANE, DrawingTool.LINE_DOTTED -> {
                createLinePath(start = startOffset, end = offset)
            }

            DrawingTool.ARROW_ONE_SIDED, DrawingTool.ARROW_TWO_SIDED -> {
                null
            }

            DrawingTool.CIRCLE_OUTLINED, DrawingTool.CIRCLE_FILLED -> {
                createCirclePath(start = startOffset, end = offset)
            }

            DrawingTool.RECTANGLE_OUTLINED,  DrawingTool.RECTANGLE_FILLED -> {
                createRectanglePath(start = startOffset, end = offset)

            }

            DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                createTrianglePath(start = startOffset, end = offset)

            }


        }
        updatedPath?.let    {path ->
            _state.update {
                it.copy(
                    currentPath = DrawnPath(
                        path = path,
                        drawingTool = state.value.selectedTool,
                        color = Color.Black,
                        strokeWidth = 5f

                    )
                )
            }
        }
    }

    private fun createFreeHandPath(start: Offset?, end: Offset): Path {
        val existingPath = state.value.currentPath?.path?: Path().apply{
            start?.let { moveTo(it.x, it.y) }
        }
        return Path().apply {
            addPath(existingPath)
            lineTo(end.x, end.y)
        }
    }

    private fun createLinePath(start: Offset?, end: Offset): Path {
        return Path().apply {
            start?.let { moveTo(it.x, it.y) }
            lineTo(end.x, end.y)
        }
    }
    private fun createRectanglePath(start: Offset?, end: Offset): Path {
        return Path().apply {
            start?.let {
                // Determine the actual top-left and bottom-right corners
                val left = min(it.x, end.x)
                val top = min(it.y, end.y)
                val right = max(it.x, end.x)
                val bottom = max(it.y, end.y)

                addRect(Rect(left, top, right, bottom))
            }
        }
    }
    private fun createCirclePath(start: Offset?, end: Offset): Path {
        return Path().apply {
            start?.let {
                val dx = end.x - it.x
                val dy = end.y - it.y
                // Correctly calculate the radius using the distance formula
                val radius = sqrt(dx.pow(2) + dy.pow(2))

                // Create the circle with the start point as the center
                addOval(Rect(center = it, radius = radius))
            }
        }
    }
    private fun createTrianglePath(start: Offset?, end: Offset): Path {
        return Path().apply {
            start?.let {
                val topVertex = it

                val baseY = end.y

                val halfBaseWidth = abs(it.x - end.x)


                val bottomLeftVertex = Offset(x = it.x - halfBaseWidth, y = baseY)
                val bottomRightVertex = Offset(x = it.x + halfBaseWidth, y = baseY)

                moveTo(topVertex.x, topVertex.y)
                lineTo(bottomLeftVertex.x, bottomLeftVertex.y)
                lineTo(bottomRightVertex.x, bottomRightVertex.y)
                close()
            }
        }
    }
}