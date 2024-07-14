package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.Cage

fun DrawScope.drawKillerCage(
    cage: Cage,
    cellWithSum: Cell,
    cellSize: Float,
    strokeWidth: Float,
    color: Color,
    cornerTextPadding: Offset
) {
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellSize / 12f, cellSize / 12f))
    val padding = cellSize * 0.1f

    cage.cells.forEach { cell ->
        val halfPadding = padding / 2f
        val xCell = cell.col * cellSize
        val yCell = cell.row * cellSize

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
                    y = yCell + if (hasPaddingForText) (cornerTextPadding.y + padding) else if (hasNeighborTop) -halfPadding else padding
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
                    x = xCell + if (hasPaddingForText) (cornerTextPadding.x + padding) else if (hasNeighborLeft) -halfPadding else padding,
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
