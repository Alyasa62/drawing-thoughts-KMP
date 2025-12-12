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
import androidx.compose.ui.graphics.graphicsLayer

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

    val viewportZoom = state.zoom
    val viewportPan = state.pan

    // Viewport Gestures
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
// ...
// ...

                    detectTransformGestures { centroid, pan, zoom, _ ->
                        // Calculate new zoom
                        val newZoom = (viewportZoom * zoom).coerceIn(0.1f, 5f)

                        // specific logic to keep centroid stable is complex (Pan adjustment)
                        // Simplified: Zoom towards center + Pan

                        // Standard math for centroid zooming:
                        // offset = (offset - centroid) * zoomRatio + centroid ??
                        // Let's stick to simple "Pan moves, Zoom scales" for the MVP to avoid jumping.
                        // Refinement: Accumulate Pan

                        // Determine effective pan addition considering zoom?
                        // No, detectTransformGestures pan is in screen pixels.
                        val newPan = viewportPan + pan

                        onEvent(WhiteBoardEvent.OnViewportChange(newZoom, newPan))
                    }
                }
        ) {
            // 1. Grid (Background)
            org.example.project.presentation.whiteboard.component.InfiniteGrid(
                zoom = viewportZoom,
                pan = viewportPan
            )

            // Interaction Timer Logic
            var isInteracting by remember { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(viewportPan, viewportZoom) {
                isInteracting = true
                kotlinx.coroutines.delay(2000)
                isInteracting = false
            }

            // 2. Content (Capture & Draw)
            val coroutineScope = rememberCoroutineScope()
            // Capture Logic Note: capturing 'graphicsLayer' on a Box that wraps DrawingCanvas
            // works if DrawingCanvas draws EVERYTHING. 
            // BUT, since we are moving the transforms INSIDE DrawingCanvas,
            // the 'record' might need to be smart?
            // Actually, for capture, we WANT the shapes to be drawn. 
            // If we capture the 'Canvas', we capture the viewport?
            // Or do we want to capture the whole world? 
            // The previous 'Save to Device' captured the *Viewport*. 
            // "Capture what I see" is standard.
            // So we can wrap the DrawingCanvas in a graphicsLayer just for recording, sans transform?
            // NO, DrawingCanvas will now draw transformed content.
            // So 'graphicsLayer.record { drawContent() }' will capture the screen view. 
            // This is correct for "Screenshot".
            
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

            // 3. HUD Layers (Static on top)
            androidx.compose.animation.AnimatedVisibility(
                visible = isInteracting || state.isDrawingToolCardVisible, // Also show if tools are open (optional)
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 100.dp, end = 20.dp)
            ) {
                org.example.project.presentation.whiteboard.component.Minimap(
                    modifier = Modifier.size(150.dp),
                    shapes = state.shapes,
                    viewportZoom = viewportZoom,
                    viewportPan = viewportPan,
                    viewportSize = Size(1000f, 2000f), // TODO: Get real size via BoxWithConstraints
                    onJumpTo = { newPan ->
                        onEvent(WhiteBoardEvent.OnViewportChange(viewportZoom, newPan))
                    }
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
                onToolSelected = { tool -> 
                    // Toggle Logic: If clicking selected, switch to HAND. Else select new.
                    if (state.selectedTool == tool) {
                        onEvent(WhiteBoardEvent.OnDrawingToolSelected(DrawingTool.HAND))
                    } else {
                        onEvent(WhiteBoardEvent.OnDrawingToolSelected(tool)) 
                    }
                },
                onClosedIconClick = { onEvent(WhiteBoardEvent.OnCloseDrawingToolsCard) },
                isVisible = state.isDrawingToolCardVisible
            )
        }
    }
}

// Keep DrawingCanvas definition below... but fix the gesture handling to avoid conflict?
// Since we used graphicsLayer on the PARENT, the DrawingCanvas receives events in LOCAL coordinates?
// Yes, Compose 'graphicsLayer' transforms the coordinate system for children.
// So drawing at (100,100) will still mean (100,100) in the world.
// However, the Parent's detectTransformGestures might consume touches?
// NO, detectTransformGestures does NOT consume touches if they are just pans/zooms?
// Actually it usually does consume. But we want it to consume 2-finger touches.
// If we draw with 1 finger, DrawingCanvas should handle it.
// Default Compose behavior: Child gets priority.
// If Child handles 1-finger drag, Parent won't see it?
// Parent sees it if using PointerEventPass.Main vs Initial.
// But standard detectTransformGestures works fine with child click handlers usually.

