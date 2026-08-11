package com.pecmi.studio.editor

import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.Layer
import java.util.ArrayDeque

data class HistoryState(
    val layers: List<Layer>,
    val settings: CanvasSettings
)

class HistoryManager(private val maxHistory: Int = 30) {
    private val undoStack = ArrayDeque<HistoryState>()
    private val redoStack = ArrayDeque<HistoryState>()

    fun pushState(layers: List<Layer>, settings: CanvasSettings) {
        undoStack.push(HistoryState(layers, settings))
        if (undoStack.size > maxHistory) {
            undoStack.removeLast()
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.size > 1

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(): HistoryState? {
        if (!canUndo()) return null
        val currentState = undoStack.pop()
        redoStack.push(currentState)
        return undoStack.peek()
    }

    fun redo(): HistoryState? {
        if (!canRedo()) return null
        val nextState = redoStack.pop()
        undoStack.push(nextState)
        return nextState
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
