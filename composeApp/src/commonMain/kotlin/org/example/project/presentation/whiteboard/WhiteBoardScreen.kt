package org.example.project.presentation.whiteboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape
import org.example.project.presentation.whiteboard.component.DrawingToolCard
import org.example.project.presentation.whiteboard.component.DrawingToolFAB
import org.example.project.presentation.whiteboard.component.TopBar
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer

@Composable
fun WhiteBoardScreen(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit,
    imageSaver: org.example.project.utils.PlatformImageSaver
) {
    var showStrokeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }

    if (showStrokeDialog) {
        StrokeWidthDialog(
            currentWidth = state.currentStrokeWidth,
            onDismiss = { showStrokeDialog = false },
            onConfirm = { width ->
                onEvent(WhiteBoardEvent.OnStrokeWidthChange(width))
                showStrokeDialog = false
            }
        )
    }

    if (showColorDialog) {
        ColorPickerDialog(
            currentColor = state.currentColor,
            onDismiss = { showColorDialog = false },
            onConfirm = { color ->
                onEvent(WhiteBoardEvent.OnColorChange(color))
                showColorDialog = false
            }
        )
    }

    if (showBackgroundDialog) {
        ColorPickerDialog(
            currentColor = state.canvasBackgroundColor,
            onDismiss = { showBackgroundDialog = false },
            onConfirm = { color ->
                onEvent(WhiteBoardEvent.OnBackgroundChange(color))
                showBackgroundDialog = false
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val coroutineScope = rememberCoroutineScope()
        val graphicsLayer = rememberGraphicsLayer()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                }
        ) {
            DrawingCanvas(
                modifier = Modifier.fillMaxSize(),
                state = state,
                onEvent = onEvent
            )
        }

        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(20.dp),
            onStrokeWidthClick = { showStrokeDialog = true },
            onHomeIconClick = { },
            onUndoIconClick = { onEvent(WhiteBoardEvent.OnUndo) },
            onRedoIconClick = { onEvent(WhiteBoardEvent.OnRedo) },
            onDrawingColorClick = { showColorDialog = true },
            onBackgroundClick = { showBackgroundDialog = true },
            onSettingsClick = { },
            onSaveClick = {
                coroutineScope.launch {
                    val bitmap = graphicsLayer.toImageBitmap()
                    val result = imageSaver.saveImage(bitmap)
                    // TODO: Show Snackbar with result
                    println("Save Result: $result")
                }
            }
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
    val isSelector = state.selectedTool == DrawingTool.SELECTOR

    Canvas(
        modifier = modifier
            .background(state.canvasBackgroundColor)
            .fillMaxSize()
            .pointerInput(isSelector) {
                if (isSelector) {
                    // --- SELECTOR MODE GESTURES ---
                    detectTapGestures(
                        onTap = { offset -> onEvent(WhiteBoardEvent.StartDrawing(offset)) }
                    )
                } else {
                    // --- DRAWING MODE GESTURES ---
                    detectDragGestures(
                        onDragStart = { offset -> onEvent(WhiteBoardEvent.StartDrawing(offset)) },
                        onDrag = { change, _ ->
                            val offset = Offset(change.position.x, change.position.y)
                            onEvent(WhiteBoardEvent.ContinueDrawing(offset))
                        },
                        onDragEnd = { onEvent(WhiteBoardEvent.FinishDrawing) }
                    )
                }
            }
            .pointerInput(isSelector, state.selectedShapeId) {
                 if (isSelector && state.selectedShapeId != null) {
                      detectTransformGestures { _, pan, zoom, rotation ->
                          onEvent(WhiteBoardEvent.OnShapeTransform(zoom, pan, rotation))
                      }
                 }
            }
            // Listener for Transform End? detectTransformGestures doesn't have onEnd.
            // Workaround: We might need a custom detector or assume end when touch lifts (complex).
            // For MVP, we will rely on a separate pointerInput for "up" or just apply transient constantly?
            // "detectTransformGestures" blocks. 
            // Better: use 'forEachGesture' manually if we need 'onEnd'. 
            // OR: Just let the transient state stick until next tap? No, that's bad UX.
            // Simplified: We accept that changes are committed constantly or we add a "UP" listener.
            // Let's stick to commiting on every frame? No, expensive.
            // Let's add a separate listener for 'onRelease'.
            .pointerInput(isSelector, state.selectedShapeId) {
                if (isSelector && state.selectedShapeId != null) {
                    awaitPointerEventScope {
                         while (true) {
                             val event = awaitPointerEvent()
                             if (event.changes.all { !it.pressed }) {
                                 onEvent(WhiteBoardEvent.OnShapeTransformEnd)
                             }
                         }
                    }
                }
            }
    ) {
        val allShapes = state.shapes + listOfNotNull(state.currentShape)
        
        allShapes.forEach { shape ->
            val isSelected = shape.id == state.selectedShapeId
            
            // --- PHYSICS & ATTRIBUTES CALCULATION ---
            var actualColor = shape.color
            var actualAlpha = 1.0f
            var actualBlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver
            var actualStrokeWidth = shape.strokeWidth
            var actualPathEffect: androidx.compose.ui.graphics.PathEffect? = null
            var actualCap = androidx.compose.ui.graphics.StrokeCap.Round

            when (shape.drawingTool) {
                DrawingTool.HIGHLIGHTER -> {
                    actualAlpha = 0.4f
                    actualStrokeWidth = shape.strokeWidth * 3 // Fat marker
                    actualCap = androidx.compose.ui.graphics.StrokeCap.Square
                }
                DrawingTool.ERASER -> {
                    actualColor = state.canvasBackgroundColor // "Fake" erase
                    actualStrokeWidth = shape.strokeWidth * 1.5f
                }
                DrawingTool.LINE_DOTTED -> {
                    actualPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                }
                else -> { /* Default Pen/Shapes */ }
            }

            // Define the style based on tool type (Filled vs Stroke)
            val style = if (shape.drawingTool.isFilled()) {
                Fill
            } else {
                Stroke(
                    width = actualStrokeWidth,
                    cap = actualCap,
                    pathEffect = actualPathEffect
                )
            }

            // Transform Context for Selected Shape
            val drawParams = if (isSelected) {
                // Calculate Center for Pivot
                 var centerX = 0f
                 var centerY = 0f
                 if(shape is DrawnShape.Geometric) {
                     centerX = (shape.start.x + shape.end.x) / 2
                     centerY = (shape.start.y + shape.end.y) / 2
                 } else if (shape is DrawnShape.FreeHand) {
                     // Approximate center (Optimization: Cache this?)
                      var minX = Float.POSITIVE_INFINITY; var maxX = Float.NEGATIVE_INFINITY
                      var minY = Float.POSITIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
                      shape.points.forEach { minX = min(minX, it.x); maxX = max(maxX, it.x); minY = min(minY, it.y); maxY = max(maxY, it.y) }
                      centerX = (minX + maxX)/2
                      centerY = (minY + maxY)/2
                 }
                 
                 Triple(state.transientScale, state.transientOffset, Offset(centerX, centerY))
            } else {
                Triple(1f, Offset.Zero, Offset.Zero)
            }

            val (scale, offset, pivot) = drawParams

            withTransform({
                if (isSelected) {
                     translate(offset.x, offset.y)
                     scale(scale, scale, pivot)
                }
            }) {
                // --- RENDERING ---
                when (shape) {
                    is DrawnShape.FreeHand -> {
                        drawPath(
                            path = shape.path,
                            color = actualColor,
                            alpha = actualAlpha,
                            style = style,
                            blendMode = actualBlendMode
                        )
                    }
                    is DrawnShape.Geometric -> {
                        val topLeft = Offset(
                            min(shape.start.x, shape.end.x),
                            min(shape.start.y, shape.end.y)
                        )
                        val size = Size(
                            abs(shape.start.x - shape.end.x),
                            abs(shape.start.y - shape.end.y)
                        )

                        when (shape.drawingTool) {
                            DrawingTool.CIRCLE_OUTLINED, DrawingTool.CIRCLE_FILLED -> {
                                drawOval(color = actualColor, topLeft = topLeft, size = size, style = style, alpha = actualAlpha)
                            }
                            DrawingTool.RECTANGLE_OUTLINED, DrawingTool.RECTANGLE_FILLED -> {
                                drawRect(color = actualColor, topLeft = topLeft, size = size, style = style, alpha = actualAlpha)
                            }
                            DrawingTool.LINE_PLANE, DrawingTool.LINE_DOTTED, DrawingTool.PEN -> {
                                drawLine(color = actualColor, start = shape.start, end = shape.end, strokeWidth = actualStrokeWidth, cap = actualCap, pathEffect = actualPathEffect, alpha = actualAlpha)
                            }
                            DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                                val path = org.example.project.utils.GeometryHelper.calculateTrianglePath(shape.start, shape.end)
                                drawPath(path = path, color = actualColor, alpha = actualAlpha, style = style)
                            }
                            DrawingTool.ARROW_ONE_SIDED -> {
                                val path = org.example.project.utils.GeometryHelper.calculateArrowPath(shape.start, shape.end, false)
                                drawPath(path = path, color = actualColor, style = Stroke(width = actualStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round), alpha = actualAlpha)
                            }
                            DrawingTool.ARROW_TWO_SIDED -> {
                                val path = org.example.project.utils.GeometryHelper.calculateArrowPath(shape.start, shape.end, true)
                                drawPath(path = path, color = actualColor, style = Stroke(width = actualStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round), alpha = actualAlpha)
                            }
                            else -> {}
                        }
                        
                         // --- FIGMA SELECTION OVERLAY (INSIDE TRANSFORM) ---
                         if (isSelected) {
                            // Border
                            drawRect(
                                color = Color(0xFF18A0FB), // Figma Blue
                                topLeft = topLeft,
                                size = size,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            
                            // Handles (Corners)
                            val handleRadius = 5.dp.toPx()
                            val handleColor = Color.White
                            val handleStroke = Color(0xFF18A0FB)
                            
                            val corners = listOf(
                                topLeft,
                                Offset(topLeft.x + size.width, topLeft.y),
                                Offset(topLeft.x, topLeft.y + size.height),
                                Offset(topLeft.x + size.width, topLeft.y + size.height)
                            )
                            
                            corners.forEach { center ->
                                drawCircle(handleColor, radius = handleRadius, center = center)
                                drawCircle(handleStroke, radius = handleRadius, center = center, style = Stroke(width = 1.dp.toPx()))
                            }
                         }
                    }
                }
            } // End withTransform
        }
    }
}

private fun DrawingTool.isFilled(): Boolean {
    return this == DrawingTool.CIRCLE_FILLED || 
           this == DrawingTool.RECTANGLE_FILLED || 
           this == DrawingTool.TRIANGLE_FILLED
}

@Composable
fun StrokeWidthDialog(currentWidth: Float, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var sliderValue by remember { mutableStateOf(currentWidth) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stroke Width") },
        text = {
            Column {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..50f
                )
                Text("${sliderValue.toInt()} px")
            }
        },
        confirmButton = { Button(onClick = { onConfirm(sliderValue) }) { Text("OK") } }
    )
}

@Composable
fun ColorPickerDialog(currentColor: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color.Yellow, Color.White, Color.Gray)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Color") },
        text = {
            Row {
                colors.forEach { color ->
                    Box(modifier = Modifier
                        .size(40.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onConfirm(color) }
                    )
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}
