package com.yasaDevs.drawingthoughts.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.yasaDevs.drawingthoughts.domain.model.DrawingTool
import com.yasaDevs.drawingthoughts.domain.model.DrawnShape
import com.yasaDevs.drawingthoughts.utils.GeometryHelper.getBounds
import com.yasaDevs.drawingthoughts.utils.PathSmoother
import com.yasaDevs.drawingthoughts.utils.toImageBitmap
import kotlin.math.max
import kotlin.math.min

class WhiteBoardViewModel : ViewModel() {

    private val _state = MutableStateFlow(WhiteBoardState())
    val state = _state.asStateFlow()

    private val undoStack = mutableListOf<List<DrawnShape>>()
    private val redoStack = mutableListOf<List<DrawnShape>>()
    private val MAX_HISTORY_SIZE = 50

    // Temporary storage for freehand points before smoothing
    private var currentFreeHandPoints = mutableListOf<Offset>()

    // Transaction Snapshot for Undo/Redo (Transformations)
    private var transactionSnapshot: List<DrawnShape>? = null

    // Auto-save debounce job
    private var autoSaveJob: Job? = null

    // Repository Integration
    private val repository: com.yasaDevs.drawingthoughts.data.repository.ShapeRepository by lazy {
        println("ViewModel: Initializing ShapeRepository and building database")
        val db = com.yasaDevs.drawingthoughts.data.local.getDatabaseBuilder().build()
        println("ViewModel: Database built successfully")
        com.yasaDevs.drawingthoughts.data.repository.ShapeRepository(db.shapeDao())
    }

    private val folderRepository: com.yasaDevs.drawingthoughts.data.repository.FolderRepository by lazy {
        println("ViewModel: Initializing FolderRepository and building database")
        val db = com.yasaDevs.drawingthoughts.data.local.getDatabaseBuilder().build()
        println("ViewModel: Database built successfully for folders")
        com.yasaDevs.drawingthoughts.data.repository.FolderRepository(db.folderDao())
    }

    private val canvasSettingsDao: com.yasaDevs.drawingthoughts.data.local.dao.CanvasSettingsDao by lazy {
        println("ViewModel: Initializing CanvasSettingsDao")
        val db = com.yasaDevs.drawingthoughts.data.local.getDatabaseBuilder().build()
        db.canvasSettingsDao()
    }

