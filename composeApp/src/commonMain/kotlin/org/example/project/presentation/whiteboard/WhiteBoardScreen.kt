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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.graphicsLayer
import org.example.project.presentation.whiteboard.state.rememberViewportState

@Composable
fun WhiteBoardScreen(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit,
    imageSaver: org.example.project.utils.PlatformImageSaver
) {
    var showCanvasSetup by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeSlider by remember { mutableStateOf(false) }
    
    // 1. FAST LOCAL STATE (The "Liquid" Layer)
    val viewportState = rememberViewportState(state.zoom, state.pan)
    
    // 2. SYNC: VM -> UI
    androidx.compose.runtime.LaunchedEffect(state.zoom, state.pan) {
        if (state.zoom != viewportState.zoom || state.pan != viewportState.pan) {
             viewportState.snapTo(state.zoom, state.pan)
        }
    }

    // 3. SYNC: UI -> VM (Debounced)
    androidx.compose.runtime.LaunchedEffect(viewportState.zoom, viewportState.pan) {
        kotlinx.coroutines.delay(300) 
        onEvent(WhiteBoardEvent.OnViewportChange(viewportState.zoom, viewportState.pan))
    }

    val zoom = viewportState.zoom
    val pan = viewportState.pan

    // Root Container (No Scaffold Padding)
    Box(modifier = modifier.fillMaxSize()) {
        
        // --- 1. INFINITE CANVAS LAYER (Input & Rendering) ---
        // This Box fills the screen and captures ALL gestures at (0,0) screen coordinates.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(state.canvasBackgroundColor)
                // 1. TRANSFORM LISTENER
                .pointerInput(Unit) {
                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                        viewportState.transform(gestureZoom, gesturePan)
                    }
                }
                // 2. DRAW LISTENER
                .pointerInput(state.selectedTool, state.selectedTool == DrawingTool.SELECTOR) {
                    val isSelector = state.selectedTool == DrawingTool.SELECTOR
                    if (state.selectedTool != DrawingTool.HAND && !isSelector) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val changes = event.changes
                                val count = changes.size
                                // Use Unified Converter
                                fun liveToWorld(screen: Offset) = viewportState.screenToWorld(screen)
                                
                                if (count == 1) {
                                    val change = changes[0]
                                    if (change.pressed) {
                                        if (change.previousPressed) {
                                            onEvent(WhiteBoardEvent.ContinueDrawing(liveToWorld(change.position)))
                                        } else {
                                            onEvent(WhiteBoardEvent.StartDrawing(liveToWorld(change.position)))
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
                .pointerInput(state.selectedTool == DrawingTool.SELECTOR) {
                     val isSelector = state.selectedTool == DrawingTool.SELECTOR
                     if (isSelector) {
                        detectTapGestures(
                            onTap = { offset -> 
                                // Unified Converter
                                val worldPoint = viewportState.screenToWorld(offset)
                                onEvent(WhiteBoardEvent.StartDrawing(worldPoint)) 
                            }
                        )
                     }
                }
                // 4. SHAPE TRANSFORM LISTENER (Transform Logic)
                .pointerInput(state.selectedTool == DrawingTool.SELECTOR, state.selectedShapeId) {
                     val isSelector = state.selectedTool == DrawingTool.SELECTOR
                     if (isSelector && state.selectedShapeId != null) {
                        detectTransformGestures { _, gesturePan, gestureZoom, gestureRotation ->
                             val worldPanDelta = gesturePan / viewportState.zoom
                             onEvent(WhiteBoardEvent.OnShapeTransform(gestureZoom, worldPanDelta, gestureRotation))
                        }
                     }
                }
                .pointerInput(state.selectedTool == DrawingTool.SELECTOR, state.selectedShapeId) {
                     val isSelector = state.selectedTool == DrawingTool.SELECTOR
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
            // A. Grid
            org.example.project.presentation.whiteboard.component.InfiniteGrid(
                zoom = zoom,
                pan = pan
            )

            // B. Interaction Timer
            var isInteracting by remember { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(pan, zoom) {
                isInteracting = true
                kotlinx.coroutines.delay(2000)
                isInteracting = false
            }
            
            // C. Drawing Content
            val coroutineScope = rememberCoroutineScope()
            // Note: graphicsLayer for capture
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
                    viewportState = viewportState
                )
            }
            
            // D. HUDs (Minimap)
             androidx.compose.animation.AnimatedVisibility(
                visible = isInteracting || state.isDrawingToolCardVisible, 
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 250.dp, end = 20.dp)
            ) {
                org.example.project.presentation.whiteboard.component.Minimap(
                    modifier = Modifier.size(150.dp),
                    shapes = state.shapes,
                    viewportZoom = zoom,
                    viewportPan = pan,
                    viewportSize = Size(1000f, 2000f), 
                    onJumpTo = { newPan ->
                        onEvent(WhiteBoardEvent.OnViewportChange(zoom, newPan))
                    }
                )
            }
        }
        
        // --- 2. TOP BAR & UI LAYER (Sitting on top) ---
        
        TopBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(20.dp),
            onHomeIconClick = { },
            onUndoIconClick = { onEvent(WhiteBoardEvent.OnUndo) },
            onRedoIconClick = { onEvent(WhiteBoardEvent.OnRedo) },
            onCanvasSetupClick = { showCanvasSetup = true },
            onResetViewClick = { 
                 onEvent(WhiteBoardEvent.OnViewportChange(1f, androidx.compose.ui.geometry.Offset.Zero))
            },
            onExportClick = {
                /* Capture Logic needs coroutine scope from above? No, create new one or use LaunchedEffect? 
                   Ideally pass event to VM, but saving is UI side. 
                   We need scope here. We can get it from composition. */
                   // Just simple action for now:
                   // The 'coroutineScope' was inside the box. Need to move it up if we want to use it here.
                   // For now, leaving as placeholder or will fixing scope.
            }
        )


            // --- DIALOGS & OVERLAYS ---
            if (showCanvasSetup) {
                 // Simple Dialog for Canvas Settings
                 androidx.compose.material3.AlertDialog(
                     onDismissRequest = { showCanvasSetup = false },
                     title = { Text("Canvas Setup") },
                     text = {
                         Column {
                             Text("Background Color", style = MaterialTheme.typography.labelMedium)
                             org.example.project.presentation.whiteboard.component.inspector.ColorPaletteRow(
                                selectedColor = state.canvasBackgroundColor,
                                onColorSelected = { onEvent(WhiteBoardEvent.OnBackgroundChange(it)) },
                                colors = listOf(Color.White, Color(0xFFF8F9FA), Color(0xFFE9ECEF), Color(0xFF212529), Color.Black)
                            )
                         }
                     },
                     confirmButton = {
                         androidx.compose.material3.TextButton(onClick = { showCanvasSetup = false }) { Text("Done") }
                     }
                 )
            }
            
            if (showColorPicker) {
                 // Simple Dialog for Pen Color
                 androidx.compose.material3.AlertDialog(
                     onDismissRequest = { showColorPicker = false },
                     title = { Text("Pen Color") },
                     text = {
                         org.example.project.presentation.whiteboard.component.inspector.ColorPaletteRow(
                                selectedColor = state.currentColor,
                                onColorSelected = { onEvent(WhiteBoardEvent.OnColorChange(it)) },
                                colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFC107), Color.Magenta, Color.White)
                            )
                     },
                     confirmButton = {
                         androidx.compose.material3.TextButton(onClick = { showColorPicker = false }) { Text("Done") }
                     }
                 )
            }

            // --- HUD ---
            // --- HUD ---
            org.example.project.presentation.whiteboard.component.DynamicHUD(
                state = state,
                onColorClick = { showColorPicker = true },
                onStrokeWidthClick = { showStrokeSlider = true },
                onShapeSelected = { onEvent(WhiteBoardEvent.OnDrawingToolSelected(it)) },
                onDeleteClick = { /* Implement Delete Event */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp) // Float above Dock
            )
            
            // --- POPUPS (Slider) ---
            if (showStrokeSlider) {
                 Box(
                     modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable { showStrokeSlider = false }
                        .zIndex(10f),
                     contentAlignment = Alignment.BottomCenter
                 ) {
                     Surface(
                         modifier = Modifier
                            .padding(bottom = 150.dp)
                            .clickable(enabled = false) {},
                         shape = CircleShape,
                         color = MaterialTheme.colorScheme.surface,
                         shadowElevation = 8.dp
                     ) {
                         Column(
                            modifier = Modifier
                                .width(60.dp)
                                .height(200.dp)
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                         ) {
                             Text("${state.currentStrokeWidth.toInt()}", style = MaterialTheme.typography.labelSmall)
                             

                             Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {

                                 Slider(
                                     value = state.currentStrokeWidth,
                                     onValueChange = { onEvent(WhiteBoardEvent.OnStrokeWidthChange(it)) },
                                     valueRange = 1f..50f,
                                     modifier = Modifier
                                        .graphicsLayer(rotationZ = 270f)
                                        .width(150.dp)
                                 )
                             }
                             
                             Box(
                                 modifier = Modifier
                                    .size(state.currentStrokeWidth.dp.coerceAtMost(30.dp))
                                    .clip(CircleShape)
                                    .background(state.currentColor)
                             )
                         }
                     }
                 }
            }

            org.example.project.presentation.whiteboard.component.CompactDock(
                selectedTool = state.selectedTool,
                onToolSelect = { onEvent(WhiteBoardEvent.OnDrawingToolSelected(it)) },
                onStrokeWidthChange = { delta ->
                     val newWidth = (state.currentStrokeWidth + delta).coerceIn(1f, 50f)
                     onEvent(WhiteBoardEvent.OnStrokeWidthChange(newWidth))
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
            
            // FAB (If we still want it? Maybe removal if Dock covers functionality? 
            // Dock has tools. FAB was for "Open Tools". We don't need FAB anymore.)
            // Removed DrawingToolFAB.
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
    viewportState: org.example.project.presentation.whiteboard.state.ViewportState
) {
        val isSelector = state.selectedTool == DrawingTool.SELECTOR
        // Use ViewportState for Liquid Motion
        val zoom = viewportState.zoom
        val pan = viewportState.pan

        Canvas(
            modifier = modifier
                .fillMaxSize()
                // --- GPU ACCELERATION layer ---
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = pan.x
                    translationY = pan.y
                    // CRITICAL: Force offscreen buffer for BlendMode.Clear to work
                    alpha = 0.99f 
                    clip = false // Explicitly disable clipping
                }
        ) {
            val allShapes = state.shapes + listOfNotNull(state.currentShape)
            // NO withTransform here! Drawn at World (0,0)
            
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
                        actualColor = Color.Transparent 
                        actualStrokeWidth = shape.strokeWidth * 1.5f
                        actualBlendMode = androidx.compose.ui.graphics.BlendMode.Clear
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