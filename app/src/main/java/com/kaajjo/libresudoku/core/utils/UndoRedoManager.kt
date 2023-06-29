package com.kaajjo.libresudoku.core.utils

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note

class UndoRedoManager(private val initialState: GameState) {
    private var states: MutableList<GameState> = mutableListOf(initialState)
    private var currentState = 0

    fun addState(gameState: GameState) {
        if (gameState == states[currentState]) {
            return
        }

        val statesToDelete = mutableListOf<GameState>()
        for (i in states.indices - 1) {
            if (i > currentState) {
                statesToDelete.add(states[i])
            }
        }
        statesToDelete.forEach { item ->
            states.remove(item)
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