@Composable
private fun DrawingCanvas(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit
) {
        val isSelector = state.selectedTool == DrawingTool.SELECTOR
        val zoom = state.zoom
        val pan = state.pan

        // Helper: Screen -> World
        fun toWorld(screen: Offset): Offset {
             return (screen - pan) / zoom
        }

        Canvas(
            modifier = modifier
                .background(state.canvasBackgroundColor)
                .fillMaxSize()
                // --- GPU ACCELERATION layer ---
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                }
                // --- POINTER INPUTS (Receive Pan/Zoom from Parent or Self?) ---
                // NOTE: pointerInput is attached BEFORE graphicsLayer visually, but logically the modifier order matters.
                // If pointerInput is before graphicsLayer, it receives untransformed events? 
                // Wait, touch detection area is affected by layout, not graphicsLayer.
                // So touch area is still FullScreen.
                // Our 'toWorld' logic handles the math.
                
                // 1. TRANSFORM LISTENER
                .pointerInput(zoom, pan) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        val newZoom = (zoom * gestureZoom).coerceIn(0.1f, 5f)
                        val newPan = pan + gesturePan
                        onEvent(WhiteBoardEvent.OnViewportChange(newZoom, newPan))
                    }
                }
                // ... (Other Pointer Inputs) ...
                // 2. DRAW LISTENER
                .pointerInput(state.selectedTool, isSelector, zoom, pan) {
                    if (state.selectedTool != DrawingTool.HAND && !isSelector) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val count = changes.size
                                
                                if (count == 1) {
                                    val change = changes[0]
                                    if (change.pressed) {
                                        if (change.previousPressed) {
                                            onEvent(WhiteBoardEvent.ContinueDrawing(toWorld(change.position)))
                                        } else {
                                            onEvent(WhiteBoardEvent.StartDrawing(toWorld(change.position)))
                                        }
                                    } else {
                                        if (change.previousPressed) {
                                            onEvent(WhiteBoardEvent.FinishDrawing)
                                        }
                                    }
                                    change.consume() 
                                } else {
                                     onEvent(WhiteBoardEvent.FinishDrawing)
                                }
                            }
                        }
                    }
                }
                // 3. SELECTOR LISTENER
                .pointerInput(isSelector, zoom, pan) {
                     if (isSelector) {
                        detectTapGestures(
                            onTap = { offset -> onEvent(WhiteBoardEvent.StartDrawing(toWorld(offset))) }
                        )
                     }
                }
                // 4. SHAPE TRANSFORM LISTENER
                .pointerInput(isSelector, state.selectedShapeId, zoom, pan) {
                    if (isSelector && state.selectedShapeId != null) {
                        detectTransformGestures { _, gesturePan, gestureZoom, gestureRotation ->
                            val worldPan = gesturePan / zoom
                            onEvent(WhiteBoardEvent.OnShapeTransform(gestureZoom, worldPan, gestureRotation))
                        }
                    }
                }
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
            // NO withTransform here! Drawn at World (0,0)
            
                allShapes.forEach { shape ->
                    val isSelected = shape.id == state.selectedShapeId
             // ... rest of loop ...

             // ... rest of loop (drawing logic is unchanged, just wrapped)


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
                        actualPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(
                                20f,
                                20f
                            ), 0f
                        )
                    }

                    else -> { /* Default Pen/Shapes */
                    }
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
                    if (shape is DrawnShape.Geometric) {
                        centerX = (shape.start.x + shape.end.x) / 2
                        centerY = (shape.start.y + shape.end.y) / 2
                    } else if (shape is DrawnShape.FreeHand) {
                        // Approximate center (Optimization: Cache this?)
                        var minX = Float.POSITIVE_INFINITY;
                        var maxX = Float.NEGATIVE_INFINITY
                        var minY = Float.POSITIVE_INFINITY;
                        var maxY = Float.NEGATIVE_INFINITY
                        shape.points.forEach {
                            minX = min(minX, it.x); maxX = max(maxX, it.x); minY =
                            min(minY, it.y); maxY = max(maxY, it.y)
                        }
                        centerX = (minX + maxX) / 2
                        centerY = (minY + maxY) / 2
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
                                    drawOval(
                                        color = actualColor,
                                        topLeft = topLeft,
                                        size = size,
                                        style = style,
                                        alpha = actualAlpha
                                    )
                                }

                                DrawingTool.RECTANGLE_OUTLINED, DrawingTool.RECTANGLE_FILLED -> {
                                    drawRect(
                                        color = actualColor,
                                        topLeft = topLeft,
                                        size = size,
                                        style = style,
                                        alpha = actualAlpha
                                    )
                                }

                                DrawingTool.LINE_PLANE, DrawingTool.LINE_DOTTED, DrawingTool.PEN -> {
                                    drawLine(
                                        color = actualColor,
                                        start = shape.start,
                                        end = shape.end,
                                        strokeWidth = actualStrokeWidth,
                                        cap = actualCap,
                                        pathEffect = actualPathEffect,
                                        alpha = actualAlpha
                                    )
                                }

                                DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                                    val path =
                                        org.example.project.utils.GeometryHelper.calculateTrianglePath(
                                            shape.start,
                                            shape.end
                                        )
                                    drawPath(
                                        path = path,
                                        color = actualColor,
                                        alpha = actualAlpha,
                                        style = style
                                    )
                                }

                                DrawingTool.ARROW_ONE_SIDED -> {
                                    val path =
                                        org.example.project.utils.GeometryHelper.calculateArrowPath(
                                            shape.start,
                                            shape.end,
                                            false
                                        )
                                    drawPath(
                                        path = path,
                                        color = actualColor,
                                        style = Stroke(
                                            width = actualStrokeWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        ),
                                        alpha = actualAlpha
                                    )
                                }

                                DrawingTool.ARROW_TWO_SIDED -> {
                                    val path =
                                        org.example.project.utils.GeometryHelper.calculateArrowPath(
                                            shape.start,
                                            shape.end,
                                            true
                                        )
                                    drawPath(
                                        path = path,
                                        color = actualColor,
                                        style = Stroke(
                                            width = actualStrokeWidth,
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        ),
                                        alpha = actualAlpha
                                    )
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
                                    drawCircle(
                                        handleStroke,
                                        radius = handleRadius,
                                        center = center,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                } // End withTransform (Selected Shape)
            } // End Loop

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
        val colors = listOf(
            Color.Black,
            Color.Red,
            Color.Blue,
            Color.Green,
            Color.Yellow,
            Color.White,
            Color.Gray
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select Color") },
            text = {
                Row {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
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