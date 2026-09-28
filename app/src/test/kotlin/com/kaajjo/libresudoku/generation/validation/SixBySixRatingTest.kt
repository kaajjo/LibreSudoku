package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.*
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import org.junit.Assert.*
import org.junit.Test

class SixBySixRatingTest {
    private val geometry = SudokuGeometry(6, 2, 3)

    private data class Example(
        val puzzle: String,
        val floor: LogicTier,
        val difficulty: GameDifficulty,
        val score: Int
    )

    // Fixed witnesses from the 2026-09-15 audit, including the previously unavailable Moderate.
    private val examples = listOf(
        Example("003054514263240531001026035602160300", LogicTier.NAKED_SINGLES, GameDifficulty.Easy, 48),
        Example("263040400326012000300060030600020004", LogicTier.SINGLES, GameDifficulty.Moderate, 100),
        Example("620030000040010000002004100000003006", LogicTier.BASIC, GameDifficulty.Moderate, 224),
        Example("410005000010300006042000104052020000", LogicTier.ADVANCED, GameDifficulty.Hard, 412),
        Example("006001100040600000005004060210200000", LogicTier.CHAINS, GameDifficulty.Challenge, 740)
    )

    @Test
    fun auditedExamplesUseActualTechniquesWithoutAddingPassesOrChangingScores() {
        val compactAdapter = ClassicDifficultyAdapter()
        val capturedAdapter = ClassicDifficultyAdapter(captureSteps = true)
        for (example in examples) {
            val puzzle = example.puzzle.map { it.digitToInt() }.toIntArray()
            val solutions = IndependentOracle(geometry).solve(puzzle, limit = 2)
            assertEquals("Fixture must be unique: ${example.puzzle}", 1, solutions.size)
            val compact = compactAdapter.evaluateKnownUnique(GameType.Default6x6, puzzle)
            val captured = capturedAdapter.evaluateKnownUnique(GameType.Default6x6, puzzle)
            val compactRating = compact.rating as LogicalRating.Rated
            val capturedRating = captured.rating as LogicalRating.Rated
            val report = compactRating.report
            assertEquals(example.difficulty, compact.difficulty)
            assertEquals(example.floor, report.techniqueFloor)
            assertEquals(example.score, report.effortScore)
            assertEquals(example.score, compact.effortScore)
            assertEquals(RatingPolicy.identifier(geometry), compact.scorePolicy)
            assertArrayEquals(solutions.single(), report.solutionSnapshot())
            assertTrue(report.steps.isEmpty())
            assertEquals(captured.difficulty, compact.difficulty)
            assertEquals(capturedRating.lowerTiersStalled, compactRating.lowerTiersStalled)
            assertEquals(capturedRating.report.operations, report.operations)
            assertEquals(capturedRating.report.counts(), report.counts())
            assertEquals(capturedRating.report.steps.sumOf { RatingPolicy.weight(it.technique) }, report.effortScore)
            if (example.floor in listOf(LogicTier.NAKED_SINGLES, LogicTier.SINGLES)) {
                // Both solve in the SINGLES pass: its name cannot decide between Easy and Moderate.
                assertEquals(LogicTier.SINGLES, report.tier)
                assertTrue(compactRating.lowerTiersStalled.isEmpty())
            }
        }
    }

    @Test
    fun fullHouseRemainsEasyAndSimpleIsStillOptional() {
        val solved = IntArray(36) { cell ->
            val row = cell / 6
            (row * 3 + row / 2 + cell % 6) % 6 + 1
        }
        val puzzle = solved.copyOf().also { it[0] = 0 }
        val ordinary = ClassicDifficultyAdapter(captureSteps = true)
            .evaluateKnownUnique(GameType.Default6x6, puzzle)
        assertEquals(GameDifficulty.Easy, ordinary.difficulty)
        assertEquals(Technique.FULL_HOUSE,
            (ordinary.rating as LogicalRating.Rated).report.steps.single().technique)
        assertEquals(GameDifficulty.Simple, ClassicDifficultyAdapter(exposeSimple = true)
            .evaluateKnownUnique(GameType.Default6x6, puzzle).difficulty)
        val hiddenSingles = examples[1].puzzle.map { it.digitToInt() }.toIntArray()
        assertEquals(GameDifficulty.Easy, ClassicDifficultyAdapter(exposeSimple = true)
            .evaluateKnownUnique(GameType.Default6x6, hiddenSingles).difficulty)
    }

    @Test
    fun optionalFiveLabelPolicyKeepsEveryCategoryReachable() {
        val adapter = ClassicDifficultyAdapter(exposeSimple = true)
        val expected = listOf(GameDifficulty.Simple, GameDifficulty.Easy, GameDifficulty.Moderate,
            GameDifficulty.Hard, GameDifficulty.Challenge)
        for ((example, difficulty) in examples.zip(expected)) {
            val puzzle = example.puzzle.map { it.digitToInt() }.toIntArray()
            val assessment = adapter.evaluateKnownUnique(GameType.Default6x6, puzzle)
            val report = (assessment.rating as LogicalRating.Rated).report
            assertEquals(difficulty, assessment.difficulty)
            assertEquals(example.floor, report.techniqueFloor)
            assertEquals(example.score, assessment.effortScore)
            for (score in listOf(0, 801, 1001, 1601, 100_000)) {
                assertEquals(difficulty,
                    RatingPolicy.difficulty(geometry, example.floor, score, exposeSimple = true))
            }
        }
    }

    @Test
    fun sixBySixCategoriesIgnoreUncalibratedScoreThresholds() {
        for (score in listOf(0, 800, 801, 1000, 1001, 1600, 1601, 100_000)) {
            for (example in examples) {
                assertEquals(example.difficulty, RatingPolicy.difficulty(geometry, example.floor, score))
            }
            assertEquals(GameDifficulty.Simple,
                RatingPolicy.difficulty(geometry, LogicTier.NAKED_SINGLES, score, exposeSimple = true))
        }
    }

    @Test
    fun labelPolicyVersionsOnlySixBySixAssessmentsAndPreservesTheBaseVersion() {
        val adapter = ClassicDifficultyAdapter()
        val baseVersion = "libresudoku-logic-v3.0.0:chain12:als3:libresudoku-effort-v1:four-labels"
        assertEquals(baseVersion, adapter.ratingVersion)
        assertEquals(baseVersion, adapter.ratingVersion(GameType.Default9x9))
        assertEquals(baseVersion, adapter.ratingVersion(GameType.Default12x12))
        val sixVersion = "$baseVersion:${RatingPolicy.SIX_BY_SIX_LABEL_VERSION}"
        assertEquals(sixVersion, adapter.ratingVersion(GameType.Default6x6))
        assertEquals("libresudoku-effort-v1:9x9", RatingPolicy.identifier(SudokuGeometry(9, 3, 3)))
        assertEquals("libresudoku-effort-v1:technique-only", RatingPolicy.identifier(SudokuGeometry(12, 3, 4)))
        assertEquals("libresudoku-effort-v1:technique-only:${RatingPolicy.SIX_BY_SIX_LABEL_VERSION}",
            RatingPolicy.identifier(geometry))
        val puzzle = examples[1].puzzle.map { it.digitToInt() }.toIntArray()
        assertEquals(sixVersion, adapter.evaluateKnownUnique(GameType.Default6x6, puzzle).ratingVersion)
        val invalid = adapter.evaluateKnownUnique(GameType.Default6x6, IntArray(1))
        assertEquals(sixVersion, invalid.ratingVersion)
        assertNull(invalid.difficulty)
        assertNull(invalid.effortScore)
        assertNull(invalid.scorePolicy)
    }
}
