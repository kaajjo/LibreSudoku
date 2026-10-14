package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.Cage

/**
 * Draws each Killer Sudoku cage with a dashed outline and a sum in its first cell's corner
 *
 * @param cages cages containing their cells and target sums
 * @param geometry board and cell dimensions in pixels
 * @param sumGlyphs painter configured for cage sum text
 * @param lineColor dashed outline color
 * @param sumColor cage sum text color
 * @param lineWidth dashed outline stroke width in pixels
 * @param questions whether to replace each sum with a question mark
 */
fun DrawScope.drawCages(
    cages: List<Cage>,
    geometry: BoardGeometry,
    sumGlyphs: GlyphPainter,
    lineColor: Color,
    sumColor: Color,
    lineWidth: Float,
    questions: Boolean
) {
    if (cages.isEmpty()) return
    val cellSize = geometry.cellSize
    val dashes = PathEffect.dashPathEffect(floatArrayOf(cellSize / 12f, cellSize / 12f))
    val sumPadding = cellSize * 0.05f

    cages.forEach { cage ->
        val cellWithSum = cage.cells.firstOrNull() ?: return@forEach
        val label = if (questions) "?" else cage.sum.toString()
        drawGlyphAt(
            painter = sumGlyphs,
            text = label,
            topLeft = geometry.cellTopLeft(cellWithSum.position) + Offset(sumPadding, sumPadding),
            color = sumColor
        )
        drawCageOutline(
            cage = cage,
            cellWithSum = cellWithSum,
            cellSize = cellSize,
            strokeWidth = lineWidth,
            color = lineColor,
            pathEffect = dashes,
            sumSize = sumGlyphs.inkBounds(label).size
        )
    }
}

private fun DrawScope.drawCageOutline(
    cage: Cage,
    cellWithSum: Cell,
    cellSize: Float,
    strokeWidth: Float,
    color: Color,
    pathEffect: PathEffect,
    sumSize: Size
) {
    val padding = cellSize * 0.1f
    val halfPadding = padding / 2f

    cage.cells.forEach { cell ->
        val xCell = cell.col * cellSize
        val yCell = cell.row * cellSize

        // the outline makes way for the sum in the corner
        val hasPaddingForText = cell == cellWithSum

        val hasNeighborLeft = hasNeighborLeft(cell, cage)
        val hasNeighborRight = hasNeighborRight(cell, cage)
        val hasNeighborTop = hasNeighborTop(cell, cage)
        val hasNeighborBottom = hasNeighborBottom(cell, cage)

        if (!hasNeighborLeft) {
            drawLine(
                color = color,
                start = Offset(
                    x = xCell + padding,
                    y = yCell + if (hasPaddingForText) (sumSize.height + padding) else if (hasNeighborTop) -halfPadding else padding
                ),
                end = Offset(
                    x = xCell + padding,
                    y = yCell + cellSize + if (hasNeighborBottom) halfPadding else -padding
                ),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }

        if (!hasNeighborRight) {
            drawLine(
                color = color,
                start = Offset(
                    x = xCell + cellSize - padding,
                    y = yCell + if (hasNeighborTop) -halfPadding else padding
                ),
                end = Offset(
                    x = xCell + cellSize - padding,
                    y = yCell + cellSize + if (hasNeighborBottom) halfPadding else -padding
                ),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
        if (!hasNeighborTop) {
            drawLine(
                color = color,
                start = Offset(
                    x = xCell + if (hasPaddingForText) (sumSize.width + padding) else if (hasNeighborLeft) -halfPadding else padding,
                    y = yCell + padding
                ),
                end = Offset(
                    x = xCell + cellSize + if (hasNeighborRight) halfPadding else -padding,
                    y = yCell + padding
                ),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }

        if (!hasNeighborBottom) {
            drawLine(
                color = color,
                start = Offset(
                    x = xCell + if (hasNeighborLeft) -halfPadding else padding,
                    y = yCell + cellSize - padding
                ),
                end = Offset(
                    x = xCell + cellSize + if (hasNeighborRight) halfPadding else -padding,
                    y = yCell + cellSize - padding
                ),
                strokeWidth = strokeWidth,
                pathEffect = pathEffect
            )
        }
    }
}

private fun hasNeighborLeft(cell: Cell, cage: Cage) =
    cage.cells.any { it.row == cell.row && it.col == cell.col - 1 }

private fun hasNeighborRight(cell: Cell, cage: Cage) =
    cage.cells.any { it.row == cell.row && it.col == cell.col + 1 }

private fun hasNeighborTop(cell: Cell, cage: Cage) =
    cage.cells.any { it.row == cell.row - 1 && it.col == cell.col }

private fun hasNeighborBottom(cell: Cell, cage: Cage) =
    cage.cells.any { it.row == cell.row + 1 && it.col == cell.col }
