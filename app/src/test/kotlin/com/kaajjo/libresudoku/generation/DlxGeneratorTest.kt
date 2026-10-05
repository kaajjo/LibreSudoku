package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.dlx.DlxGenerator
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class DlxGeneratorTest {

    @Test
    fun generate6x6_ProducesValidUniquePuzzles() =
        assertGeneratesValidPuzzles(GameType.Default6x6, targetEmpty = 18)

    @Test
    fun generate9x9_ProducesValidUniquePuzzles() =
        assertGeneratesValidPuzzles(GameType.Default9x9, targetEmpty = 45)

    @Test
    fun generate12x12_ProducesValidUniquePuzzles() =
        assertGeneratesValidPuzzles(GameType.Default12x12, targetEmpty = 85)

    @Test
    fun generate_TargetAboveBoardSize_StopsGracefully() {
        // 6x6 has only 36 cells; the digger must stop once nothing more can be removed
        val puzzle = DlxGenerator.generate(
            GameType.Default6x6,
            targetEmptyCells = 100,
            random = Random(7)
        )
        assertEquals(1, engineFor(GameType.Default6x6).solve(puzzle))

        val engine = engineFor(GameType.Default6x6)
        puzzle.indices.filter { puzzle[it] != 0 }.forEach { clueIndex ->
            val withoutClue = puzzle.clone().also { it[clueIndex] = 0 }
            assertEquals(
                "Removing clue $clueIndex must leave at least two solutions",
                2,
                engine.solve(withoutClue)
            )
        }
    }

    @Test
    fun generate_DifferentSeeds_ProduceDifferentPuzzles() {
        val a = DlxGenerator.generate(GameType.Default9x9, 45, random = Random(1))
        val b = DlxGenerator.generate(GameType.Default9x9, 45, random = Random(2))
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun generate_SameSeed_ProducesIdenticalPuzzle() {
        val a = DlxGenerator.generate(GameType.Default9x9, 45, random = Random(123))
        val b = DlxGenerator.generate(GameType.Default9x9, 45, random = Random(123))
        assertArrayEquals(a, b)
    }

    @Test
    fun generate9x9_ManySeeds_AlwaysUniqueAndValid() {
        repeat(40) { seed ->
            val puzzle = DlxGenerator.generate(
                GameType.Default9x9,
                targetEmptyCells = 45,
                random = Random(seed.toLong())
            )
            assertNoUnitConflicts(puzzle, GameType.Default9x9)

            val engine = engineFor(GameType.Default9x9, seed = seed.toLong())
            assertEquals("Seed $seed produced a non-unique puzzle", 1, engine.solve(puzzle))
        }
    }

    @Test
    fun generate_TopKBelowOne_Throws() {
        for (invalid in intArrayOf(0, -1)) {
            try {
                DlxGenerator.generate(GameType.Default9x9, 45, topK = invalid, random = Random(0))
                fail("Expected IllegalArgumentException for topK=$invalid")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun generate_NegativeTarget_Throws() {
        try {
            DlxGenerator.generate(GameType.Default6x6, -1, random = Random(0))
            fail("Expected a negative target to be rejected")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun generate_UnspecifiedType_Throws() {
        try {
            DlxGenerator.generate(GameType.Unspecified, 0, random = Random(0))
            fail("Expected an unspecified game type to be rejected")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun generate_ZeroTarget_ReturnsCompleteValidGrid() {
        val puzzle = DlxGenerator.generate(GameType.Default9x9, 0, Random(5))

        assertEquals(0, puzzle.count { it == 0 })
        assertValidSolvedBoard(puzzle, GameType.Default9x9)
        assertUniqueWithQQWing(puzzle, GameType.Default9x9)
    }

    @Test
    fun generate_TopKLargerThanBoard_IsSafelyClamped() {
        val puzzle = DlxGenerator.generate(
            GameType.Default6x6,
            targetEmptyCells = 12,
            random = Random(11),
            topK = Int.MAX_VALUE
        )

        assertEquals(12, puzzle.count { it == 0 })
        assertUniqueWithQQWing(puzzle, GameType.Default6x6)
    }

    private fun assertGeneratesValidPuzzles(type: GameType, targetEmpty: Int) {
        repeat(5) { attempt ->
            val puzzle = DlxGenerator.generate(type, targetEmpty, random = Random(1000L + attempt))

            assertEquals(type.size * type.size, puzzle.size)
            assertNoUnitConflicts(puzzle, type)
            val holes = puzzle.count { it == 0 }
            assertEquals("Unexpected number of holes", targetEmpty, holes)

            val engine = engineFor(type, seed = attempt.toLong())
            assertEquals("Puzzle must have exactly one solution", 1, engine.solve(puzzle))
            assertValidSolvedBoard(engine.solvedBoard, type)
            assertSolutionKeepsGivens(puzzle, engine.solvedBoard)
            assertUniqueWithQQWing(puzzle, type)
        }
    }

    private fun assertUniqueWithQQWing(puzzle: IntArray, type: GameType) {
        val qqWing = QQWing(type, GameDifficulty.Unspecified)
        assertTrue("QQWing rejected a generated puzzle", qqWing.setPuzzle(puzzle))
        assertEquals("QQWing found a non-unique puzzle", 1, qqWing.countSolutionsLimited())
    }
}
