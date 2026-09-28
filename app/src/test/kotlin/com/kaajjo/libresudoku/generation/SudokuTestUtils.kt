package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.dlx.DlxEngine
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.random.Random

// Known-good boards, shared with QQWingTest and SudokuParserTest.
// PUZZLE_6X6 is verified to have exactly one solution: SOLUTION_6X6.
const val PUZZLE_6X6 = "500600000020053001100350040000001005"
const val SOLUTION_6X6 = "532614416523653241124356345162261435"
const val UNSOLVABLE_6X6 = "106020205001010602623100001250562010"
const val NON_UNIQUE_6X6 = "000000000020053001100350040000001005"
const val PUZZLE_9X9 =
    "000600000824753169000200000000500471000100386000400925000300000000900000000800000"
// The canonical well-formed 9x9 puzzle (Wikipedia), verified to have exactly one solution.
const val UNIQUE_9X9 =
    "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
const val PUZZLE_12X12 =
    "C00B6504710080000100000200502300089B0070000080B040600098105000000000090000B00000478300000000BC000000000500600800391062000027500630009610040B0000"

fun parseBoardString(board: String, type: GameType): IntArray =
    SudokuParser().parseBoard(board = board, gameType = type, emptySeparator = '0')
        .flatten()
        .map { it.value }
        .toIntArray()

internal fun engineFor(type: GameType, seed: Long = 42L): DlxEngine =
    DlxEngine(type.size, type.sectionHeight, type.sectionWidth, Random(seed))

/** Asserts that no two equal values share a row, column or box. Empty cells are ignored. */
fun assertNoUnitConflicts(board: IntArray, type: GameType) {
    val size = type.size
    assertEquals(size * size, board.size)

    fun assertUnit(values: List<Int>, description: String) {
        val seen = BooleanArray(size + 1)
        values.filter { it != 0 }.forEach { value ->
            assertTrue("$description contains value $value outside 1..$size", value in 1..size)
            assertFalse("$description contains duplicate value $value", seen[value])
            seen[value] = true
        }
    }

    for (row in 0 until size) {
        assertUnit((0 until size).map { col -> board[row * size + col] }, "Row $row")
    }
    for (col in 0 until size) {
        assertUnit((0 until size).map { row -> board[row * size + col] }, "Column $col")
    }
    for (boxRow in 0 until size step type.sectionHeight) {
        for (boxCol in 0 until size step type.sectionWidth) {
            val values = buildList {
                for (row in boxRow until boxRow + type.sectionHeight) {
                    for (col in boxCol until boxCol + type.sectionWidth) {
                        add(board[row * size + col])
                    }
                }
            }
            assertUnit(values, "Box at ($boxRow,$boxCol)")
        }
    }
}

fun assertValidSolvedBoard(board: IntArray, type: GameType) {
    assertEquals(type.size * type.size, board.size)
    for (v in board) assertTrue("Value $v is outside 1..${type.size}", v in 1..type.size)
    assertNoUnitConflicts(board, type)
}

fun assertSolutionKeepsGivens(puzzle: IntArray, solution: IntArray) {
    for (i in puzzle.indices) {
        if (puzzle[i] != 0) {
            assertEquals("Clue at cell $i changed in the solution", puzzle[i], solution[i])
        }
    }
}
