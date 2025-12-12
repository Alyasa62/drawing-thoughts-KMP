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
import org.example.project.utils.PathSmoother

class WhiteBoardViewModel : ViewModel() {

    private val _state = MutableStateFlow(WhiteBoardState())
    val state = _state.asStateFlow()

    private val undoStack = mutableListOf<List<DrawnShape>>()
    private val redoStack = mutableListOf<List<DrawnShape>>()
    private val MAX_HISTORY_SIZE = 50

    // Temporary storage for freehand points before smoothing
    private var currentFreeHandPoints = mutableListOf<Offset>()
    
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
                    // --- SELECTION MODE ---
                    // 1. Try to find a handle if a shape is already selected
                    // (For MVP simplicity, skipping complex handle logic inside ViewModel, assumes dragging body moves)
                    
                    // 2. Or Select a new shape
                    val shapeHit = org.example.project.utils.HitTestUtil.getShapeAt(_state.value.shapes, event.offset)
                    _state.update { 
                        it.copy(
                            selectedShapeId = shapeHit?.id,
                            startingOffset = event.offset // Track start of drag
                        ) 
                    }
                } else {
                    // --- DRAWING MODE ---
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
                if (_state.value.selectedTool == DrawingTool.SELECTOR) {
                   updateSelectedShapePosition(event.offset)
                } else {
                   updateContinuingShape(event.offset)
                }
            }

            WhiteBoardEvent.FinishDrawing -> {
                 if (_state.value.selectedTool == DrawingTool.SELECTOR) {
                     // Commit the move to undo stack (Optional: Optimization to not save every frame)
                     // For now, if we dragged, we should save.
                     // A simple way is to check if startingOffset != null.
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

            // Properties
            is WhiteBoardEvent.OnStrokeWidthChange -> {
                _state.update { it.copy(currentStrokeWidth = event.width) }
            }
            is WhiteBoardEvent.OnColorChange -> {
                _state.update { it.copy(currentColor = event.color) }
            }
            is WhiteBoardEvent.OnBackgroundChange -> {
                _state.update { it.copy(canvasBackgroundColor = event.color) }
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
        val startDragString = state.value.startingOffset ?: return
        val selectedId = state.value.selectedShapeId ?: return
        
        // Calculate delta
        val deltaX = currentOffset.x - startDragString.x
        val deltaY = currentOffset.y - startDragString.y
        
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
        val color = state.value.currentColor
        val strokeWidth = state.value.currentStrokeWidth
        // Use a temporary ID for the currently drawing shape (0L which wont clash with DB IDs easily if logic holds, or random)
        // Ideally we should generate the ID here too, but 'currentShape' is transient.
        // We will assign a real ID when 'FinishDrawing' creates the final shape.
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
            DrawnShape.Geometric(tempId, color, strokeWidth, tool, startOffset, currentOffset)
        }

        _state.update { it.copy(currentShape = newShape) }
    }

    private fun isFreeHandTool(tool: DrawingTool): Boolean {
        return when (tool) {
            DrawingTool.PEN, DrawingTool.HIGHLIGHTER, DrawingTool.LASER_PEN, DrawingTool.ERASER -> true
            else -> false
        }
    }
}