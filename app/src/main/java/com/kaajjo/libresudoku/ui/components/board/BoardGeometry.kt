package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import kotlin.math.floor
import kotlin.math.min

/**
 * Stable position key for content drawn inside a cell
 *
 * @property row zero-based row index
 * @property col zero-based column index
 */
@Immutable
data class CellPosition(val row: Int, val col: Int)

val Cell.position get() = CellPosition(row, col)
val Note.position get() = CellPosition(row, col)

/**
 * Pixel geometry of cells and boxes derived from the current board dimensions
 *
 * @property gameSize number of rows and columns, supported values are 6, 9 and 12
 * @property boardSize side length of the unzoomed board in pixels
 */
@Immutable
class BoardGeometry(val gameSize: Int, val boardSize: Float) {
    val cellSize = boardSize / gameSize

    val boxWidth = getSectionWidthForSize(gameSize)
    val boxHeight = getSectionHeightForSize(gameSize)

    /**
     * Returns the top-left corner of a cell in unzoomed board pixels
     *
     * @param cell zero-based position of the cell
     */
    fun cellTopLeft(cell: CellPosition) = Offset(cell.col * cellSize, cell.row * cellSize)

    /**
     * Returns the center of a cell in unzoomed board pixels
     *
     * @param cell zero-based position of the cell
     */
    fun cellCenter(cell: CellPosition) =
        Offset((cell.col + 0.5f) * cellSize, (cell.row + 0.5f) * cellSize)

    /**
     * Finds the cell under a point, clamping points outside the board to its nearest edge cell
     *
     * @param position touch position in unzoomed board pixels
     * @return zero-based cell position within the board
     */
    fun cellAt(position: Offset) = CellPosition(
        row = floor(position.y / cellSize).toInt().coerceIn(0, gameSize - 1),
        col = floor(position.x / cellSize).toInt().coerceIn(0, gameSize - 1)
    )
}

/**
 * Positions pencil marks in a grid that mirrors the box dimensions of the board
 *
 * @property geometry unzoomed cell dimensions and box layout
 * @param topReserve additional space reserved above notes in every cell for cage sums, in pixels
 */
@Immutable
class NoteLayout(private val geometry: BoardGeometry, topReserve: Float) {
    private val columns = geometry.boxWidth
    private val rows = geometry.boxHeight

    // Notes are pulled a bit towards the center of the cell, so they don't stick to the grid lines
    private val insetX = geometry.cellSize * 0.075f
    private val insetTop = geometry.cellSize * 0.04f + topReserve
    private val insetBottom = geometry.cellSize * 0.04f

    private val slotWidth = (geometry.cellSize - 2 * insetX) / columns
    private val slotHeight = (geometry.cellSize - insetTop - insetBottom) / rows

    val highlightRadius = min(slotWidth, slotHeight) * 0.52f

    /**
     * Returns the center of a pencil mark's slot in unzoomed board pixels
     *
     * @param note cell position and candidate value from 1 through the board size
     */
    fun center(note: Note): Offset {
        val index = note.value - 1
        val cellTopLeft = geometry.cellTopLeft(note.position)
        return Offset(
            x = cellTopLeft.x + insetX + (index % columns + 0.5f) * slotWidth,
            y = cellTopLeft.y + insetTop + (index / columns + 0.5f) * slotHeight
        )
    }
}
