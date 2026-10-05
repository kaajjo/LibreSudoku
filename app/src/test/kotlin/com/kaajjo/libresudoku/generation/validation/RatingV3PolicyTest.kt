package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.*
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import org.junit.Assert.*
import org.junit.Test

class RatingV3PolicyTest {
    private val geometry = SudokuGeometry(9, 3, 3)
    private val basicPuzzle =
        "700500090900000008100700050000023000000008300071000000060000100000095400008030000"
            .map { it.digitToInt() }.toIntArray()

    @Test
    fun fullHouseHasItsOwnWitnessAndCountsWithoutCapturingSteps() {
        val solved = IntArray(81) { cell ->
            val row = cell / 9
            (row * 3 + row / 3 + cell % 9) % 9 + 1
        }
        val puzzle = solved.copyOf().also { it[0] = 0 }
        val captured = QqWingLogicalRater().rate(puzzle, geometry, captureSteps = true) as LogicalRating.Rated
        val compact = QqWingLogicalRater().rate(puzzle, geometry) as LogicalRating.Rated
        assertEquals(1, captured.report.moveCount)
        assertEquals(Technique.FULL_HOUSE, captured.report.steps.single().technique)
        assertEquals(listOf(0), captured.report.steps.single().units)
        assertEquals(4, captured.report.effortScore)
        assertEquals(LogicTier.NAKED_SINGLES, captured.report.techniqueFloor)
        assertEquals(captured.report.counts(), compact.report.counts())
        assertEquals(captured.report.effortScore, compact.report.effortScore)
        assertTrue(compact.report.steps.isEmpty())
        assertArrayEquals(solved, compact.report.solutionSnapshot())
    }

    @Test
    fun singletonCandidateInAnUnfinishedUnitRemainsANakedSingle() {
        val state = LogicalState(Topology.of(SudokuGeometry(4, 2, 2)),
            EvaluationControl(EvaluationLimits(), {}))
        assertTrue(state.initialize(IntArray(16)))
        state.masks[0] = bit(1)
        assertEquals(Technique.SINGLE, BasicTechniques.singles(state, true)?.technique)
    }

    @Test
    fun completedScoreExcludesEveryEarlierStalledPass() {
        val observedScores = mutableMapOf<LogicTier, Int>()
        val rating = QqWingLogicalRater().rateObserved(basicPuzzle, geometry, false, true, {}) {
                tier, step, _, _ ->
            observedScores[tier] = (observedScores[tier] ?: 0) + RatingPolicy.weight(step.technique)
        } as LogicalRating.Rated
        assertEquals(LogicTier.BASIC, rating.report.tier)
        assertEquals(listOf(LogicTier.SINGLES), rating.lowerTiersStalled)
        assertTrue(observedScores.getValue(LogicTier.SINGLES) > 0)
        assertEquals(observedScores.getValue(LogicTier.BASIC), rating.report.effortScore)
        assertEquals(rating.report.steps.sumOf { RatingPolicy.weight(it.technique) }, rating.report.effortScore)
        assertEquals(LogicTier.BASIC, rating.report.techniqueFloor)
        val compact = QqWingLogicalRater().rate(basicPuzzle, geometry) as LogicalRating.Rated
        assertEquals(rating.report.effortScore, compact.report.effortScore)
        assertEquals(rating.report.counts(), compact.report.counts())
        assertArrayEquals(rating.report.solutionSnapshot(), compact.report.solutionSnapshot())
    }

    @Test
    fun scorePromotesButCannotDemoteTechniqueFloor() {
        assertEquals(GameDifficulty.Easy, RatingPolicy.difficulty(geometry, LogicTier.SINGLES, 800))
        assertEquals(GameDifficulty.Moderate, RatingPolicy.difficulty(geometry, LogicTier.SINGLES, 801))
        assertEquals(GameDifficulty.Moderate, RatingPolicy.difficulty(geometry, LogicTier.BASIC, 1000))
        assertEquals(GameDifficulty.Hard, RatingPolicy.difficulty(geometry, LogicTier.BASIC, 1001))
        assertEquals(GameDifficulty.Hard, RatingPolicy.difficulty(geometry, LogicTier.ADVANCED, 0))
        assertEquals(GameDifficulty.Hard, RatingPolicy.difficulty(geometry, LogicTier.ADVANCED, 1600))
        assertEquals(GameDifficulty.Challenge, RatingPolicy.difficulty(geometry, LogicTier.ADVANCED, 1601))
        assertEquals(GameDifficulty.Challenge, RatingPolicy.difficulty(geometry, LogicTier.CHAINS, 0))
        assertEquals(GameDifficulty.Simple, RatingPolicy.difficulty(geometry, LogicTier.NAKED_SINGLES, 4, true))
        assertEquals(GameDifficulty.Easy, RatingPolicy.difficulty(geometry, LogicTier.NAKED_SINGLES, 4))
    }

