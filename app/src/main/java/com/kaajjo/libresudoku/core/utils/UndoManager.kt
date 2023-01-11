package com.kaajjo.libresudoku.core.utils

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note

// TODO REFACTOR
class UndoManager(private val initState: GameState) {
    private var states: List<GameState> = mutableListOf(initState)
    private var undone = false

    fun addState(gameState: GameState) {
        states = states.plus(gameState)
        undone = false
        if (states.count() > 50) {
            states = states.takeLast(50)
        }
    }

    fun getPrevState(): GameState {
        if (states.count() <= 1) {
            return initState
        }
        if (!undone) {
            states = states.dropLast(1)
        }
        val state = states.last()
        undone = true
        states = states.dropLast(1)
        return state
    }

    fun count(): Int = states.count()
    fun clear() {
        states = emptyList()
    }
}

class GameState(
    val board: List<List<Cell>>,
    val notes: List<Note>
)