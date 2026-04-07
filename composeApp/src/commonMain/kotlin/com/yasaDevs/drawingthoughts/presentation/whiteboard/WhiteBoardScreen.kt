package com.yasaDevs.drawingthoughts.presentation.whiteboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import com.yasaDevs.drawingthoughts.presentation.whiteboard.component.DrawingToolCard
import com.yasaDevs.drawingthoughts.presentation.whiteboard.component.DrawingToolFAB
import com.yasaDevs.drawingthoughts.presentation.whiteboard.component.TopBar
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PickerMode
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.yasaDevs.drawingthoughts.presentation.whiteboard.state.rememberViewportState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

@Composable
fun WhiteBoardScreen(
    modifier: Modifier = Modifier,
    state: WhiteBoardState,
    onEvent: (WhiteBoardEvent) -> Unit,
    imageSaver: com.yasaDevs.drawingthoughts.utils.PlatformImageSaver,
    viewModel: WhiteBoardViewModel
) {
    var showCanvasSetup by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeSlider by remember { mutableStateOf(false) }
    var showShapeVariants by remember { mutableStateOf(false) }
    var lastShapeFamily by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val textMeasurer = rememberTextMeasurer()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

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

    val zoom by remember { derivedStateOf { viewportState.zoom } }
    val pan by remember { derivedStateOf { viewportState.pan } }

    var actualViewportSize by remember { mutableStateOf(Size(1000f, 1000f)) }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = PickerType.Image,
        mode = PickerMode.Single,
        title = "Select Image"
    ) { file ->
        file?.let {
            scope.launch {
                val bytes = it.readBytes()
                onEvent(WhiteBoardEvent.OnAddImage(bytes))
            }
        }
    }

    // Interaction Timer (Hoisted)
    var isInteracting by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pan, zoom) {
        isInteracting = true
        kotlinx.coroutines.delay(2000)
        isInteracting = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.FolderDrawerContent(
                folders = state.folders,
                selectedFolderId = state.selectedFolderId,
                onFolderClick = { folder ->
                    val folderId = if (folder.id == "default") null else folder.id
                    onEvent(WhiteBoardEvent.OnFolderSelect(folderId))
                    scope.launch { drawerState.close() }
                },
                onCreateFolderClick = {
                    onEvent(WhiteBoardEvent.OnCreateFolderRequest)
                }
            )
        }
    ) {
        // LAYER A: WHITE PAPER (Bottom)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(state.canvasBackgroundColor)
        ) {
        // LAYER B: INFINITE CANVAS CONTAINER (Input & Grid)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    actualViewportSize = Size(it.width.toFloat(), it.height.toFloat())
                    viewportState.updateViewportSize(
                        Size(it.width.toFloat(), it.height.toFloat())
                    )
                }
                // 1. TRANSFORM LISTENER (Pinch/Pan)
                // Only allow viewport transform when not actively drawing
                .pointerInput(state.currentShape) {
                    if (state.currentShape == null) {
                        detectTransformGestures { centroid, gesturePan, gestureZoom, _ ->
                            // Zoom relative to centroid for natural feel
                            viewportState.zoomRelativeTo(gestureZoom, centroid)
                            // Pan is already handled by zoomRelativeTo, but add gesture pan
                            viewportState.transform(1f, gesturePan)
                        }
                    }
                }
                // 2. DRAW LISTENER
                .pointerInput(state.selectedTool) {
                    val isSelector = state.selectedTool == DrawingTool.SELECTOR
                    if (state.selectedTool != DrawingTool.HAND && !isSelector) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val world = viewportState.screenToWorld(start)
                                onEvent(WhiteBoardEvent.StartDrawing(world))
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                val world = viewportState.screenToWorld(change.position)
                                onEvent(WhiteBoardEvent.ContinueDrawing(world))
                            },
                            onDragEnd = {
                                onEvent(WhiteBoardEvent.FinishDrawing)
                            },
                            onDragCancel = {
                                onEvent(WhiteBoardEvent.FinishDrawing)
                            }
                        )
                    }
                }
                // 3. TEXT TOOL LISTENER (Tap to create text + Universal double-tap to edit)
                .pointerInput(state.selectedTool) {
                    val isTextTool = state.selectedTool == DrawingTool.TEXT
                    val isPenOrHighlighter = state.selectedTool == DrawingTool.PEN ||
                                             state.selectedTool == DrawingTool.HIGHLIGHTER
                    detectTapGestures(
                        onTap = { offset ->
                            val worldPoint = viewportState.screenToWorld(offset)
                            if (isTextTool) {
                                // Create new text when TEXT tool is active
                                onEvent(WhiteBoardEvent.OnTextCreate(worldPoint))
                            } else if (isPenOrHighlighter) {
                                // Create a dot for single tap with Pen or Highlighter
                                onEvent(WhiteBoardEvent.StartDrawing(worldPoint))
                                onEvent(WhiteBoardEvent.FinishDrawing)
                            }
                        },
                        onDoubleTap = { offset ->
                            // UNIVERSAL: Double-tap to edit text works with ANY tool
                            // This provides industry-standard UX (Figma/Canva behavior)
                            val worldPoint = viewportState.screenToWorld(offset)
                            val textShapes = state.shapes.filterIsInstance<DrawnShape.Text>()
                            val tappedText = com.yasaDevs.drawingthoughts.utils.HitTestUtil.getShapeAt(textShapes, worldPoint) as? DrawnShape.Text
                            if (tappedText != null) {
                                onEvent(WhiteBoardEvent.OnTextEdit(tappedText.id))
                            }
                        }
                    )
                }
                // 4. SELECTOR LISTENER
                .pointerInput(state.selectedTool == DrawingTool.SELECTOR) {
                    val isSelector = state.selectedTool == DrawingTool.SELECTOR
                    if (isSelector) {
                        detectTapGestures(
                            onTap = { offset ->
                                val worldPoint = viewportState.screenToWorld(offset)
                                onEvent(WhiteBoardEvent.StartDrawing(worldPoint))
                            }
                        )
                    }
                }
                // 5. SHAPE TRANSFORM LISTENER (Move/Drag with History Transaction)
                .pointerInput(state.selectedTool == DrawingTool.SELECTOR, state.selectedShapeId) {
                    val isSelector = state.selectedTool == DrawingTool.SELECTOR
                    if (isSelector && state.selectedShapeId != null) {
                        var checkHandle = com.yasaDevs.drawingthoughts.utils.TransformHandle.NONE

                        detectDragGestures(
                            onDragStart = { startOffset ->
                                onEvent(WhiteBoardEvent.OnShapeTransformStart)

                                val worldPoint = viewportState.screenToWorld(startOffset)
                                val shape = state.shapes.find { it.id == state.selectedShapeId }
                                if (shape != null) {
                                    checkHandle =
                                        com.yasaDevs.drawingthoughts.utils.GeometryHelper.getHandleAtPoint(
                                            touchPoint = worldPoint,
                                            bounds = com.yasaDevs.drawingthoughts.utils.GeometryHelper.run { shape.getBounds() },
                                            rotation = 0f,
                                            zoom = viewportState.zoom,
                                            density = this@pointerInput
                                        )
                                } else {
                                    checkHandle = com.yasaDevs.drawingthoughts.utils.TransformHandle.NONE
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val worldDragAmount = dragAmount / viewportState.zoom

                                if (checkHandle == com.yasaDevs.drawingthoughts.utils.TransformHandle.BODY ||
                                    checkHandle == com.yasaDevs.drawingthoughts.utils.TransformHandle.NONE) {
                                    // Move
                                    onEvent(
                                        WhiteBoardEvent.OnShapeTransform(
                                            1f,
                                            worldDragAmount,
                                            0f
                                        )
                                    )
                                } else {
                                    // Resize
                                    onEvent(
                                        WhiteBoardEvent.OnResizeShape(
                                            checkHandle,
                                            worldDragAmount
                                        )
                                    )
                                }
                            },
                            onDragEnd = {
                                onEvent(WhiteBoardEvent.OnShapeTransformEnd)
                                checkHandle = com.yasaDevs.drawingthoughts.utils.TransformHandle.NONE
                            },
                            onDragCancel = {
                                onEvent(WhiteBoardEvent.OnShapeTransformEnd)
                                checkHandle = com.yasaDevs.drawingthoughts.utils.TransformHandle.NONE
                            }
                        )
                    }
                }
        ) {
            // LAYER B: GRID (Middle Layer - Visual Guide Only)
            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.InfiniteGrid(
                zoom = zoom,
                pan = pan,
                pattern = state.selectedPattern
            )

            // LAYER C: INK LAYER (Top Layer - Offscreen Compositing)
            // This is THE critical fix for the eraser.
            // By wrapping the entire ink canvas in an offscreen layer,
            // BlendMode.DstOut in the eraser can subtract alpha from THIS layer only,
            // revealing the grid and white paper beneath, NOT the window background.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // OFFSCREEN COMPOSITING STRATEGY
                        // This renders all ink into an isolated bitmap buffer.
                        // The eraser's BlendMode.DstOut will subtract alpha from this buffer,
                        // creating true transparent holes that reveal Layer A + B beneath.
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.WhiteboardCanvas(
                    shapes = state.shapes,
                    currentShape = state.currentShape,
                    selectionShapeId = state.selectedShapeId,
                    viewportState = viewportState,
                    isDragging = state.isDragging,
                    dragStartPosition = state.dragStartPosition,
                    isCropModeActive = state.isCropModeActive,
                    modifier = Modifier.fillMaxSize()
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
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.Minimap(
                    modifier = Modifier.size(150.dp),
                    shapes = state.shapes,
                    viewportZoom = zoom,
                    viewportPan = pan,
                    viewportSize = actualViewportSize, // CRITICAL FIX: Use actual screen size
                    onJumpTo = { newPan ->
                        onEvent(WhiteBoardEvent.OnViewportChange(zoom, newPan))
                    }
                )
            }

            // --- 2. TOP BAR & UI LAYER (Sitting on top) ---

            TopBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(20.dp),
                onMenuIconClick = {
                    scope.launch { drawerState.open() }
                },
                onInsertImageClick = { filePickerLauncher.launch() },
                onHomeIconClick = { },
                onUndoIconClick = { onEvent(WhiteBoardEvent.OnUndo) },
                onRedoIconClick = { onEvent(WhiteBoardEvent.OnRedo) },
                onClearCanvasClick = { onEvent(WhiteBoardEvent.OnClearCanvasRequest) },
                onCanvasSetupClick = { showCanvasSetup = true },
                onResetViewClick = {
                    onEvent(
                        WhiteBoardEvent.OnViewportChange(
                            1f,
                            Offset.Zero
                        )
                    )
                },
                onExportClick = {
                    // Show export dialog instead of exporting immediately
                    onEvent(WhiteBoardEvent.OnExportRequest)
                },
                onGridSettingsClick = {
                    onEvent(WhiteBoardEvent.OnGridSettingsRequest)
                }
            )

            // --- DIALOGS & OVERLAYS ---
            if (showCanvasSetup) {
                AlertDialog(
                    onDismissRequest = { showCanvasSetup = false },
                    title = { Text("Canvas Setup") },
                    text = {
                        Column {
                            Text("Background Color", style = MaterialTheme.typography.labelMedium)
                            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.inspector.ColorPaletteRow(
                                selectedColor = state.canvasBackgroundColor,
                                onColorSelected = { onEvent(WhiteBoardEvent.OnBackgroundChange(it)) },
                                colors = listOf(
                                    Color.White,
                                    Color(0xFFF8F9FA),
                                    Color(0xFFE9ECEF),
                                    Color(0xFF212529),
                                    Color.Black
                                )
                            )
                        }
                    },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = {
                            showCanvasSetup = false
                        }) { Text("Done") }
                    }
                )
            }

            if (state.showClearConfirmDialog) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.ClearCanvasConfirmDialog(
                    onConfirm = { onEvent(WhiteBoardEvent.OnClearCanvasConfirm) },
                    onDismiss = { onEvent(WhiteBoardEvent.OnClearCanvasCancel) }
                )
            }

            if (state.showExportDialog) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.ExportDialog(
                    onWholeCanvasExport = {
                        scope.launch {
                            exportWholeCanvas(
                                shapes = state.shapes,
                                backgroundColor = state.canvasBackgroundColor,
                                textMeasurer = textMeasurer,
                                density = density,
                                layoutDirection = layoutDirection,
                                imageSaver = imageSaver,
                                snackbarHostState = snackbarHostState
                            )
                        }
                        onEvent(WhiteBoardEvent.OnExportWholeCanvas)
                    },
                    onVisibleScreenExport = {
                        scope.launch {
                            exportVisibleScreen(
                                shapes = state.shapes,
                                backgroundColor = state.canvasBackgroundColor,
                                viewportSize = actualViewportSize,
                                zoom = zoom,
                                pan = pan,
                                textMeasurer = textMeasurer,
                                density = density,
                                layoutDirection = layoutDirection,
                                imageSaver = imageSaver,
                                snackbarHostState = snackbarHostState
                            )
                        }
                        onEvent(WhiteBoardEvent.OnExportVisibleScreen)
                    },
                    onDismiss = { onEvent(WhiteBoardEvent.OnExportDialogDismiss) }
                )
            }

            if (state.showCreateFolderDialog) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.CreateFolderDialog(
                    onConfirm = { name, color ->
                        onEvent(WhiteBoardEvent.OnCreateFolderConfirm(name, color))
                    },
                    onDismiss = { onEvent(WhiteBoardEvent.OnCreateFolderCancel) }
                )
            }

            if (state.showGridSettingsDialog) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.GridSettingsDialog(
                    selectedPattern = state.selectedPattern,
                    onPatternSelect = { pattern ->
                        onEvent(WhiteBoardEvent.OnCanvasPatternChange(pattern))
                    },
                    onConfirm = { onEvent(WhiteBoardEvent.OnGridSettingsConfirm) },
                    onDismiss = { onEvent(WhiteBoardEvent.OnGridSettingsCancel) }
                )
            }

            if (state.showStyleStudioDialog) {
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.StyleStudioDialog(
                    currentBackgroundColor = state.canvasBackgroundColor,
                    currentStrokeColor = state.currentColor,
                    currentFillColor = state.currentColor, // Using stroke color for fill for now
                    currentStrokeWidth = state.currentStrokeWidth,
                    currentAlpha = state.currentColor.alpha,
                    onBackgroundColorChange = { color ->
                        onEvent(WhiteBoardEvent.OnStyleStudioBackgroundChange(color))
                    },
                    onStrokeColorChange = { color ->
                        onEvent(WhiteBoardEvent.OnStyleStudioStrokeChange(color))
                    },
                    onFillColorChange = { color ->
                        onEvent(WhiteBoardEvent.OnStyleStudioFillChange(color))
                    },
                    onStrokeWidthChange = { width ->
                        onEvent(WhiteBoardEvent.OnStyleStudioStrokeWidthChange(width))
                    },
                    onAlphaChange = { alpha ->
                        onEvent(WhiteBoardEvent.OnStyleStudioAlphaChange(alpha))
                    },
                    onDismiss = { onEvent(WhiteBoardEvent.OnStyleStudioDismiss) }
                )
            }

            // OLD COLOR PICKER - Replaced by Style Studio
            // Keeping variable for compatibility but no longer showing dialog
            // if (showColorPicker) {
            //     // Old color picker removed - now using StyleStudioDialog
            // }

            // --- HUD ---
            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.DynamicHUD(
                state = state,
                onColorClick = { onEvent(WhiteBoardEvent.OnStyleStudioRequest) },
                onStrokeWidthClick = { onEvent(WhiteBoardEvent.OnStyleStudioRequest) },
                onShapeSelected = { onEvent(WhiteBoardEvent.OnDrawingToolSelected(it)) },
                onDeleteClick = { onEvent(WhiteBoardEvent.OnDeleteSelectedShape) },
                onFontSizeChange = { onEvent(WhiteBoardEvent.OnTextFontSizeChange(it)) },
                onFontFamilyChange = { onEvent(WhiteBoardEvent.OnTextFontFamilyChange(it)) },
                onFontWeightChange = { onEvent(WhiteBoardEvent.OnTextFontWeightChange(it)) },
                onFontStyleChange = { onEvent(WhiteBoardEvent.OnTextFontStyleChange(it)) },
                onCropClick = { onEvent(WhiteBoardEvent.OnToggleCropMode) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            )

            // --- Text Editing Layer (Full-screen immersive editing) ---
            if (state.isTextEditing) {
                // Get color from currentShape if it exists (for synchronized state)
                val textColor = (state.currentShape as? com.yasaDevs.drawingthoughts.domain.model.DrawnShape.Text)?.color
                    ?: state.currentColor

                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.TextEditingLayer(
                    text = state.currentTextContent,
                    fontSize = state.textFontSize,
                    fontFamily = state.textFontFamily,
                    fontWeight = state.textFontWeight,
                    fontStyle = state.textFontStyle,
                    color = textColor,
                    onTextChange = { onEvent(WhiteBoardEvent.OnTextChange(it)) },
                    onDismiss = {
                        if (state.currentTextContent.isNotBlank()) {
                            onEvent(WhiteBoardEvent.OnTextCommit)
                        } else {
                            onEvent(WhiteBoardEvent.OnTextCancel)
                        }
                    },
                    modifier = Modifier.zIndex(20f)
                )

                // Keyboard-attached toolbar (appears above keyboard)
                // Uses UNIFIED bar - exact same UI as TextToolHUD
                com.yasaDevs.drawingthoughts.presentation.whiteboard.component.KeyboardAttachedToolbar(
                    state = state,
                    visible = state.isTextEditing,
                    onColorClick = { onEvent(WhiteBoardEvent.OnStyleStudioRequest) },
                    onFontSizeChange = { onEvent(WhiteBoardEvent.OnTextFontSizeChange(it)) },
                    onFontFamilyChange = { onEvent(WhiteBoardEvent.OnTextFontFamilyChange(it)) },
                    onFontWeightChange = { onEvent(WhiteBoardEvent.OnTextFontWeightChange(it)) },
                    onFontStyleChange = { onEvent(WhiteBoardEvent.OnTextFontStyleChange(it)) },
                    onDoneClick = {
                        // Save and commit the text
                        if (state.currentTextContent.isNotBlank()) {
                            onEvent(WhiteBoardEvent.OnTextCommit)
                        } else {
                            onEvent(WhiteBoardEvent.OnTextCancel)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(21f)
                )
            }

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
                            Text(
                                "${state.currentStrokeWidth.toInt()}",
                                style = MaterialTheme.typography.labelSmall
                            )


                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {

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

            // Shape variants menu (appears well above toolbar when toggled)
            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.ShapeVariantsMenu(
                currentTool = state.selectedTool,
                isVisible = showShapeVariants,
                onToolSelected = { tool ->
                    onEvent(WhiteBoardEvent.OnDrawingToolSelected(tool))
                    // Keep menu visible after selection - user can toggle it via bottom dock
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp) // Increased from 100dp to give more space
            )

            com.yasaDevs.drawingthoughts.presentation.whiteboard.component.CompactDock(
                selectedTool = state.selectedTool,
                onToolSelect = { onEvent(WhiteBoardEvent.OnDrawingToolSelected(it)) },
                onStrokeWidthChange = { delta ->
                    val newWidth = (state.currentStrokeWidth + delta).coerceIn(1f, 50f)
                    onEvent(WhiteBoardEvent.OnStrokeWidthChange(newWidth))
                },
                onShapeClick = {
                    // Toggle the shape variants menu
                    val currentFamily = state.selectedTool.getShapeFamily()
                    if (showShapeVariants && currentFamily == lastShapeFamily) {
                        // Same family clicked again - hide
                        showShapeVariants = false
                    } else {
                        // Different family or first click - show
                        showShapeVariants = true
                        lastShapeFamily = currentFamily
                        // Select first shape of family if not already a shape
                        if (!state.selectedTool.isShape()) {
                            onEvent(WhiteBoardEvent.OnDrawingToolSelected(DrawingTool.RECTANGLE_OUTLINED))
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // Snackbar for notifications
            androidx.compose.material3.SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            )
        }
    }
    }
}

private fun DrawingTool.isFilled(): Boolean {
        return this == DrawingTool.CIRCLE_FILLED ||
                this == DrawingTool.RECTANGLE_FILLED ||
                this == DrawingTool.TRIANGLE_FILLED
    }

@Composable
fun StrokeWidthDialog(
    currentWidth: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
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
fun ColorPickerDialog(
    currentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
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

// SHARED DRAWING LOGIC (Used for Export)
// Note: OnScreen drawing is now handled by WhiteboardCanvas.kt!
// This is strictly for the ImageSaver export logic.
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDrawnShape(
    shape: DrawnShape,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
        // 1. Resolve Properties
        var actualColor = shape.color
        var actualStrokeWidth = shape.strokeWidth
        var actualAlpha = 1.0f
        var actualCap = androidx.compose.ui.graphics.StrokeCap.Round
        var actualBlendMode = androidx.compose.ui.graphics.BlendMode.SrcOver
        var actualPathEffect: androidx.compose.ui.graphics.PathEffect? = null

        with(this) {
            when (shape.drawingTool) {
                DrawingTool.HIGHLIGHTER -> {
                    actualColor = shape.color.copy(alpha = 0.4f)
                    actualStrokeWidth = shape.strokeWidth * 1.5f
                    actualCap = androidx.compose.ui.graphics.StrokeCap.Square
                    actualBlendMode =
                        androidx.compose.ui.graphics.BlendMode.SrcOver
                }

                DrawingTool.ERASER -> {
                    // EXPORT ERASER: mirror on-screen behavior – punch transparent holes in ink.
                    // The export pipeline records into an offscreen ink layer first, so
                    // BlendMode.DstOut subtracts alpha from that ink just like on screen.
                    actualColor = Color.Black // opaque mask for DstOut
                    actualStrokeWidth = shape.strokeWidth * 1.5f
                    actualBlendMode = androidx.compose.ui.graphics.BlendMode.DstOut
                }

                DrawingTool.LINE_DOTTED -> {
                    actualPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(20f, 20f), 0f
                    )
                }

                else -> {}
            }

            val style = if (shape.drawingTool.isFilled()) {
                Fill
            } else {
                Stroke(
                    width = actualStrokeWidth,
                    cap = actualCap,
                    pathEffect = actualPathEffect
                )
            }

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
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.RECTANGLE_OUTLINED, DrawingTool.RECTANGLE_FILLED -> {
                            drawRect(
                                color = actualColor,
                                topLeft = topLeft,
                                size = size,
                                style = style,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.LINE_PLANE -> {
                            drawLine(
                                color = actualColor,
                                start = shape.start,
                                end = shape.end,
                                strokeWidth = actualStrokeWidth,
                                cap = actualCap,
                                pathEffect = actualPathEffect,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.LINE_DOTTED -> {
                            drawLine(
                                color = actualColor,
                                start = shape.start,
                                end = shape.end,
                                strokeWidth = actualStrokeWidth,
                                cap = actualCap,
                                pathEffect = actualPathEffect,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.TRIANGLE_OUTLINED, DrawingTool.TRIANGLE_FILLED -> {
                            val isFilled = shape.drawingTool == DrawingTool.TRIANGLE_FILLED
                            val triangleStyle = if (isFilled) androidx.compose.ui.graphics.drawscope.Fill
                                               else androidx.compose.ui.graphics.drawscope.Stroke(
                                                   width = actualStrokeWidth,
                                                   cap = actualCap,
                                                   pathEffect = actualPathEffect
                                               )

                            val left = min(shape.start.x, shape.end.x)
                            val right = max(shape.start.x, shape.end.x)
                            val top = min(shape.start.y, shape.end.y)
                            val bottom = max(shape.start.y, shape.end.y)

                            val trianglePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo((left + right) / 2f, top)
                                lineTo(right, bottom)
                                lineTo(left, bottom)
                                close()
                            }

                            drawPath(
                                path = trianglePath,
                                color = actualColor,
                                alpha = actualAlpha,
                                style = triangleStyle,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.ARROW_ONE_SIDED, DrawingTool.ARROW_TWO_SIDED -> {
                            // Arrow heads
                            val arrowHeadLength = actualStrokeWidth * 4f
                            val angle = kotlin.math.atan2(shape.end.y - shape.start.y, shape.end.x - shape.start.x)

                            val lineEnd = Offset(
                                shape.end.x - arrowHeadLength * 0.7f * kotlin.math.cos(angle),
                                shape.end.y - arrowHeadLength * 0.7f * kotlin.math.sin(angle)
                            )

                            val lineStart = if (shape.drawingTool == DrawingTool.ARROW_TWO_SIDED) {
                                Offset(
                                    shape.start.x + arrowHeadLength * 0.7f * kotlin.math.cos(angle),
                                    shape.start.y + arrowHeadLength * 0.7f * kotlin.math.sin(angle)
                                )
                            } else shape.start

                            // Main line
                            drawLine(
                                color = actualColor,
                                start = lineStart,
                                end = lineEnd,
                                strokeWidth = actualStrokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )

                            val arrowAngle = 0.5f
                            val endArrowPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(shape.end.x, shape.end.y)
                                lineTo(
                                    shape.end.x - arrowHeadLength * kotlin.math.cos(angle - arrowAngle),
                                    shape.end.y - arrowHeadLength * kotlin.math.sin(angle - arrowAngle)
                                )
                                lineTo(
                                    shape.end.x - arrowHeadLength * kotlin.math.cos(angle + arrowAngle),
                                    shape.end.y - arrowHeadLength * kotlin.math.sin(angle + arrowAngle)
                                )
                                close()
                            }
                            drawPath(endArrowPath, actualColor, actualAlpha, androidx.compose.ui.graphics.drawscope.Fill, blendMode = actualBlendMode)

                            if (shape.drawingTool == DrawingTool.ARROW_TWO_SIDED) {
                                val startAngle = angle + kotlin.math.PI.toFloat()
                                val startArrowPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(shape.start.x, shape.start.y)
                                    lineTo(
                                        shape.start.x - arrowHeadLength * kotlin.math.cos(startAngle - arrowAngle),
                                        shape.start.y - arrowHeadLength * kotlin.math.sin(startAngle - arrowAngle)
                                    )
                                    lineTo(
                                        shape.start.x - arrowHeadLength * kotlin.math.cos(startAngle + arrowAngle),
                                        shape.start.y - arrowHeadLength * kotlin.math.sin(startAngle + arrowAngle)
                                    )
                                    close()
                                }
                                drawPath(startArrowPath, actualColor, actualAlpha, androidx.compose.ui.graphics.drawscope.Fill, blendMode = actualBlendMode)
                            }
                        }

                        DrawingTool.RECTANGLE_ROUNDED -> {
                            drawRoundRect(
                                color = actualColor,
                                topLeft = topLeft,
                                size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
                                style = style,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.SQUARE_OUTLINED, DrawingTool.SQUARE_FILLED -> {
                            val sideLength = min(size.width, size.height)
                            val squareSize = Size(sideLength, sideLength)
                            drawRect(
                                color = actualColor,
                                topLeft = topLeft,
                                size = squareSize,
                                style = style,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.ELLIPSE_OUTLINED, DrawingTool.ELLIPSE_FILLED -> {
                            drawOval(
                                color = actualColor,
                                topLeft = topLeft,
                                size = size,
                                style = style,
                                alpha = actualAlpha,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.STAR_OUTLINED, DrawingTool.STAR_FILLED -> {
                            val centerX = (shape.start.x + shape.end.x) / 2f
                            val centerY = (shape.start.y + shape.end.y) / 2f
                            val radiusX = abs(shape.end.x - shape.start.x) / 2f
                            val radiusY = abs(shape.end.y - shape.start.y) / 2f
                            val points = 5
                            val outerRadius = max(radiusX, radiusY)
                            val innerRadius = outerRadius * 0.4f

                            val starPath = androidx.compose.ui.graphics.Path().apply {
                                for (i in 0 until points * 2) {
                                    val angle = (i * kotlin.math.PI / points).toFloat() - (kotlin.math.PI / 2).toFloat()
                                    val radius = if (i % 2 == 0) outerRadius else innerRadius
                                    val x = centerX + radius * kotlin.math.cos(angle)
                                    val y = centerY + radius * kotlin.math.sin(angle)
                                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                                }
                                close()
                            }

                            drawPath(
                                path = starPath,
                                color = actualColor,
                                alpha = actualAlpha,
                                style = style,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.PENTAGON -> {
                            val centerX = (shape.start.x + shape.end.x) / 2f
                            val centerY = (shape.start.y + shape.end.y) / 2f
                            val radius = max(abs(shape.end.x - shape.start.x), abs(shape.end.y - shape.start.y)) / 2f
                            val sides = 5

                            val pentagonPath = androidx.compose.ui.graphics.Path().apply {
                                for (i in 0 until sides) {
                                    val angle = (i * 2 * kotlin.math.PI / sides).toFloat() - (kotlin.math.PI / 2).toFloat()
                                    val x = centerX + radius * kotlin.math.cos(angle)
                                    val y = centerY + radius * kotlin.math.sin(angle)
                                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                                }
                                close()
                            }

                            drawPath(
                                path = pentagonPath,
                                color = actualColor,
                                alpha = actualAlpha,
                                style = style,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.HEXAGON -> {
                            val centerX = (shape.start.x + shape.end.x) / 2f
                            val centerY = (shape.start.y + shape.end.y) / 2f
                            val radius = max(abs(shape.end.x - shape.start.x), abs(shape.end.y - shape.start.y)) / 2f
                            val sides = 6

                            val hexagonPath = androidx.compose.ui.graphics.Path().apply {
                                for (i in 0 until sides) {
                                    val angle = (i * 2 * kotlin.math.PI / sides).toFloat() - (kotlin.math.PI / 2).toFloat()
                                    val x = centerX + radius * kotlin.math.cos(angle)
                                    val y = centerY + radius * kotlin.math.sin(angle)
                                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                                }
                                close()
                            }

                            drawPath(
                                path = hexagonPath,
                                color = actualColor,
                                alpha = actualAlpha,
                                style = style,
                                blendMode = actualBlendMode
                            )
                        }

                        DrawingTool.DIAMOND -> {
                            val centerX = (shape.start.x + shape.end.x) / 2f
                            val centerY = (shape.start.y + shape.end.y) / 2f
                            val width = abs(shape.end.x - shape.start.x) / 2f
                            val height = abs(shape.end.y - shape.start.y) / 2f

                            val diamondPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(centerX, centerY - height)
                                lineTo(centerX + width, centerY)
                                lineTo(centerX, centerY + height)
                                lineTo(centerX - width, centerY)
                                close()
                            }

                            drawPath(
                                path = diamondPath,
                                color = actualColor,
                                alpha = actualAlpha,
                                style = style,
                                blendMode = actualBlendMode
                            )
                        }

                        else -> {}
                    }
                }
                is DrawnShape.Text -> {
                    val textLayoutResult = textMeasurer.measure(
                        text = shape.text,
                        style = androidx.compose.ui.text.TextStyle(
                            color = shape.color,
                            fontSize = shape.fontSize.sp,
                            fontFamily = shape.fontFamily,
                            fontWeight = shape.fontWeight,
                            fontStyle = shape.fontStyle
                        )
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = shape.position
                    )
                }
                is DrawnShape.Image -> {
                    shape.bitmap?.let { bitmap ->
                        val dstSize = androidx.compose.ui.unit.IntSize(shape.bounds.width.toInt(), shape.bounds.height.toInt())
                        val dstOffset = androidx.compose.ui.unit.IntOffset(shape.bounds.left.toInt(), shape.bounds.top.toInt())

                        if (shape.cropRect != null) {
                            withTransform({
                                clipRect(
                                    left = shape.cropRect.left,
                                    top = shape.cropRect.top,
                                    right = shape.cropRect.right,
                                    bottom = shape.cropRect.bottom
                                )
                            }) {
                                drawImage(
                                    image = bitmap,
                                    dstOffset = dstOffset,
                                    dstSize = dstSize
                                )
                            }
                        } else {
                            drawImage(
                                image = bitmap,
                                dstOffset = dstOffset,
                                dstSize = dstSize
                            )
                        }
                    }
                }
            }
        }
    }

// Export Helper Functions

/**
 * Export Mode A: Whole Canvas
 * - Calculates bounding box of all shapes
 * - Creates bitmap sized to fit content (with padding)
 * - Draws shapes at scale 1.0 translated to start at (0,0)
 */
private suspend fun exportWholeCanvas(
    shapes: List<DrawnShape>,
    backgroundColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    imageSaver: com.yasaDevs.drawingthoughts.utils.PlatformImageSaver,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    // Show notification
    val snackbarJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        snackbarHostState.showSnackbar(
            message = "✓ Exporting whole canvas...",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
    }

    try {
        // Calculate bounding box of all shapes
        val bounds = com.yasaDevs.drawingthoughts.utils.GeometryHelper.calculateOverallBounds(shapes)

        if (bounds == null || shapes.isEmpty()) {
            // No content to export
            snackbarJob.cancel()
            snackbarHostState.showSnackbar(
                message = "⚠ Canvas is empty, nothing to export",
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            return
        }

        // Add padding
        val padding = 50f
        val contentWidth = (bounds.width + padding * 2).toInt().coerceAtLeast(100)
        val contentHeight = (bounds.height + padding * 2).toInt().coerceAtLeast(100)

        // Create bitmap
        val bitmap = androidx.compose.ui.graphics.ImageBitmap(contentWidth, contentHeight)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)

        // Draw
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = Size(contentWidth.toFloat(), contentHeight.toFloat())
        ) {
            // Draw background
            drawRect(
                color = backgroundColor,
                size = Size(contentWidth.toFloat(), contentHeight.toFloat())
            )

            // Draw ink layer with offscreen compositing
            drawIntoCanvas { canvas ->
                canvas.saveLayer(
                    androidx.compose.ui.geometry.Rect(0f, 0f, contentWidth.toFloat(), contentHeight.toFloat()),
                    androidx.compose.ui.graphics.Paint()
                )

                // Translate so top-left shape is at padding
                canvas.translate(-bounds.left + padding, -bounds.top + padding)

                // Draw all shapes at scale 1.0
                shapes.forEach { shape ->
                    this.drawDrawnShape(shape, textMeasurer)
                }

                canvas.restore()
            }
        }

        imageSaver.saveImage(bitmap)

        // Success notification
        snackbarJob.cancel()
        snackbarHostState.showSnackbar(
            message = "✓ Whole canvas saved (${contentWidth}x${contentHeight}px)",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
    } catch (e: Exception) {
        snackbarJob.cancel()
        snackbarHostState.showSnackbar(
            message = "✗ Export failed: ${e.message}",
            duration = androidx.compose.material3.SnackbarDuration.Long
        )
    }
}

/**
 * Export Mode B: Visible Screen
 * - Creates bitmap sized to viewport
 * - Applies current zoom and pan
 * - Captures exactly what user sees
 */
private suspend fun exportVisibleScreen(
    shapes: List<DrawnShape>,
    backgroundColor: Color,
    viewportSize: Size,
    zoom: Float,
    pan: Offset,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    density: androidx.compose.ui.unit.Density,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    imageSaver: com.yasaDevs.drawingthoughts.utils.PlatformImageSaver,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    // Show notification
    val snackbarJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
        snackbarHostState.showSnackbar(
            message = "✓ Exporting visible screen...",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
    }

    try {
        val width = viewportSize.width.toInt().coerceAtLeast(100)
        val height = viewportSize.height.toInt().coerceAtLeast(100)

        // Create bitmap
        val bitmap = androidx.compose.ui.graphics.ImageBitmap(width, height)
        val canvas = androidx.compose.ui.graphics.Canvas(bitmap)

        // Draw
        androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = Size(width.toFloat(), height.toFloat())
        ) {
            // Draw background
            drawRect(
                color = backgroundColor,
                size = Size(width.toFloat(), height.toFloat())
            )

            // Draw ink layer with offscreen compositing
            drawIntoCanvas { canvas ->
                canvas.saveLayer(
                    androidx.compose.ui.geometry.Rect(0f, 0f, width.toFloat(), height.toFloat()),
                    androidx.compose.ui.graphics.Paint()
                )

                // Apply viewport transform (zoom + pan)
                canvas.scale(zoom, zoom)
                canvas.translate(pan.x / zoom, pan.y / zoom)

                // Draw all shapes with current viewport transform
                shapes.forEach { shape ->
                    this.drawDrawnShape(shape, textMeasurer)
                }

                canvas.restore()
            }
        }

        imageSaver.saveImage(bitmap)

        // Success notification
        snackbarJob.cancel()
        snackbarHostState.showSnackbar(
            message = "✓ Visible screen saved (${width}x${height}px)",
            duration = androidx.compose.material3.SnackbarDuration.Short
        )
    } catch (e: Exception) {
        snackbarJob.cancel()
        snackbarHostState.showSnackbar(
            message = "✗ Export failed: ${e.message}",
            duration = androidx.compose.material3.SnackbarDuration.Long
        )
    }
}
