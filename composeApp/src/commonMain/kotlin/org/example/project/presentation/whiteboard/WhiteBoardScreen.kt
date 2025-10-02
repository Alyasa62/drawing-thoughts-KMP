package org.example.project.presentation.whiteboard



import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawingTool
import org.example.project.presentation.whiteboard.component.DrawingToolCard
import org.example.project.presentation.whiteboard.component.DrawingToolFAB
import org.example.project.presentation.whiteboard.component.TopBar

@Composable
fun WhiteBoardScreen(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit
) {

    Box(
        modifier = modifier.fillMaxSize()
    ) {

        DrawingCanvas(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onEvent = onEvent
        )


        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(20.dp),
            onStrokeWidthClick = { },
            onHomeIconClick = { },
            onUndoIconClick = { },
            onRedoIconClick = { },
            onDrawingColorClick = { },
            onBackgroundClick = { },
            onSettingsClick = { }
        )
        DrawingToolFAB(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            isVisible = !state.isDrawingToolCardVisible,
            selectedTool = state.selectedTool,
            onClick = { onEvent(WhiteBoardEvent.OnFABClick) }
        )
        DrawingToolCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            selectedTool = state.selectedTool,
            onToolSelected = { tool -> onEvent(WhiteBoardEvent.OnDrawingToolSelected(tool)) },
            onClosedIconClick = { onEvent(WhiteBoardEvent.OnCloseDrawingToolsCard) },
            isVisible = state.isDrawingToolCardVisible
        )
    }
}

@Composable
private fun DrawingCanvas(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit
) {

    Canvas(
        modifier = modifier
            .background(Color.White)
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onEvent(WhiteBoardEvent.StartDrawing(offset))

                    },
                    onDrag = { change, _ ->
                        val offset = Offset(change.position.x, change.position.y)
                        onEvent(WhiteBoardEvent.ContinueDrawing(offset))

                    },
                    onDragEnd = {
                        onEvent(WhiteBoardEvent.FinishDrawing)

                    }
                )
            }
    ) {
        state.paths.forEach { drawnPath ->
            val pathEffect = if(
    drawnPath.drawingTool == DrawingTool.LINE_DOTTED
            ) {

                PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            } else {
                null
            }

            val drawnStyle = when(drawnPath.drawingTool) {
                DrawingTool.CIRCLE_FILLED, DrawingTool.RECTANGLE_FILLED, DrawingTool.TRIANGLE_FILLED ->{
                    Fill
                }else -> {
                    Stroke(width = 10f, pathEffect = pathEffect)
                }
            }

            drawPath(
                path = drawnPath.path,
                color = Color.Black,
                style = drawnStyle
            )
        }


        state.currentPath?.let { drawnPath ->
            val pathEffect = if(
                drawnPath.drawingTool == DrawingTool.LINE_DOTTED
            ) {

                PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            } else {
                null
            }

            val drawnStyle = when(drawnPath.drawingTool) {
                DrawingTool.CIRCLE_FILLED, DrawingTool.RECTANGLE_FILLED, DrawingTool.TRIANGLE_FILLED ->{
                    Fill
                }else -> {
                    Stroke(width = 10f, pathEffect = pathEffect)
                }
            }

            drawPath(
                path = drawnPath.path,
                color = Color.Black,
                style = drawnStyle
            )
        }

        }
    }
