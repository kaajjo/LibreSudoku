package com.kaajjo.libresudoku.core.generator.dlx

import com.kaajjo.libresudoku.core.qqwing.GameType
import kotlin.random.Random

/**
 * Generates Sudoku puzzles by computing a random complete grid and then removing clues while the
 * solution remains unique
 */
object DlxGenerator {

    /**
     * Generates a puzzle by producing a random complete grid and then removing clues for as long as
     * the solution stays unique.
     *
     * Removal is best-effort: a cell is permanently skipped if clearing it introduces a second
     * solution, so the final number of empty cells may be lower than [targetEmptyCells].
     *
     * @param type the variant whose dimensions and box shape define the constraint matrix
     * @param targetEmptyCells the desired number of empty cells (an upper bound, not a guarantee)
     * @param random the randomness source; pass `Random(seed)` for reproducibility, or leave the
     *   default for variety across calls
     * @return the puzzle as a row-major grid, with 0 for empty cells and 1..N for clues
     */
    fun generate(
        type: GameType,
        targetEmptyCells: Int,
        random: Random = Random.Default
    ): IntArray {
        val boardSize = getBoardSize(type)
        val gridSizeRow = getGridSizeRow(type)
        val gridSizeCol = getGridSizeCol(type)
        val totalCells = boardSize * boardSize

        // use a single engine instance: the matrix is built once and reused for both full generation
        // and uniqueness checks
        val engine = DlxEngine(boardSize, gridSizeRow, gridSizeCol, random)

        // generate a random full solution (empty board + randomized matrix).
        val emptyBoard = IntArray(totalCells)
        engine.solve(emptyBoard, limitToTwo = true)
        val currentBoard = engine.solvedBoard.clone()

        // dig holes in a random order while ensuring the solution remains unique
        val positions = IntArray(totalCells) { it }
        positions.shuffle(random)

        var removedCount = 0
        for (pos in positions) {
            if (removedCount >= targetEmptyCells) break

            val backupVal = currentBoard[pos]
            currentBoard[pos] = 0

            // the engine resets its own state and rewinds clues on every solve().
            if (engine.solve(currentBoard, limitToTwo = true) != 1) {
                currentBoard[pos] = backupVal
            } else {
                removedCount++
            }
        }

        return currentBoard
    }

    /**
     * Returns the grid side length for the given variant.
     *
     * @param type the Sudoku variant
     * @return the board size N (sectionHeight × sectionWidth)
     */
    private fun getBoardSize(type: GameType): Int = type.sectionHeight * type.sectionWidth

    /**
     * Returns the box height in cells for the given variant.
     *
     * @param type the Sudoku variant
     * @return the box height (sectionHeight)
     */
    private fun getGridSizeRow(type: GameType): Int = type.sectionHeight

    /**
     * Returns the box width in cells for the given variant.
     *
     * @param type the Sudoku variant
     * @return the box width (sectionWidth)
     */
    private fun getGridSizeCol(type: GameType): Int = type.sectionWidth
}
