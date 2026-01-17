package org.example.project.presentation.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.model.DrawingTool
import org.example.project.domain.model.DrawnShape
import org.example.project.utils.GeometryHelper.getBounds
import org.example.project.utils.PathSmoother
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
    
    // Repository Integration
    private val repository: org.example.project.data.repository.ShapeRepository by lazy {
        val db = org.example.project.data.local.getDatabaseBuilder().build()
        org.example.project.data.repository.ShapeRepository(db.shapeDao())
    }

    init {
        // Load initial state
        viewModelScope.launch {
            try {
                val loadedShapes = repository.getShapes()
                _state.update { it.copy(shapes = loadedShapes) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Auto-save logic (Debounce)
        viewModelScope.launch {
            state.collect { currentState ->
                 kotlinx.coroutines.delay(2000) // Debounce 2 seconds
                 if (currentState.shapes.isNotEmpty()) {
                     repository.saveShapes(currentState.shapes)
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
                        }
                        !isEraser && !isInvisibleColor && isValidSize
                    }
                    val shapeHit = org.example.project.utils.HitTestUtil.getShapeAt(visibleShapes, event.offset)
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
                    val currentShape = state.value.currentShape
                    if (currentShape != null) {
                        addToHistory(state.value.shapes) // Save state BEFORE adding new shape
                        
                        val finalShape = if (currentShape is DrawnShape.FreeHand) {
                            // Smooth the path
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
                     val bounds = org.example.project.utils.GeometryHelper.run { shape.getBounds() }
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
            state.value.currentColor
        }
        val strokeWidth = state.value.currentStrokeWidth
        val tempId = "temp_${kotlin.random.Random.nextInt()}" 

        val newShape: DrawnShape = if (isFreeHandTool(tool)) {
            currentFreeHandPoints.add(currentOffset)
            // For live preview, we use the raw points or simple path
            val path = Path().apply {
                if (currentFreeHandPoints.isNotEmpty()) {
                    moveTo(currentFreeHandPoints.first().x, currentFreeHandPoints.first().y)
                    for (i in 1 until currentFreeHandPoints.size) {
                        lineTo(currentFreeHandPoints[i].x, currentFreeHandPoints[i].y)
                    }
                }
            }
            DrawnShape.FreeHand(tempId, color, strokeWidth, tool, path, currentFreeHandPoints.toList())
        } else {
            // Geometric shapes
            val fixedStart = if (startOffset == Offset.Unspecified) currentOffset else startOffset
            DrawnShape.Geometric(tempId, color, strokeWidth, tool, fixedStart, currentOffset)
        }

        _state.update { it.copy(currentShape = newShape) }
    }

    private fun isFreeHandTool(tool: DrawingTool): Boolean {
        return when (tool) {
            DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.LASER_PEN, DrawingTool.ERASER -> true
            else -> false
        }
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

    private fun resizeSelectedShape(handle: org.example.project.utils.TransformHandle, worldDelta: Offset) {
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
                            org.example.project.utils.TransformHandle.RIGHT, 
                            org.example.project.utils.TransformHandle.TOP_RIGHT, 
                            org.example.project.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newEnd = newEnd.copy(x = newEnd.x + worldDelta.x)
                            }
                            org.example.project.utils.TransformHandle.LEFT,
                            org.example.project.utils.TransformHandle.TOP_LEFT,
                            org.example.project.utils.TransformHandle.BOTTOM_LEFT -> {
                                newStart = newStart.copy(x = newStart.x + worldDelta.x)
                            }
                            else -> {}
                        }
                        
                        when (handle) {
                            org.example.project.utils.TransformHandle.BOTTOM, 
                            org.example.project.utils.TransformHandle.BOTTOM_LEFT, 
                            org.example.project.utils.TransformHandle.BOTTOM_RIGHT -> {
                                newEnd = newEnd.copy(y = newEnd.y + worldDelta.y)
                            }
                            org.example.project.utils.TransformHandle.TOP,
                            org.example.project.utils.TransformHandle.TOP_LEFT,
                            org.example.project.utils.TransformHandle.TOP_RIGHT -> {
                                newStart = newStart.copy(y = newStart.y + worldDelta.y)
                            }
                            else -> {}
                        }
                        current.copy(start = newStart, end = newEnd)
                    }
                    is DrawnShape.FreeHand -> current // No resizing for Freehand yet
                    is DrawnShape.Text -> current // No resizing for Text yet
                }
            } else {
                current
            }
        }
        
        _state.update { it.copy(shapes = updatedShapes) }
    }
}
