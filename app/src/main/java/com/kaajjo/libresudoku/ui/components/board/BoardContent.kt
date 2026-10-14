package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.runtime.Immutable
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note

/**
 * A digit written in a cell
 *
 * @property position zero-based cell position
 * @property value nonzero digit value stored in the cell
 */
@Immutable
data class Digit(val position: CellPosition, val value: Int)

/**
 * Content and highlights tracked by the board animations
 *
 * Set differences identify which items should enter or leave the board
 *
 * @property gameSize number of rows and columns
 * @property digits nonzero values paired with their cell positions
 * @property notes pencil marks requested for display
 * @property errors filled cell positions whose errors should be highlighted
 * @property selected selected cell position, or null when no board cell is selected
 * @property sameNumberCells filled cells matching the selected value when highlighting is enabled
 * @property hintCells cell positions highlighted by a hint or tutorial
 * @property highlightedNotes pencil marks present in [notes] that should receive a highlight
 */
@Immutable
data class BoardContent(
    val gameSize: Int,
    val digits: Set<Digit>,
    val notes: Set<Note>,
    val errors: Set<CellPosition>,
    val selected: CellPosition?,
    val sameNumberCells: Set<CellPosition>,
    val hintCells: Set<CellPosition>,
    val highlightedNotes: Set<Note>
) {
    companion object {
        /**
         * Creates content with no digits, notes, selection or highlights
         *
         * @param gameSize number of rows and columns in the target board
         */
        fun empty(gameSize: Int) = BoardContent(
            gameSize = gameSize,
            digits = emptySet(),
            notes = emptySet(),
            errors = emptySet(),
            selected = null,
            sameNumberCells = emptySet(),
            hintCells = emptySet(),
            highlightedNotes = emptySet()
        )
    }
}

/**
 * Converts board data and display options into sets used by the animations
 *
 * @param board cells with their values and error flags
 * @param gameSize number of rows and columns in the board
 * @param notes pencil marks requested for display after visibility options have been applied
 * @param selectedCell selected cell, or a cell with negative coordinates in digit-first input mode
 * @param highlightSameNumbers whether filled cells matching the selected value are highlighted
 * @param highlightErrors whether filled cells marked as errors are highlighted
 * @param cellsToHighlight cells highlighted by a hint or tutorial
 * @param notesToHighlight requested note highlights, restricted to notes present in [notes]
 */
fun boardContent(
    board: List<List<Cell>>,
    gameSize: Int,
    notes: List<Note>,
    selectedCell: Cell,
    highlightSameNumbers: Boolean,
    highlightErrors: Boolean,
    cellsToHighlight: List<Cell>,
    notesToHighlight: List<Note>
): BoardContent {
    val filledCells = board.flatten().filter { it.value != 0 }
    val noteSet = notes.toHashSet()
    val selectedValue = selectedCell.value

    return BoardContent(
        gameSize = gameSize,
        digits = filledCells.mapTo(HashSet()) { Digit(it.position, it.value) },
        notes = noteSet,
        errors = if (highlightErrors) {
            filledCells.filter { it.error }.mapTo(HashSet()) { it.position }
        } else {
            emptySet()
        },
        selected = selectedCell.position.takeIf { selectedCell.row >= 0 && selectedCell.col >= 0 },
        sameNumberCells = if (highlightSameNumbers && selectedValue != 0) {
            filledCells.filter { it.value == selectedValue }.mapTo(HashSet()) { it.position }
        } else {
            emptySet()
        },
        hintCells = cellsToHighlight.mapTo(HashSet()) { it.position },
        highlightedNotes = notesToHighlight.filterTo(HashSet()) { it in noteSet }
    )
}
