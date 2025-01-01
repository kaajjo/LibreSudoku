package com.kaajjo.libresudoku.core.utils

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note

class UndoRedoManager(private val initialState: GameState) {
    private var states: MutableList<GameState> = mutableListOf(initialState)
    private var currentState = 0

    /**
     * Adds a new game state to the history of states.
     * If the current state changes, all later states are removed.
     *
     * @param gameState The new game state to add.
     */
    fun addState(gameState: GameState) {
        if (currentState in states.indices && gameState == states[currentState]) {
            return
        }

        if (currentState < states.size - 1) {
            states = states.subList(0, currentState + 1).toMutableList()
        }

        states.add(gameState)
        currentState = states.size - 1
    }

    fun undo(): GameState {
        return if (canUndo()) {
            currentState -= 1
            states[currentState]
        } else {
            initialState
        }
    }

    fun redo(): GameState? {
        return if (canRedo()) {
            currentState += 1
            states[currentState]
        } else {
            null
        }
    }

    fun canRedo() = currentState < states.size - 1
    fun canUndo() = currentState > 0 && states.isNotEmpty()

    fun count() = states.count()
    fun clear() = states.clear()
}

data class GameState(
    val board: List<List<Cell>>,
    val notes: List<Note>
)