    @Test
    fun nonStandardGeometryDoesNotUseUncalibratedNineByNineThresholds() {
        for (other in listOf(SudokuGeometry(12, 3, 4), SudokuGeometry(9, 1, 9))) {
            assertEquals(GameDifficulty.Easy, RatingPolicy.difficulty(other, LogicTier.SINGLES, 100_000))
            assertEquals(GameDifficulty.Moderate, RatingPolicy.difficulty(other, LogicTier.BASIC, 100_000))
            assertEquals(GameDifficulty.Hard, RatingPolicy.difficulty(other, LogicTier.ADVANCED, 100_000))
            assertTrue(RatingPolicy.identifier(other).endsWith(":technique-only"))
        }
        val six = SudokuGeometry(6, 2, 3)
        assertEquals(GameDifficulty.Easy, RatingPolicy.difficulty(six, LogicTier.NAKED_SINGLES, 100_000))
        assertEquals(GameDifficulty.Moderate, RatingPolicy.difficulty(six, LogicTier.SINGLES, 100_000))
        assertEquals(GameDifficulty.Moderate, RatingPolicy.difficulty(six, LogicTier.BASIC, 100_000))
        assertEquals(GameDifficulty.Hard, RatingPolicy.difficulty(six, LogicTier.ADVANCED, 100_000))
        assertFalse(RatingPolicy.usesScoreThresholds(six))
    }

    @Test
    fun activePassesExcludeLegacyTiersAndEveryTechniqueHasPositiveWeight() {
        assertEquals(listOf(LogicTier.SINGLES, LogicTier.BASIC, LogicTier.ADVANCED, LogicTier.CHAINS),
            RatingPolicy.activeTiers())
        assertEquals(LogicTier.NAKED_SINGLES, RatingPolicy.activeTiers(true).first())
        assertEquals(LogicTier.PAIRS, LogicTier.valueOf("PAIRS"))
        assertEquals(LogicTier.LOCKED_CANDIDATES, LogicTier.valueOf("LOCKED_CANDIDATES"))
        for (technique in Technique.values()) assertTrue("Missing positive score for $technique",
            RatingPolicy.weight(technique) > 0)
        assertEquals(LogicTier.BASIC, Technique.NAKED_TRIPLE.tier)
        assertEquals(LogicTier.BASIC, Technique.ROW_BOX.tier)
        assertEquals(LogicTier.ADVANCED, Technique.NAKED_QUAD.tier)
    }

    @Test
    fun incompleteAndInvalidAssessmentsNeverExposeUsableScores() {
        val adapter = ClassicDifficultyAdapter()
        val invalid = adapter.evaluateKnownUnique(GameType.Default9x9, IntArray(1))
        assertTrue(invalid.rating is LogicalRating.InvalidInput)
        assertNull(invalid.difficulty)
        assertNull(invalid.effortScore)
        assertNull(invalid.scorePolicy)
        // The adapter's caller precondition is intentionally bypassed to exercise its unknown path.
        val incomplete = adapter.evaluateKnownUnique(GameType.Default6x6, IntArray(36))
        assertTrue(incomplete.rating is LogicalRating.BeyondSupported)
        assertNull(incomplete.difficulty)
        assertNull(incomplete.effortScore)
        assertNull(incomplete.scorePolicy)
        val solved = IntArray(36) { cell ->
            val row = cell / 6
            (row * 3 + row / 2 + cell % 6) % 6 + 1
        }
        val alreadySolved = adapter.evaluateKnownUnique(GameType.Default6x6, solved)
        assertTrue(alreadySolved.rating is LogicalRating.AlreadySolved)
        assertNull(alreadySolved.difficulty)
        assertNull(alreadySolved.effortScore)
        assertNull(alreadySolved.scorePolicy)
        val completed = adapter.evaluateKnownUnique(GameType.Default9x9, basicPuzzle)
        assertNotNull(completed.effortScore)
        assertEquals(RatingPolicy.identifier(geometry), completed.scorePolicy)
    }
}
