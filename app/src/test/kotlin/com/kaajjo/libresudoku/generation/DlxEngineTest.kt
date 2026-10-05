package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.dlx.DlxEngine
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class DlxEngineTest {

    @Test
    fun solve6x6_KnownPuzzle_UniqueAndMatchesKnownSolution() {
        val puzzle = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        val expected = parseBoardString(SOLUTION_6X6, GameType.Default6x6)
        val engine = engineFor(GameType.Default6x6)

        assertEquals(1, engine.solve(puzzle))
        assertArrayEquals(expected, engine.solvedBoard)
    }

    @Test
    fun solve6x6_UnsolvablePuzzle_ReturnsZero() {
        val puzzle = parseBoardString(UNSOLVABLE_6X6, GameType.Default6x6)
        assertEquals(0, engineFor(GameType.Default6x6).solve(puzzle))
    }

    @Test
    fun solve6x6_NonUniquePuzzle_ReturnsTwo() {
        val puzzle = parseBoardString(NON_UNIQUE_6X6, GameType.Default6x6)
        assertEquals(2, engineFor(GameType.Default6x6).solve(puzzle))
    }

    @Test
    fun solve9x9_AgreesWithQQWing() = assertAgreesWithQQWing(PUZZLE_9X9, GameType.Default9x9)

    @Test
    fun solve12x12_AgreesWithQQWing() = assertAgreesWithQQWing(PUZZLE_12X12, GameType.Default12x12)

    @Test
    fun engineReuse_RepeatedSolvesGiveIdenticalResults() {
        val engine = engineFor(GameType.Default6x6)
        val puzzle = parseBoardString(PUZZLE_6X6, GameType.Default6x6)

        val firstCount = engine.solve(puzzle)
        val firstBoard = engine.solvedBoard.clone()

        // interleave other solves to stress the cover/uncover rollback
        engine.solve(parseBoardString(NON_UNIQUE_6X6, GameType.Default6x6))
        engine.solve(IntArray(36), maxSolutions = 1)

        assertEquals(firstCount, engine.solve(puzzle))
        assertArrayEquals(firstBoard, engine.solvedBoard)
    }

    @Test
    fun solve_ConflictingClues_ThrowsAndKeepsEngineUsable() {
        val engine = engineFor(GameType.Default9x9)
        val puzzle = parseBoardString(PUZZLE_9X9, GameType.Default9x9)
        val expectedCount = engine.solve(puzzle)
        val expectedBoard = engine.solvedBoard.clone()

        val conflicting = IntArray(81)
        conflicting[0] = 5
        conflicting[3] = 5 // duplicate in row 0
        try {
            engine.solve(conflicting)
            fail("Expected IllegalArgumentException for conflicting clues")
        } catch (expected: IllegalArgumentException) {
        }

        // the rollback must leave the matrix intact, so the same engine solves as before
        assertEquals(expectedCount, engine.solve(puzzle))
        assertArrayEquals(expectedBoard, engine.solvedBoard)
    }

    @Test
    fun solve_OutOfRangeValue_Throws() {
        val board = IntArray(36)
        board[0] = 7
        try {
            engineFor(GameType.Default6x6).solve(board)
            fail("Expected IllegalArgumentException for an out-of-range value")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun solve_NegativeValue_Throws() {
        val board = IntArray(36)
        board[0] = -1
        try {
            engineFor(GameType.Default6x6).solve(board)
            fail("Expected IllegalArgumentException for a negative value")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun solve_MaxValueClue_Accepted() {
        val board = IntArray(36)
        board[0] = 6
        assertEquals(2, engineFor(GameType.Default6x6).solve(board))
    }

    @Test
    fun solve_MaxSolutionsOne_StopsAtOneForNonUniquePuzzle() {
        val engine = engineFor(GameType.Default6x6)
        val puzzle = parseBoardString(NON_UNIQUE_6X6, GameType.Default6x6)

        assertEquals("Puzzle is expected to be non-unique", 2, engine.solve(puzzle))

        assertEquals(1, engine.solve(puzzle, maxSolutions = 1))
        assertValidSolvedBoard(engine.solvedBoard, GameType.Default6x6)
        assertSolutionKeepsGivens(puzzle, engine.solvedBoard)
    }

    @Test
    fun solve_MaxSolutionsBelowOne_Throws() {
        val engine = engineFor(GameType.Default6x6)
        for (invalid in intArrayOf(0, -1)) {
            try {
                engine.solve(IntArray(36), maxSolutions = invalid)
                fail("Expected IllegalArgumentException for maxSolutions=$invalid")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun solve_WrongBoardLength_Throws() {
        val engine = engineFor(GameType.Default6x6)
        for (badLength in intArrayOf(0, 35, 37, 81)) {
            try {
                engine.solve(IntArray(badLength))
                fail("Expected IllegalArgumentException for a board of length $badLength")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun solve_FullySolvedBoard_ReturnsOneAndEchoesInput() {
        val solved = parseBoardString(SOLUTION_6X6, GameType.Default6x6)
        val engine = engineFor(GameType.Default6x6)

        assertEquals(1, engine.solve(solved))
        assertArrayEquals(solved, engine.solvedBoard)
    }

    @Test
    fun solve_ConflictingCluesInColumn_ThrowsAndKeepsEngineUsable() {
        val conflicting = IntArray(81)
        conflicting[0] = 5
        conflicting[9] = 5 // same column 0, rows 0 and 1
        assertConflictRejectedAndEngineUsable(conflicting)
    }

    @Test
    fun solve_ConflictingCluesInBox_ThrowsAndKeepsEngineUsable() {
        val conflicting = IntArray(81)
        conflicting[0] = 5
        conflicting[10] = 5 // same top-left box, different row and column
        assertConflictRejectedAndEngineUsable(conflicting)
    }

    @Test
    fun solve9x9_UniquePuzzle_MatchesQQWingSolution() {
        val puzzle = parseBoardString(UNIQUE_9X9, GameType.Default9x9)
        val engine = engineFor(GameType.Default9x9)

        // both solvers can only agree on the grid when the puzzle is uniquely solvable
        assertEquals("Puzzle is expected to be uniquely solvable", 1, engine.solve(puzzle))

        val qqwing = QQWing(GameType.Default9x9, GameDifficulty.Unspecified)
        qqwing.setPuzzle(puzzle)
        assertTrue("QQWing must solve the puzzle", qqwing.solve())

        assertArrayEquals(
            "DLX and QQWing must agree on the unique solution",
            qqwing.solution,
            engine.solvedBoard
        )
        assertValidSolvedBoard(engine.solvedBoard, GameType.Default9x9)
    }

    @Test
    fun solve4x4_EmptyBoard_HasExactly288SolutionsAndEngineIsReusable() {
        val engine = DlxEngine(4, 2, 2, Random(7))

        assertEquals(288, engine.solve(IntArray(16), maxSolutions = 300))
        assertEquals(288, engine.solutionCount)
        assertValidSquareSolution(engine.solvedBoard, size = 4, boxHeight = 2, boxWidth = 2)

        assertEquals(288, engine.solve(IntArray(16), maxSolutions = 300))
        assertEquals(288, engine.solutionCount)
    }

    @Test
    fun solve4x4_MaxSolutionsThree_StopsAtThree() {
        val engine = DlxEngine(4, 2, 2, Random(8))

        assertEquals(3, engine.solve(IntArray(16), maxSolutions = 3))
        assertEquals(3, engine.solutionCount)
        assertValidSquareSolution(engine.solvedBoard, size = 4, boxHeight = 2, boxWidth = 2)
    }

    @Test
    fun solve_Random4x4Boards_AgreesWithIndependentBacktrackingSolver() {
        val random = Random(20260820)
        val engine = DlxEngine(4, 2, 2, Random(99))

        repeat(150) { caseIndex ->
            val board = randomLocallyValid4x4Board(random)
            val expected = countWithBacktracking(board, limit = 2)
            val inputSnapshot = board.clone()

            assertEquals(
                "Different capped solution count for case $caseIndex: ${board.joinToString()}",
                expected,
                engine.solve(board, maxSolutions = 2)
            )
            assertArrayEquals("solve() must not mutate its input", inputSnapshot, board)

            if (expected > 0) {
                assertValidSquareSolution(
                    engine.solvedBoard,
                    size = 4,
                    boxHeight = 2,
                    boxWidth = 2
                )
                assertSolutionKeepsGivens(board, engine.solvedBoard)
            } else {
                assertArrayEquals(IntArray(16), engine.solvedBoard)
            }
        }
    }

    @Test
    fun solve_UnsolvableBoard_ClearsSolutionAndKeepsEngineUsable() {
        val engine = engineFor(GameType.Default6x6)
        val solvable = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        val expected = parseBoardString(SOLUTION_6X6, GameType.Default6x6)

        assertEquals(1, engine.solve(solvable))
        assertArrayEquals(expected, engine.solvedBoard)
        assertEquals(0, engine.solve(parseBoardString(UNSOLVABLE_6X6, GameType.Default6x6)))
        assertArrayEquals(IntArray(36), engine.solvedBoard)
        assertEquals(1, engine.solve(solvable))
        assertArrayEquals(expected, engine.solvedBoard)
    }

    @Test
    fun solve_LateInvalidValue_RollsBackAllEarlierGivens() {
        val invalid = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        invalid[invalid.lastIndex] = 7
        assertRejectedAnd6x6EngineReusable(invalid)
    }

    @Test
    fun solve_LateConflictingClue_RollsBackAllEarlierGivens() {
        val conflicting = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        conflicting[conflicting.lastIndex] = 1 // row 5 already contains 1 at column 2
        assertRejectedAnd6x6EngineReusable(conflicting)
    }

    @Test
    fun solvedBoard_ReturnsDefensiveSnapshot() {
        val engine = engineFor(GameType.Default6x6)
        val puzzle = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        val expected = parseBoardString(SOLUTION_6X6, GameType.Default6x6)

        assertEquals(1, engine.solve(puzzle))
        engine.solvedBoard.fill(0)

        assertArrayEquals(expected, engine.solvedBoard)
    }

    @Test
    fun constructor_InvalidGeometry_ThrowsIllegalArgumentException() {
        val invalidDimensions = listOf(
            intArrayOf(0, 1, 1),
            intArrayOf(-1, 1, 1),
            intArrayOf(4, 0, 4),
            intArrayOf(4, 2, 3),
            intArrayOf(9, 3, 4),
            intArrayOf(26, 2, 13)
        )

        invalidDimensions.forEach { (size, boxHeight, boxWidth) ->
            try {
                DlxEngine(size, boxHeight, boxWidth, Random(0))
                fail("Expected invalid geometry $size/$boxHeight/$boxWidth to be rejected")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    /**
     * Solves a reference puzzle, asserts [conflicting] is rejected, then re-solves the reference to
     * prove the cover/uncover rollback left the shared matrix intact.
     */
    private fun assertConflictRejectedAndEngineUsable(conflicting: IntArray) {
        val engine = engineFor(GameType.Default9x9)
        val reference = parseBoardString(PUZZLE_9X9, GameType.Default9x9)
        val expectedCount = engine.solve(reference)
        val expectedBoard = engine.solvedBoard.clone()

        try {
            engine.solve(conflicting)
            fail("Expected IllegalArgumentException for conflicting clues")
        } catch (expected: IllegalArgumentException) {
        }

        assertEquals(expectedCount, engine.solve(reference))
        assertArrayEquals(expectedBoard, engine.solvedBoard)
    }

    private fun assertRejectedAnd6x6EngineReusable(invalid: IntArray) {
        val engine = engineFor(GameType.Default6x6)
        val reference = parseBoardString(PUZZLE_6X6, GameType.Default6x6)
        val expected = parseBoardString(SOLUTION_6X6, GameType.Default6x6)

        try {
            engine.solve(invalid)
            fail("Expected invalid board to be rejected")
        } catch (expected: IllegalArgumentException) {
        }

        assertEquals(1, engine.solve(reference))
        assertArrayEquals(expected, engine.solvedBoard)
    }

    private fun assertAgreesWithQQWing(boardString: String, type: GameType) {
        val puzzle = parseBoardString(boardString, type)
        val engine = engineFor(type)
        val solutions = engine.solve(puzzle)

        val qqwing = QQWing(type, GameDifficulty.Unspecified)
        qqwing.setPuzzle(puzzle)
        assertEquals("DLX and QQWing must agree on solvability", qqwing.solve(), solutions > 0)

        if (solutions > 0) {
            assertValidSolvedBoard(engine.solvedBoard, type)
            assertSolutionKeepsGivens(puzzle, engine.solvedBoard)
        }
    }

    private fun randomLocallyValid4x4Board(random: Random): IntArray {
        val board = IntArray(16)
        val positions = IntArray(16) { it }
        positions.shuffle(random)

        for (position in positions.take(random.nextInt(0, 11))) {
            val values = (1..4).shuffled(random)
            val value = values.firstOrNull { canPlace4x4(board, position, it) }
            if (value != null) board[position] = value
        }
        return board
    }

    private fun countWithBacktracking(initial: IntArray, limit: Int): Int {
        val board = initial.clone()

        fun search(): Int {
            var bestPosition = -1
            var bestValues = emptyList<Int>()

            for (position in board.indices) {
                if (board[position] != 0) continue
                val values = (1..4).filter { canPlace4x4(board, position, it) }
                if (values.isEmpty()) return 0
                if (bestPosition == -1 || values.size < bestValues.size) {
                    bestPosition = position
                    bestValues = values
                }
            }

            if (bestPosition == -1) return 1

            var solutions = 0
            for (value in bestValues) {
                board[bestPosition] = value
                solutions += search()
                board[bestPosition] = 0
                if (solutions >= limit) return limit
            }
            return solutions
        }

        return search()
    }

    private fun canPlace4x4(board: IntArray, position: Int, value: Int): Boolean {
        val row = position / 4
        val col = position % 4
        for (i in 0 until 4) {
            if (board[row * 4 + i] == value || board[i * 4 + col] == value) return false
        }

        val boxRow = row / 2 * 2
        val boxCol = col / 2 * 2
        for (r in boxRow until boxRow + 2) {
            for (c in boxCol until boxCol + 2) {
                if (board[r * 4 + c] == value) return false
            }
        }
        return true
    }

    private fun assertValidSquareSolution(
        board: IntArray,
        size: Int,
        boxHeight: Int,
        boxWidth: Int
    ) {
        assertEquals(size * size, board.size)
        val expected = (1..size).toSet()

        for (row in 0 until size) {
            assertEquals(expected, (0 until size).map { col -> board[row * size + col] }.toSet())
        }
        for (col in 0 until size) {
            assertEquals(expected, (0 until size).map { row -> board[row * size + col] }.toSet())
        }
        for (boxRow in 0 until size step boxHeight) {
            for (boxCol in 0 until size step boxWidth) {
                val values = buildSet {
                    for (row in boxRow until boxRow + boxHeight) {
                        for (col in boxCol until boxCol + boxWidth) {
                            add(board[row * size + col])
                        }
                    }
                }
                assertEquals(expected, values)
            }
        }
    }
}
