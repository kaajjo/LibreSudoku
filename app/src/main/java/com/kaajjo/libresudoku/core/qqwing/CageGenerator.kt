package com.kaajjo.libresudoku.core.qqwing

import android.util.Log
import com.kaajjo.libresudoku.core.Cell
import kotlin.random.Random

class CageGenerator(
    private val board: List<List<Cell>>,
    private val type: GameType
) {
    private var unusedCells: MutableList<Cell> = board.flatten().toMutableList()

    private fun generateCage(startCell: Cell, requiredSize: Int = 2, id: Int = 0): Cage? {
        if (unusedCells.isEmpty() || !unusedCells.contains(startCell)) {
            return null
        }
        unusedCells.remove(startCell)

        var cage = Cage(
            id = id,
            cells = listOf(startCell)
        )

        if(getUnusedNeighbors(startCell).isEmpty()) {
            return cage
        }

        while (cage.size() < requiredSize) {
            var neighbors = emptyList<Cell>()

            val cageCells = cage.cells.toMutableList()
            // searching for any cell with at lest 1 unused neighbor
            println("Searching neighbors")
            for (i in cageCells.indices) {
                val cell = cageCells.random()
                cageCells.remove(cell)
                neighbors = getUnusedNeighbors(cell)

                if (neighbors.isNotEmpty()) {
                    break
                }
            }

            if (neighbors.isEmpty()) {
                return cage
            }

            var cell: Cell? = null
            for(i in neighbors) {
                // select a random neighbor to add in a cage
                val tempCell = neighbors.random()
                if (cage.cells.all { it.value != tempCell.value } && unusedCells.contains(tempCell)) {
                    cell = tempCell
                }
            }
            if (cell != null) {
                unusedCells.remove(cell)
                cage = cage.copy(
                    cells = cage.cells + cell
                )
            } else {
                break
            }
        }

        return cage
    }

    fun generate(minSize: Int = 2, maxSize: Int = 6): List<Cage> {
        val cages = mutableListOf<Cage>()

        var id = 0
        while (unusedCells.isNotEmpty()) {
            val cage = generateCage(
                startCell = unusedCells.random(),
                requiredSize = Random.nextInt(minSize, maxSize + 1),
                id = id
            )
            if (cage != null) {
                cages.add(cage.copy(sum = cage.cells.sumOf { it.value }))
            } else {
                Log.d("KillerSudokuCageGen", "Couldn't generate cage")
            }
            id += 1
        }
        return cages.toList()
    }

    private fun getUnusedNeighbors(cell: Cell): List<Cell> {
        val neighbors = mutableListOf<Cell>()
        val row = cell.row
        val col = cell.col

        if (row > 0) {
            neighbors.add(board[row - 1][col])
        }
        if (col > 0) {
            neighbors.add(board[row][col - 1])
        }
        if (col < type.size - 1) {
            neighbors.add(board[row][col + 1])
        }
        if (row < type.size - 1) {
            neighbors.add(board[row + 1][col])
        }

        return neighbors
            .toList()
            .filter { unusedCells.contains(it) }
    }
}