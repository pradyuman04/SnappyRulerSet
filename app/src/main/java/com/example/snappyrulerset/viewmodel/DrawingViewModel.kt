package com.example.snappyrulerset.viewmodel

import android.graphics.PointF
import androidx.lifecycle.ViewModel
import com.example.snappyrulerset.tools.ToolType
import kotlin.math.hypot

// Base interface for all shapes
sealed interface Shape

data class LineShape(val start: PointF, val end: PointF) : Shape {
    fun length(): Float = hypot((start.x - end.x).toDouble(), (start.y - end.y).toDouble()).toFloat()
}

data class CompassShape(val center: PointF, val radius: Float) : Shape
data class ProtractorShape(val center: PointF, val radius: Float) : Shape

sealed interface DrawingAction {
    data class Add(val shape: Shape) : DrawingAction
    data class Remove(val shape: Shape) : DrawingAction
}

class DrawingViewModel : ViewModel() {

    // Backward-compatible: only line shapes for old tools
    var lines: List<LineShape> = emptyList()
        private set

    // Full shape list (for all tools)
    var shapes: List<Shape> = emptyList()
        private set

    private val undoStack = ArrayDeque<DrawingAction>()
    private val redoStack = ArrayDeque<DrawingAction>()
    private val maxHistory = 80

    var selectedTool: ToolType = ToolType.Pen
        private set

    fun setTool(t: ToolType) { selectedTool = t }

    // Add shape
    fun addShape(shape: Shape) {
        shapes = shapes + shape
        if (shape is LineShape) lines = lines + shape
        pushAction(DrawingAction.Add(shape))
    }

    // For backward compatibility
    fun addLine(line: LineShape) { addShape(line) }

    // Replace all shapes (for redraw)
    fun replaceAllShapes(newList: List<Shape>) {
        shapes = newList
        lines = newList.filterIsInstance<LineShape>()
    }

    fun replaceAllLines(newList: List<LineShape>) {
        lines = newList
        shapes = newList
    }

    private fun pushAction(action: DrawingAction) {
        undoStack.addLast(action)
        if (undoStack.size > maxHistory) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo() {
        val action = undoStack.removeLastOrNull() ?: return
        when (action) {
            is DrawingAction.Add -> {
                shapes = shapes - action.shape
                if (action.shape is LineShape) lines = lines - action.shape
                redoStack.addLast(action)
            }
            is DrawingAction.Remove -> {
                shapes = shapes + action.shape
                if (action.shape is LineShape) lines = lines + action.shape
                redoStack.addLast(action)
            }
        }
    }

    fun redo() {
        val action = redoStack.removeLastOrNull() ?: return
        when (action) {
            is DrawingAction.Add -> {
                shapes = shapes + action.shape
                if (action.shape is LineShape) lines = lines + action.shape
                undoStack.addLast(action)
            }
            is DrawingAction.Remove -> {
                shapes = shapes - action.shape
                if (action.shape is LineShape) lines = lines - action.shape
                undoStack.addLast(action)
            }
        }
    }
}