    init {
        // Load initial state - "All Drawings" (shapes without folder)
        viewModelScope.launch {
            try {
                val loadedShapes = repository.getShapesByFolder(null)
                _state.update { it.copy(shapes = loadedShapes) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Load canvas settings for initial folder (All Drawings)
        viewModelScope.launch {
            loadCanvasSettingsForCurrentFolder()
        }

        // Collect folders from database
        viewModelScope.launch {
            folderRepository.getAllFolders().collect { folders ->
                _state.update { it.copy(folders = folders) }
            }
        }

        // Auto-save logic with proper debouncing
        viewModelScope.launch {
            state.collect { currentState ->
                // Cancel any pending save
                autoSaveJob?.cancel()

                // Start a new save job with delay
                autoSaveJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(2000) // Wait 2 seconds
                    // Save the LATEST state's shapes (read at save time, not collect time)
                    val latestState = state.value
                    println("ViewModel: Auto-saving ${latestState.shapes.size} shapes for folder ${latestState.selectedFolderId}")
                    // CRITICAL: Use folder-specific save to preserve shapes in other folders
                    repository.saveShapesForFolder(latestState.shapes, latestState.selectedFolderId)
                }
            }
        }
    }

    fun onEvent(event: WhiteBoardEvent) {
        when (event) {
            is WhiteBoardEvent.StartDrawing -> {
                currentFreeHandPoints.clear()
                val tool = _state.value.selectedTool

                if (tool == DrawingTool.SELECTOR) {
                    val visibleShapes = _state.value.shapes.filter { shape ->
                        val isEraser = shape.drawingTool == DrawingTool.ERASER
                        val isInvisibleColor = shape.color == _state.value.canvasBackgroundColor || shape.color == Color.Transparent
                        val isValidSize = when(shape) {
                             is DrawnShape.Geometric -> {
                                 val w = kotlin.math.abs(shape.start.x - shape.end.x)
                                 val h = kotlin.math.abs(shape.start.y - shape.end.y)
                                 w > 5f || h > 5f
                             }
                             is DrawnShape.FreeHand -> shape.points.size > 2
                             is DrawnShape.Text -> shape.text.isNotBlank()
                             is DrawnShape.Image -> true
                        }
                        !isEraser && !isInvisibleColor && isValidSize
                    }
                    val shapeHit = com.yasaDevs.drawingthoughts.utils.HitTestUtil.getShapeAt(visibleShapes, event.offset)
                    _state.update { 
                        it.copy(
                            selectedShapeId = shapeHit?.id,
                            startingOffset = event.offset // Track start of drag
                        ) 
                    }
                } else {
                    // DRAWING TOOLS (Pen, Highlighter, Eraser, etc.)
                    // Object Eraser: delete whole strokes instead of painting a path.
                    if (tool == DrawingTool.ERASER && _state.value.isObjectEraserEnabled) {
                        performObjectErase(event.offset)
                        return
                    }

                    if (isFreeHandTool(tool)) {
                        currentFreeHandPoints.add(event.offset)
                    }
                    _state.update { 
                        it.copy(
                            selectedShapeId = null, // Deselect when drawing new
                            startingOffset = event.offset 
                        ) 
                    }
                }
            }

            is WhiteBoardEvent.ContinueDrawing -> {
                val tool = _state.value.selectedTool
                if (tool == DrawingTool.SELECTOR) {
                   updateSelectedShapePosition(event.offset)
                } else {
                   // Object eraser keeps erasing as you drag instead of drawing a new stroke.
                   if (tool == DrawingTool.ERASER && _state.value.isObjectEraserEnabled) {
                       performObjectErase(event.offset)
                       return
                   }
                   updateContinuingShape(event.offset)
                }
            }

            WhiteBoardEvent.FinishDrawing -> {
                 if (_state.value.selectedTool == DrawingTool.SELECTOR) {
                     _state.update { it.copy(startingOffset = null) }
                 } else {
                    var currentShape = state.value.currentShape
                    val tool = state.value.selectedTool

                    // Handle single tap case: if currentShape is null but we have a starting point, create a dot
                    if (currentShape == null && isFreeHandTool(tool) && currentFreeHandPoints.isNotEmpty()) {
                        val tapPoint = currentFreeHandPoints.first()
                        val path = Path().apply {
                            moveTo(tapPoint.x, tapPoint.y)
                            lineTo(tapPoint.x, tapPoint.y)
                        }
                        val color = state.value.currentColor
                        val strokeWidth = state.value.currentStrokeWidth
                        val folderId = state.value.selectedFolderId
                        val tempId = "temp_${kotlin.random.Random.nextInt()}"

                        currentShape = DrawnShape.FreeHand(
                            tempId, color, strokeWidth, tool, folderId, path, currentFreeHandPoints.toList()
                        )
                    }

                    if (currentShape != null) {
                        addToHistory(state.value.shapes) // Save state BEFORE adding new shape

                        val finalShape = if (currentShape is DrawnShape.FreeHand) {
                            // Smooth the path (rendering layer will detect single taps)
                            val smoothedPath = PathSmoother.createSmoothedPath(currentFreeHandPoints)
                            currentShape.copy(path = smoothedPath, points = currentFreeHandPoints.toList())
                        } else {
                            currentShape
                        }

                        _state.update {
                            it.copy(
                                shapes = it.shapes + finalShape,
                                currentShape = null,
                                startingOffset = null
                            )
                        }
                        redoStack.clear()
                        currentFreeHandPoints.clear()
                    }
                 }
            }

            is WhiteBoardEvent.OnDrawingToolSelected -> {
                _state.update {
                    it.copy(
                        selectedTool = event.tool,
                        isDrawingToolCardVisible = false
                    )
                }
            }

            WhiteBoardEvent.OnFABClick -> {
                _state.update { it.copy(isDrawingToolCardVisible = true) }
            }

            WhiteBoardEvent.OnCloseDrawingToolsCard -> {
                _state.update { it.copy(isDrawingToolCardVisible = false) }
            }

            // Undo/Redo
            WhiteBoardEvent.OnUndo -> performUndo()
            WhiteBoardEvent.OnRedo -> performRedo()

            // Clear Canvas
            WhiteBoardEvent.OnClearCanvasRequest -> {
                _state.update { it.copy(showClearConfirmDialog = true) }
            }
            WhiteBoardEvent.OnClearCanvasConfirm -> {
                performClearCanvas()
                _state.update { it.copy(showClearConfirmDialog = false) }
            }
            WhiteBoardEvent.OnClearCanvasCancel -> {
                _state.update { it.copy(showClearConfirmDialog = false) }
            }

            // Export
            WhiteBoardEvent.OnExportRequest -> {
                _state.update { it.copy(showExportDialog = true) }
            }
            WhiteBoardEvent.OnExportWholeCanvas -> {
                // Logic handled in WhiteBoardScreen
                _state.update { it.copy(showExportDialog = false) }
            }
            WhiteBoardEvent.OnExportVisibleScreen -> {
                // Logic handled in WhiteBoardScreen
                _state.update { it.copy(showExportDialog = false) }
            }
            WhiteBoardEvent.OnExportDialogDismiss -> {
                _state.update { it.copy(showExportDialog = false) }
            }

            // Properties - Update per-tool settings
            is WhiteBoardEvent.OnStrokeWidthChange -> {
                _state.update { current ->
                    val updatedSettings = current.toolSettings.toMutableMap()
                    val currentToolSettings = updatedSettings[current.selectedTool]
                    if (currentToolSettings != null) {
                        updatedSettings[current.selectedTool] = currentToolSettings.copy(
                            strokeWidth = event.width
                        )
                    }
                    current.copy(toolSettings = updatedSettings)
                }
            }
            is WhiteBoardEvent.OnColorChange -> {
                _state.update { current ->
                    // The Eraser is a transparency brush; it does not own a color.
                    if (current.selectedTool == DrawingTool.ERASER) {
                        current
                    } else {
                        val updatedSettings = current.toolSettings.toMutableMap()
                        val currentToolSettings = updatedSettings[current.selectedTool]
                        if (currentToolSettings != null) {
                            updatedSettings[current.selectedTool] = currentToolSettings.copy(
                                color = event.color
                            )
                        }
                        current.copy(toolSettings = updatedSettings)
                    }
                }
            }
            is WhiteBoardEvent.OnBackgroundChange -> {
                _state.update { it.copy(canvasBackgroundColor = event.color) }
            }
            is WhiteBoardEvent.OnToggleEraseMode -> {
                _state.update { it.copy(isObjectEraserEnabled = event.enabled) }
            }
            is WhiteBoardEvent.OnShapeTransform -> {
                _state.update { 
                    it.copy(
                        transientScale = it.transientScale * event.zoom,
                        transientOffset = it.transientOffset + event.pan,
                        transientRotation = it.transientRotation + event.rotation
                    )
                }
            }
            is WhiteBoardEvent.OnResizeShape -> {
                resizeSelectedShape(event.handle, event.dragAmount)
            }
            WhiteBoardEvent.OnShapeTransformStart -> {
                 // Snapshot state before transform begins
                 transactionSnapshot = state.value.shapes

                 // Capture original position of selected shape
                 val selectedShape = state.value.shapes.find { it.id == state.value.selectedShapeId }
                 val originalCenter = selectedShape?.let { shape ->
                     val bounds = com.yasaDevs.drawingthoughts.utils.GeometryHelper.run { shape.getBounds() }
                     Offset(
                         (bounds.left + bounds.right) / 2f,
                         (bounds.top + bounds.bottom) / 2f
                     )
                 }

                 _state.update { it.copy(isDragging = true, dragStartPosition = originalCenter) }
            }
            WhiteBoardEvent.OnShapeTransformEnd -> {
                _state.update { it.copy(isDragging = false, dragStartPosition = null) }
                applyTransientTransform()
            }
            WhiteBoardEvent.OnDeleteSelectedShape -> deleteSelectedShape()
            is WhiteBoardEvent.OnViewportChange -> {
                _state.update {
                    it.copy(
                        zoom = event.zoom,
                        pan = event.pan
                    )
                }
            }

            // Text Tool Events
            is WhiteBoardEvent.OnTextCreate -> {
                addToHistory(_state.value.shapes)
                _state.update { current ->
                    current.copy(
                        isTextEditing = true,
                        currentTextContent = "",
                        currentShape = DrawnShape.Text(
                            id = "text_${System.currentTimeMillis()}",
                            color = current.currentColor,
                            strokeWidth = 0f,
                            drawingTool = DrawingTool.TEXT,
                            folderId = current.selectedFolderId,
                            position = event.position,
                            text = "",
                            fontSize = current.textFontSize,
                            fontFamily = current.textFontFamily,
                            fontWeight = current.textFontWeight,
                            fontStyle = current.textFontStyle
                        )
                    )
                }
            }
            is WhiteBoardEvent.OnTextEdit -> {
                val textShape = _state.value.shapes.find { it.id == event.textId } as? DrawnShape.Text
                if (textShape != null) {
                    addToHistory(_state.value.shapes)
                    _state.update { current ->
                        current.copy(
                            isTextEditing = true,
                            editingTextId = event.textId,
                            currentTextContent = textShape.text,
                            currentShape = textShape, // FIX: Set currentShape so updates work
                            textFontSize = textShape.fontSize,
                            textFontFamily = textShape.fontFamily,
                            textFontWeight = textShape.fontWeight,
                            textFontStyle = textShape.fontStyle
                        )
                    }
                }
            }
            is WhiteBoardEvent.OnTextChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        text = event.text
                    )
                    current.copy(
                        currentTextContent = event.text,
                        currentShape = updatedShape
                    )
                }
            }
            is WhiteBoardEvent.OnTextColorChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        color = event.color
                    )
                    // Also update tool settings so the color persists
                    val updatedSettings = current.toolSettings.toMutableMap()
                    val currentToolSettings = updatedSettings[DrawingTool.TEXT]
                    if (currentToolSettings != null) {
                        updatedSettings[DrawingTool.TEXT] = currentToolSettings.copy(
                            color = event.color
                        )
                    }
                    current.copy(
                        currentShape = updatedShape,
                        toolSettings = updatedSettings
                    )
                }
            }
            is WhiteBoardEvent.OnTextFontSizeChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        fontSize = event.fontSize
                    )
                    current.copy(
                        textFontSize = event.fontSize,
                        currentShape = updatedShape
                    )
                }
            }
            is WhiteBoardEvent.OnTextFontFamilyChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        fontFamily = event.fontFamily
                    )
                    current.copy(
                        textFontFamily = event.fontFamily,
                        currentShape = updatedShape
                    )
                }
            }
            is WhiteBoardEvent.OnTextFontWeightChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        fontWeight = event.fontWeight
                    )
                    current.copy(
                        textFontWeight = event.fontWeight,
                        currentShape = updatedShape
                    )
                }
            }
            is WhiteBoardEvent.OnTextFontStyleChange -> {
                _state.update { current ->
                    val updatedShape = (current.currentShape as? DrawnShape.Text)?.copy(
                        fontStyle = event.fontStyle
                    )
                    current.copy(
                        textFontStyle = event.fontStyle,
                        currentShape = updatedShape
                    )
                }
            }
            WhiteBoardEvent.OnTextCommit -> {
                val currentShape = _state.value.currentShape as? DrawnShape.Text
                val editingId = _state.value.editingTextId

                if (currentShape != null && currentShape.text.isNotBlank()) {
                    _state.update { current ->
                        val updatedShapes = if (editingId != null) {
                            // Editing existing text
                            current.shapes.map { shape ->
                                if (shape.id == editingId) currentShape else shape
                            }
                        } else {
                            // Creating new text
                            current.shapes + currentShape
                        }
                        current.copy(
                            shapes = updatedShapes,
                            isTextEditing = false,
                            editingTextId = null,
                            currentTextContent = "",
                            currentShape = null
                        )
                    }
                } else {
                    // Empty text, just cancel
                    _state.update { current ->
                        current.copy(
                            isTextEditing = false,
                            editingTextId = null,
                            currentTextContent = "",
                            currentShape = null
                        )
                    }
                }
            }
            WhiteBoardEvent.OnTextCancel -> {
                _state.update { current ->
                    current.copy(
                        isTextEditing = false,
                        editingTextId = null,
                        currentTextContent = "",
                        currentShape = null
                    )
                }
                // Undo to remove the uncommitted text
                if (undoStack.isNotEmpty()) {
                    performUndo()
                }
            }

            // Folder System Events
            is WhiteBoardEvent.OnFolderSelect -> {
                handleFolderSelect(event.folderId)
            }
            WhiteBoardEvent.OnCreateFolderRequest -> {
                _state.update { it.copy(showCreateFolderDialog = true) }
            }
            is WhiteBoardEvent.OnCreateFolderConfirm -> {
                handleCreateFolder(event.name, event.color)
            }
            WhiteBoardEvent.OnCreateFolderCancel -> {
                _state.update { it.copy(showCreateFolderDialog = false) }
            }
            is WhiteBoardEvent.OnDeleteFolder -> {
                handleDeleteFolder(event.folder)
            }

            // Grid Settings Events
            WhiteBoardEvent.OnGridSettingsRequest -> {
                _state.update { it.copy(showGridSettingsDialog = true) }
            }
            is WhiteBoardEvent.OnCanvasPatternChange -> {
                _state.update { it.copy(selectedPattern = event.pattern) }
            }
            WhiteBoardEvent.OnGridSettingsConfirm -> {
                _state.update { it.copy(showGridSettingsDialog = false) }
                saveCanvasSettings()
            }
            WhiteBoardEvent.OnGridSettingsCancel -> {
                _state.update { it.copy(showGridSettingsDialog = false) }
            }

            // Style Studio Events
            WhiteBoardEvent.OnStyleStudioRequest -> {
                _state.update { it.copy(showStyleStudioDialog = true) }
            }
            is WhiteBoardEvent.OnStyleStudioBackgroundChange -> {
                _state.update { it.copy(canvasBackgroundColor = event.color) }
                // Save immediately when background color changes
                saveCanvasSettings()
            }
            is WhiteBoardEvent.OnStyleStudioStrokeChange -> {
                val currentTool = _state.value.selectedTool
                val currentSettings = _state.value.toolSettings[currentTool]
                if (currentSettings != null) {
                    val updatedSettings = currentSettings.copy(color = event.color)
                    _state.update {
                        it.copy(
                            toolSettings = it.toolSettings + (currentTool to updatedSettings)
                        )
                    }
                }
            }
            is WhiteBoardEvent.OnStyleStudioFillChange -> {
                // Fill color - for future shape fill implementation
                // Currently not used but reserved for filled shapes
            }
            is WhiteBoardEvent.OnStyleStudioStrokeWidthChange -> {
                val currentTool = _state.value.selectedTool
                val currentSettings = _state.value.toolSettings[currentTool]
                if (currentSettings != null) {
                    val updatedSettings = currentSettings.copy(strokeWidth = event.width)
                    _state.update {
                        it.copy(
                            toolSettings = it.toolSettings + (currentTool to updatedSettings)
                        )
                    }
                }
            }
            is WhiteBoardEvent.OnStyleStudioAlphaChange -> {
                val currentTool = _state.value.selectedTool
                val currentSettings = _state.value.toolSettings[currentTool]
                if (currentSettings != null) {
                    val currentColor = currentSettings.color
                    val newColor = currentColor.copy(alpha = event.alpha)
                    val updatedSettings = currentSettings.copy(color = newColor)
                    _state.update {
                        it.copy(
                            toolSettings = it.toolSettings + (currentTool to updatedSettings)
                        )
                    }
                }
            }
            WhiteBoardEvent.OnStyleStudioDismiss -> {
                _state.update { it.copy(showStyleStudioDialog = false) }
            }

            // Image & Crop
            is WhiteBoardEvent.OnAddImage -> {
                val bytes = event.bytes
                try {
                    val bitmap = bytes.toImageBitmap()
                    val zoom = _state.value.zoom
                    val pan = _state.value.pan
                    
                    // Simple center assuming generic viewport size
                    val startX = (-pan.x / zoom) + 100f
                    val startY = (-pan.y / zoom) + 100f
                    
                    // Cap image display size to prevent massive initial bounds
                    var imgWidth = bitmap.width.toFloat()
                    var imgHeight = bitmap.height.toFloat()
                    if (imgWidth > 1000f || imgHeight > 1000f) {
                        val scale = 1000f / kotlin.math.max(imgWidth, imgHeight)
                        imgWidth *= scale
                        imgHeight *= scale
                    }

                    val imageShape = DrawnShape.Image(
                        id = "img_${kotlin.random.Random.nextInt()}",
                        color = Color.Transparent,
                        strokeWidth = 0f,
                        drawingTool = DrawingTool.IMAGE,
                        folderId = _state.value.selectedFolderId,
                        bitmap = bitmap,
                        bytes = bytes,
                        bounds = androidx.compose.ui.geometry.Rect(startX, startY, startX + imgWidth, startY + imgHeight)
                    )
                    
                    addToHistory(_state.value.shapes)
                    _state.update { it.copy(shapes = it.shapes + imageShape) }
                    redoStack.clear()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            WhiteBoardEvent.OnToggleCropMode -> {
                _state.update { it.copy(isCropModeActive = !it.isCropModeActive) }
            }

        }
    }

    private fun addToHistory(shapes: List<DrawnShape>) {
        if (undoStack.size >= MAX_HISTORY_SIZE) {
            undoStack.removeAt(0)
        }
        undoStack.add(shapes)
    }

    private fun performUndo() {
        if (undoStack.isNotEmpty()) {
            val previousShapes = undoStack.removeLast()
            redoStack.add(state.value.shapes)
            _state.update { it.copy(shapes = previousShapes) }
        }
    }

    private fun performRedo() {
        if (redoStack.isNotEmpty()) {
            val nextShapes = redoStack.removeLast()
            addToHistory(state.value.shapes) // Push current to undo before redoing
            _state.update { it.copy(shapes = nextShapes) }
        }
    }

    private fun performClearCanvas() {
        val currentShapes = state.value.shapes

        // Only clear if there are shapes to clear
        if (currentShapes.isEmpty()) return

        // Save current state to undo stack BEFORE clearing
        addToHistory(currentShapes)

        // Clear all shapes
        _state.update {
            it.copy(
                shapes = emptyList(),
                selectedShapeId = null,
                currentShape = null
            )
        }

        // Clear redo stack (new action invalidates redo)
        redoStack.clear()
    }

    private fun updateSelectedShapePosition(currentOffset: Offset) {
        val startDragOffset = state.value.startingOffset ?: return
        val selectedId = state.value.selectedShapeId ?: return
        
        // Calculate delta
        val deltaX = currentOffset.x - startDragOffset.x
        val deltaY = currentOffset.y - startDragOffset.y
        
        // Update the list of shapes
        val updatedShapes = state.value.shapes.map { shape ->
            if (shape.id == selectedId) {
                when (shape) {
                    is DrawnShape.Geometric -> {
                        shape.copy(
                            start = shape.start.copy(x = shape.start.x + deltaX, y = shape.start.y + deltaY),
                            end = shape.end.copy(x = shape.end.x + deltaX, y = shape.end.y + deltaY)
                        )
                    }
                    is DrawnShape.FreeHand -> {
                        // Move all points
                        val newPoints = shape.points.map {
                            it.copy(x = it.x + deltaX, y = it.y + deltaY)
                        }
                         val newPath = PathSmoother.createSmoothedPath(newPoints)
                         shape.copy(points = newPoints, path = newPath)
                    }
                    is DrawnShape.Text -> {
                        shape.copy(
                            position = shape.position.copy(x = shape.position.x + deltaX, y = shape.position.y + deltaY)
                        )
                    }
                    is DrawnShape.Image -> {
                        shape.copy(
                            bounds = shape.bounds.translate(deltaX, deltaY),
                            cropRect = shape.cropRect?.translate(deltaX, deltaY)
                        )
                    }
                }
            } else {
                shape
            }
        }
        
        _state.update { 
            it.copy(
                shapes = updatedShapes,
                startingOffset = currentOffset // Reset start to current for incremental updates
            )
        }
    }

    private fun updateContinuingShape(currentOffset: Offset) {
        val startOffset = state.value.startingOffset ?: return
        val tool = state.value.selectedTool
        // The Eraser is a transparency brush – its "color" is irrelevant.
        // We still persist a color value for non-eraser tools for compatibility.
        val color = if (tool == DrawingTool.ERASER) {
            Color.Black // Placeholder, never rendered – eraser uses BlendMode.Clear.
        } else {
            val toolColor = state.value.currentColor
            println("updateContinuingShape: tool=$tool, currentColor=$toolColor, toolSettings=${state.value.toolSettings[tool]}")
            toolColor
        }
        val strokeWidth = state.value.currentStrokeWidth
        val folderId = state.value.selectedFolderId
        val tempId = "temp_${kotlin.random.Random.nextInt()}"

        val newShape: DrawnShape = if (isFreeHandTool(tool)) {
            currentFreeHandPoints.add(currentOffset)
            // For live preview, we use simple lineTo commands to ensure round caps at both ends
            val path = Path().apply {
                if (currentFreeHandPoints.isNotEmpty()) {
                    moveTo(currentFreeHandPoints.first().x, currentFreeHandPoints.first().y)
                    // Draw lines to all subsequent points
                    for (i in 1 until currentFreeHandPoints.size) {
                        lineTo(currentFreeHandPoints[i].x, currentFreeHandPoints[i].y)
                    }
                }
            }
            DrawnShape.FreeHand(tempId, color, strokeWidth, tool, folderId, path, currentFreeHandPoints.toList())
        } else {
            // Geometric shapes
            val fixedStart = if (startOffset == Offset.Unspecified) currentOffset else startOffset
            DrawnShape.Geometric(tempId, color, strokeWidth, tool, folderId, fixedStart, currentOffset)
        }

        _state.update { it.copy(currentShape = newShape) }
    }

    private fun isFreeHandTool(tool: DrawingTool): Boolean {
        return when (tool) {
            DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.LASER_PEN, DrawingTool.ERASER -> true
            else -> false
        }
    }

    private fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * "Object Eraser" – deletes entire strokes whose bounds intersect the touch point.
     * This operates on already-committed shapes instead of drawing a new stroke.
     */
    private fun performObjectErase(worldPoint: Offset) {
        val currentShapes = state.value.shapes
        if (currentShapes.isEmpty()) return

        // Only consider visible, non-eraser strokes as erasable "objects".
        val remaining = currentShapes.filter { shape ->
            val isEraserStroke = shape.drawingTool == DrawingTool.ERASER
            if (isEraserStroke) {
                true
            } else {
                // Remove stroke if the touch point falls inside its bounds.
                !shape.getBounds().contains(worldPoint)
            }
        }

        if (remaining.size == currentShapes.size) return // Nothing hit

        // Snapshot for undo BEFORE mutating.
        addToHistory(currentShapes)
        redoStack.clear()

        _state.update { it.copy(shapes = remaining, selectedShapeId = null) }
    }

    private fun applyTransientTransform() {
        val selectedId = state.value.selectedShapeId ?: return
        val scale = state.value.transientScale
        val offset = state.value.transientOffset
        
        if (scale == 1f && offset == Offset.Zero) {
            transactionSnapshot = null // No change
            return
        }
        
        val updatedShapes = state.value.shapes.map { shape ->
            if (shape.id == selectedId) {
                when (shape) {
                    is DrawnShape.Geometric -> {
                        val centerX = (shape.start.x + shape.end.x) / 2
                        val centerY = (shape.start.y + shape.end.y) / 2
                        
                        val width = (shape.end.x - shape.start.x) * scale
                        val height = (shape.end.y - shape.start.y) * scale
                        
                        val newHalfWidth = width / 2
                        val newHalfHeight = height / 2
                        
                        val newCenterX = centerX + offset.x
                        val newCenterY = centerY + offset.y
                        
                        shape.copy(
                            start = Offset(newCenterX - newHalfWidth, newCenterY - newHalfHeight),
                            end = Offset(newCenterX + newHalfWidth, newCenterY + newHalfHeight)
                        )
                    }
                    is DrawnShape.FreeHand -> {
                        var minX = Float.POSITIVE_INFINITY
                        var minY = Float.POSITIVE_INFINITY
                        var maxX = Float.NEGATIVE_INFINITY
                        var maxY = Float.NEGATIVE_INFINITY
                        shape.points.forEach { 
                            minX = min(minX, it.x)
                            maxX = max(maxX, it.x)
                            minY = min(minY, it.y)
                            maxY = max(maxY, it.y)
                        }
                        val centerX = (minX + maxX) / 2
                        val centerY = (minY + maxY) / 2
                        
                        val newCenterX = centerX + offset.x
                        val newCenterY = centerY + offset.y
                        
                        val newPoints = shape.points.map { p ->
                            val relX = (p.x - centerX) * scale
                            val relY = (p.y - centerY) * scale
                            Offset(newCenterX + relX, newCenterY + relY)
                        }
                        val newPath = PathSmoother.createSmoothedPath(newPoints)
                        shape.copy(points = newPoints, path = newPath)
                    }
                    is DrawnShape.Text -> {
                        // Scale font size and move position
                        val newCenterX = shape.position.x + offset.x
                        val newCenterY = shape.position.y + offset.y
                        shape.copy(
                            position = Offset(newCenterX, newCenterY),
                            fontSize = shape.fontSize * scale
                        )
                    }
                    is DrawnShape.Image -> {
                        val centerX = shape.bounds.center.x
                        val centerY = shape.bounds.center.y
                        
                        val width = shape.bounds.width * scale
                        val height = shape.bounds.height * scale
                        
                        val newCenterX = centerX + offset.x
                        val newCenterY = centerY + offset.y
                        
                        val newBounds = androidx.compose.ui.geometry.Rect(
                            newCenterX - width / 2, newCenterY - height / 2,
                            newCenterX + width / 2, newCenterY + height / 2
                        )
                        
                        val newCropRect = shape.cropRect?.let { crop ->
                            val cropCenterX = crop.center.x
                            val cropCenterY = crop.center.y
                            val cropWidth = crop.width * scale
                            val cropHeight = crop.height * scale
                            val relCropX = (cropCenterX - centerX) * scale
                            val relCropY = (cropCenterY - centerY) * scale
                            androidx.compose.ui.geometry.Rect(
                                newCenterX + relCropX - cropWidth / 2, newCenterY + relCropY - cropHeight / 2,
                                newCenterX + relCropX + cropWidth / 2, newCenterY + relCropY + cropHeight / 2
                            )
                        }
                        
                        shape.copy(bounds = newBounds, cropRect = newCropRect)
                    }
                }
            } else {
                shape
            }
        }
        
        // COMMIT TO HISTORY (Transaction)
        if (transactionSnapshot != null) {
            addToHistory(transactionSnapshot!!) // Save the snapshot (OLD state)
        }
        transactionSnapshot = null

        _state.update { 
            it.copy(
                shapes = updatedShapes,
                transientScale = 1f,
                transientOffset = Offset.Zero,
                transientRotation = 0f
            )
        }
        redoStack.clear()
    }

    private fun deleteSelectedShape() {
        val selectedId = state.value.selectedShapeId ?: return
        val currentShapes = state.value.shapes
        
        addToHistory(currentShapes) // Save state before delete
        
        val newShapes = currentShapes.filter { it.id != selectedId }
        
        _state.update { 
            it.copy(
                shapes = newShapes,
                selectedShapeId = null,
                isDrawingToolCardVisible = false
            ) 
        }
        redoStack.clear()
    }

    private fun resizeSelectedShape(handle: com.yasaDevs.drawingthoughts.utils.TransformHandle, worldDelta: Offset) {
        val selectedId = state.value.selectedShapeId ?: return
        val shapes = state.value.shapes
        val shape = shapes.find { it.id == selectedId } ?: return
        
        // Resize Logic directly modifies the shape (Transient would be complex for individual handles)
        // Since we snapshot at Start, we can modify directly safely for Undo support?
        // Wait, 'OnShapeTransformStart' takes a snapshot.
        // If we modify 'shapes' directly here, the 'transactionSnapshot' will hold the OLD state.
        // When 'OnShapeTransformEnd' is called, it checks 'transactionSnapshot'.
        // If we modify state directly here, we need to ensure 'OnShapeTransformEnd' logic doesn't overwrite it
        // or that it commits it.
        // Currently 'OnShapeTransformEnd' uses 'applyTransientTransform' which merges transient state.
        // IF we modify actual shapes here, transient state is irrelevant (Scale=1, Offset=0).
        // BUT 'OnShapeTransformEnd' does: "if (transactionSnapshot != null) addToHistory(snapshot)".
        // So yes, modifying directly here is Compatibile with the Snapshot pattern!
        
        val updatedShapes = shapes.map { current ->
            if (current.id == selectedId) {
                when (current) {
                    is DrawnShape.Geometric -> {
                        var newStart = current.start
                        var newEnd = current.end
                        
                        when (handle) {
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.RIGHT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_RIGHT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newEnd = newEnd.copy(x = newEnd.x + worldDelta.x)
                            }
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_LEFT -> {
                                newStart = newStart.copy(x = newStart.x + worldDelta.x)
                            }
                            else -> {}
                        }
                        
                        when (handle) {
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_LEFT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newEnd = newEnd.copy(y = newEnd.y + worldDelta.y)
                            }
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_RIGHT -> {
                                newStart = newStart.copy(y = newStart.y + worldDelta.y)
                            }
                            else -> {}
                        }
                        current.copy(start = newStart, end = newEnd)
                    }
                    is DrawnShape.FreeHand -> current // No resizing for Freehand yet
                    is DrawnShape.Text -> current // No resizing for Text yet
                    is DrawnShape.Image -> {
                        val activeRect = if (state.value.isCropModeActive) {
                            current.cropRect ?: current.bounds
                        } else {
                            current.bounds
                        }
                        
                        var newLeft = activeRect.left
                        var newTop = activeRect.top
                        var newRight = activeRect.right
                        var newBottom = activeRect.bottom
                        
                        when (handle) {
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.RIGHT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_RIGHT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newRight += worldDelta.x
                            }
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_LEFT -> {
                                newLeft += worldDelta.x
                            }
                            else -> {}
                        }
                        
                        when (handle) {
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_LEFT, 
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newBottom += worldDelta.y
                            }
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_LEFT,
                            com.yasaDevs.drawingthoughts.utils.TransformHandle.TOP_RIGHT -> {
                                newTop += worldDelta.y
                            }
                            else -> {}
                        }
                        
                        val newRect = androidx.compose.ui.geometry.Rect(newLeft, newTop, newRight, newBottom)
                        if (state.value.isCropModeActive) {
                            current.copy(cropRect = newRect)
                        } else {
                            // When resizing bounds directly, proportionally scale the cropRect map? 
                            // Simplified: Keep cropRect relative to bounds, or just drop cropRect if resizing bounds. 
                            // For Figma-style, resizing an image frame resizes the bounds but keeps the image content anchored or stretches it.
                            // If we stretch it, we stretch the cropRect accordingly.
                            val scaleX = newRect.width / current.bounds.width
                            val scaleY = newRect.height / current.bounds.height
                            
                            val newCropRect = current.cropRect?.let { crop ->
                                val cx = newLeft + (crop.left - current.bounds.left) * scaleX
                                val cy = newTop + (crop.top - current.bounds.top) * scaleY
                                val cw = crop.width * scaleX
                                val ch = crop.height * scaleY
                                androidx.compose.ui.geometry.Rect(cx, cy, cx + cw, cy + ch)
                            }
                            current.copy(bounds = newRect, cropRect = newCropRect)
                        }
                    }
                }
            } else {
                current
            }
        }

        _state.update { it.copy(shapes = updatedShapes) }
    }

    // Folder System Handlers
    private fun handleFolderSelect(folderId: String?) {
        viewModelScope.launch {
            try {
                println("ViewModel: handleFolderSelect - Switching from folder ${_state.value.selectedFolderId} to folder $folderId")

                // Auto-save current shapes to their folder before switching (even if empty)
                val currentFolderId = _state.value.selectedFolderId
                val currentShapes = _state.value.shapes
                println("ViewModel: Current folder has ${currentShapes.size} shapes in memory")
                currentShapes.forEachIndexed { index, shape ->
                    println("  Shape $index: folderId=${shape.folderId}, type=${shape::class.simpleName}")
                }

                repository.saveShapesForFolder(currentShapes, currentFolderId)
                println("ViewModel: Saved shapes for current folder $currentFolderId")

                // Load shapes for the selected folder
                println("ViewModel: Loading shapes for new folder $folderId")
                val loadedShapes = repository.getShapesByFolder(folderId)
                println("ViewModel: Loaded ${loadedShapes.size} shapes for folder $folderId")

                _state.update {
                    it.copy(
                        selectedFolderId = folderId,
                        shapes = loadedShapes,
                        selectedShapeId = null,
                        currentShape = null
                    )
                }
                println("ViewModel: Folder switch complete - now showing ${loadedShapes.size} shapes")

                // Load grid pattern for the new folder
                loadCanvasSettingsForCurrentFolder()

                // Clear undo/redo stacks when switching folders
                undoStack.clear()
                redoStack.clear()
            } catch (e: Exception) {
                println("ViewModel: ERROR in handleFolderSelect: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun handleCreateFolder(name: String, color: Color) {
        viewModelScope.launch {
            try {
                val newFolder = com.yasaDevs.drawingthoughts.domain.model.Folder(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    color = color
                )
                folderRepository.insertFolder(newFolder)
                _state.update { it.copy(showCreateFolderDialog = false) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Converts folder ID to a key for canvas settings storage.
     * null folder ID -> "ALL_DRAWINGS"
     * actual folder ID -> use the folder ID as-is
     */
    private fun getFolderKey(folderId: String?): String {
        return folderId ?: "ALL_DRAWINGS"
    }

    /**
     * Loads canvas settings (grid pattern and background color) for the currently selected folder.
     */
    private suspend fun loadCanvasSettingsForCurrentFolder() {
        try {
            val currentFolderId = _state.value.selectedFolderId
            val folderKey = getFolderKey(currentFolderId)
            val settings = canvasSettingsDao.getCanvasSettings(folderKey)

            if (settings != null) {
                val pattern = com.yasaDevs.drawingthoughts.domain.model.CanvasPattern.fromString(settings.selectedPattern)
                val backgroundColor = androidx.compose.ui.graphics.Color(settings.backgroundColor.toULong())
                _state.update { it.copy(selectedPattern = pattern, canvasBackgroundColor = backgroundColor) }
                println("ViewModel: Loaded canvas settings for folder '$folderKey': pattern=$pattern, bgColor=$backgroundColor")
            } else {
                // No settings saved for this folder yet, use defaults
                _state.update {
                    it.copy(
                        selectedPattern = com.yasaDevs.drawingthoughts.domain.model.CanvasPattern.DEFAULT,
                        canvasBackgroundColor = androidx.compose.ui.graphics.Color.White
                    )
                }
                println("ViewModel: No canvas settings for folder '$folderKey', using defaults")
            }
        } catch (e: Exception) {
            println("ViewModel: Error loading canvas settings: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Saves canvas settings (grid pattern and background color) for the currently selected folder.
     */
    private fun saveCanvasSettings() {
        viewModelScope.launch {
            try {
                val currentFolderId = _state.value.selectedFolderId
                val folderKey = getFolderKey(currentFolderId)
                val currentPattern = _state.value.selectedPattern
                val currentBackgroundColor = _state.value.canvasBackgroundColor

                val settingsEntity = com.yasaDevs.drawingthoughts.data.local.entity.CanvasSettingsEntity(
                    folderId = folderKey,
                    selectedPattern = currentPattern.name,
                    backgroundColor = currentBackgroundColor.value.toLong(),
                    updatedAt = System.currentTimeMillis()
                )
                canvasSettingsDao.insertOrUpdateSettings(settingsEntity)
                println("ViewModel: Saved canvas settings for folder '$folderKey': pattern=$currentPattern, bgColor=$currentBackgroundColor")
            } catch (e: Exception) {
                println("ViewModel: Error saving canvas settings: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun handleDeleteFolder(folder: com.yasaDevs.drawingthoughts.domain.model.Folder) {
        viewModelScope.launch {
            try {
                // Delete all shapes in this folder
                repository.deleteShapesByFolder(folder.id)

                // Delete the folder itself
                folderRepository.deleteFolder(folder)

                // If the deleted folder was selected, switch to "All Drawings"
                if (_state.value.selectedFolderId == folder.id) {
                    handleFolderSelect(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // CRITICAL: Save all data immediately when ViewModel is destroyed
        // This ensures data is persisted when the app is closed or activity is destroyed
        println("ViewModel: onCleared() called - saving data immediately")

        // Cancel pending auto-save to avoid conflicts
        autoSaveJob?.cancel()

        // Perform immediate synchronous save using runBlocking
        // This is acceptable in onCleared() as it only happens once during cleanup
        kotlinx.coroutines.runBlocking {
            try {
                val currentState = state.value
                println("ViewModel: Saving ${currentState.shapes.size} shapes for folder ${currentState.selectedFolderId}")
                // Save shapes for current folder only
                repository.saveShapesForFolder(currentState.shapes, currentState.selectedFolderId)
                println("ViewModel: Save completed successfully")
            } catch (e: Exception) {
                println("ViewModel: Error saving in onCleared: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
