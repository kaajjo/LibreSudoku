package com.kaajjo.libresudoku.qqwing

import com.kaajjo.libresudoku.core.generator.dlx.DlxGenerator
import com.kaajjo.libresudoku.core.generator.rating.ClassicDifficultyAdapter
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.ClassicGameTypes
import com.kaajjo.libresudoku.core.qqwing.ClassicGenerationProfiles
import com.kaajjo.libresudoku.core.qqwing.GenerationProfile
import com.kaajjo.libresudoku.core.qqwing.NoRatedPuzzleException
import com.kaajjo.libresudoku.core.qqwing.QQWing
import com.kaajjo.libresudoku.core.qqwing.QQWingControllerV2
import com.kaajjo.libresudoku.core.qqwing.models.SolveResult
import com.kaajjo.libresudoku.generation.UNIQUE_9X9
import com.kaajjo.libresudoku.generation.SOLUTION_6X6
import com.kaajjo.libresudoku.generation.assertNoUnitConflicts
import com.kaajjo.libresudoku.generation.assertSolutionKeepsGivens
import com.kaajjo.libresudoku.generation.assertValidSolvedBoard
import com.kaajjo.libresudoku.generation.parseBoardString
import com.kaajjo.libresudoku.generation.validation.IndependentOracle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class QQWingControllerV2Test {

    @Test
    fun sixBySix_OfferedCategoriesActuallyGenerateWithProductionProfiles() = runBlocking {
        val type = GameType.Default6x6
        val categories = listOf(GameDifficulty.Easy, GameDifficulty.Moderate,
            GameDifficulty.Hard, GameDifficulty.Challenge)
        for ((categoryIndex, category) in categories.withIndex()) {
            repeat(4) { index ->
                val seed = 601509000L + categoryIndex * 1000003L + index
                // A bounded failure must fail this regression: accepting it hid the 6x6 bug.
                val result = QQWingControllerV2(Random(seed)).generate(type, category)
                assertEquals("$category, seed=$seed", category, result.difficulty)
                val assessment = ClassicDifficultyAdapter().evaluateKnownUnique(type, result.puzzle)
                assertEquals(category, assessment.difficulty)
                assertEquals(assessment.ratingVersion, result.ratingMetadata?.version)
                val solutions = IndependentOracle(ClassicGameTypes.geometryOf(type))
                    .solve(result.puzzle, limit = 2)
                assertEquals(1, solutions.size)
                assertArrayEquals(solutions.single(), result.solution)
            }
        }
    }

    @Test
    fun generationUsesProfileBudgetUnlessCallerSuppliesExplicitCap() = runBlocking {
        val category = GameDifficulty.Challenge
        val profile = GenerationProfile(mapOf(category to (1..1)),
            attemptLimits = mapOf(category to 3))
        for ((override, expectedAttempts) in listOf(null to 3, 1 to 1)) {
            val controller = QQWingControllerV2(Random(4), maxGenerationAttempts = override,
                profileFor = { profile })
            try {
                controller.generate(GameType.Default6x6, category)
                fail("One-hole puzzle cannot satisfy Challenge")
            } catch (expected: NoRatedPuzzleException) {
                assertTrue(expected.message.orEmpty().contains(
                    "attempts=$expectedAttempts, mismatched=$expectedAttempts"))
            }
        }
        assertEquals(1000, ClassicGenerationProfiles.forType(GameType.Default6x6).attemptLimit(category))
        for (type in listOf(GameType.Default6x6, GameType.Default9x9, GameType.Default12x12)) {
            assertEquals(250, ClassicGenerationProfiles.forType(type).attemptLimit(GameDifficulty.Hard))
            if (type != GameType.Default6x6) {
                assertEquals(250, ClassicGenerationProfiles.forType(type).attemptLimit(category))
            }
        }
    }

    @Test
    fun generate_AllClassicTypesAndDifficulties_ReturnsRequestedRatedPuzzleOrFails() = runBlocking {
        val types = listOf(
            GameType.Default6x6,
            GameType.Default9x9,
            GameType.Default12x12
        )
        val difficulties = listOf(
            GameDifficulty.Easy,
            GameDifficulty.Moderate,
            GameDifficulty.Hard,
            GameDifficulty.Challenge,
            GameDifficulty.Unspecified
        )

        for (type in types) {
            for (difficulty in difficulties) {
                val seed = type.ordinal * 100L + difficulty.ordinal
                val controller = QQWingControllerV2(
                    random = Random(seed),
                    maxGenerationAttempts = 4,
                    maxGenerationMillis = 60_000
                )
                val result = try {
                    withTimeout(60_000) { controller.generate(type, difficulty) }
                } catch (_: NoRatedPuzzleException) {
                    // This test intentionally allows only four attempts, not unlimited searching.
                    continue
                }

                if (difficulty != GameDifficulty.Unspecified) assertEquals(difficulty, result.difficulty)
                val assessment = ClassicDifficultyAdapter().evaluateKnownUnique(type, result.puzzle)
                assertEquals(assessment.difficulty, result.difficulty)
                assertTrue(result.ratingMetadata?.logicallySolved == true)
                assertEquals(assessment.ratingVersion, result.ratingMetadata?.version)
                assertEquals(assessment.effortScore, result.ratingMetadata?.effortScore)
                assertEquals(assessment.scorePolicy, result.ratingMetadata?.scorePolicy)
                assertEquals(1, result.solutionCount)
                assertNoUnitConflicts(result.puzzle, type)
                assertValidSolvedBoard(result.solution, type)
                assertSolutionKeepsGivens(result.puzzle, result.solution)

                val independentSolver = QQWing(type, GameDifficulty.Unspecified)
                assertTrue(independentSolver.setPuzzle(result.puzzle))
                assertEquals(1, independentSolver.countSolutionsLimited())
            }
        }
    }

    @Test
    fun generate_SingleHoleProfile_ReturnsExactOrUnspecifiedRequestForEveryClassicType() = runBlocking {
        val profile = GenerationProfile(GameDifficulty.entries.associateWith { 1..1 })
        for (type in listOf(GameType.Default6x6, GameType.Default9x9, GameType.Default12x12)) {
            val controller = QQWingControllerV2(Random(4), maxGenerationAttempts = 1,
                maxGenerationMillis = 60_000, profileFor = { profile })
            for (requested in listOf(GameDifficulty.Easy, GameDifficulty.Unspecified)) {
                val result = controller.generate(type, requested)
                assertEquals(GameDifficulty.Easy, result.difficulty)
                assertEquals(true, result.ratingMetadata?.logicallySolved)
                assertEquals(1, result.puzzle.count { it == 0 })
                assertEquals(1, result.solutionCount)
                assertValidSolvedBoard(result.solution, type)
                assertSolutionKeepsGivens(result.puzzle, result.solution)
            }
        }
    }

    @Test
    fun generate_OnlyDifferentDifficultyAvailable_ExhaustsAttemptsWithoutSubstitution() = runBlocking {
        val profile = GenerationProfile(GameDifficulty.entries.associateWith { 1..1 })
        for (difficulty in listOf(GameDifficulty.Moderate, GameDifficulty.Hard, GameDifficulty.Challenge)) {
            val controller = QQWingControllerV2(Random(4), maxGenerationAttempts = 3,
                maxGenerationMillis = 60_000, profileFor = { profile })
            try {
                controller.generate(GameType.Default6x6, difficulty)
                fail("One-hole puzzles cannot satisfy $difficulty")
            } catch (expected: NoRatedPuzzleException) {
                assertTrue(expected.message.orEmpty().contains("attempts=3, mismatched=3"))
            }
            // A failed request does not poison the controller; Unspecified accepts its known rating.
            val result = controller.generate(GameType.Default6x6, GameDifficulty.Unspecified)
            assertEquals(GameDifficulty.Easy, result.difficulty)
            assertEquals(true, result.ratingMetadata?.logicallySolved)
            assertEquals(1, result.puzzle.count { it == 0 })
        }
    }

    @Test
    fun generate_TimeBudgetExhausted_FailsWithoutStartingUnboundedSearch() = runBlocking {
        val controller = QQWingControllerV2(Random(4), maxGenerationAttempts = 250,
            maxGenerationMillis = 1, profileFor = {
                // Deterministically expire the elapsed budget before the first attempt.
                Thread.sleep(10)
                GenerationProfile(GameDifficulty.entries.associateWith { 1..1 })
            })
        try {
            controller.generate(GameType.Default6x6, GameDifficulty.Challenge)
            fail("Expected elapsed-time exhaustion")
        } catch (expected: NoRatedPuzzleException) {
            assertTrue(expected.message.orEmpty().contains("Time budget exhausted"))
            assertTrue(expected.message.orEmpty().contains("attempts=0"))
        }
    }

    @Test
    fun generate_UnsupportedDifficulty_FailsFast() = runBlocking {
        for (difficulty in listOf(GameDifficulty.Simple, GameDifficulty.Custom)) {
            try {
                QQWingControllerV2(Random(0), maxGenerationAttempts = 1)
                    .generate(GameType.Default9x9, difficulty)
                fail("Expected $difficulty to be rejected")
            } catch (expected: IllegalArgumentException) {
            }
        }

        try {
            QQWingControllerV2(Random(0), maxGenerationAttempts = 1)
                .generate(GameType.Unspecified, GameDifficulty.Easy)
            fail("Expected an unspecified game type to be rejected")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun solve_MalformedBoards_ReturnImpossible() = runBlocking {
        val controller = QQWingControllerV2(Random(0))
        val malformed = listOf(
            IntArray(80),
            IntArray(82),
            IntArray(81).also { it[0] = -1 },
            IntArray(81).also { it[0] = 10 },
            IntArray(81).also {
                it[0] = 5
                it[1] = 5
            }
        )

        malformed.forEach { board ->
            assertTrue(controller.solve(board, GameType.Default9x9) is SolveResult.Impossible)
        }
        try {
            controller.solve(IntArray(1), GameType.Unspecified)
            fail("An unsupported type must be rejected independently of board solvability")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun solve_UniquePuzzle_ReturnsItsOnlySolution() = runBlocking {
        val puzzle = parseBoardString(UNIQUE_9X9, GameType.Default9x9)
        val result = QQWingControllerV2(Random(0)).solve(puzzle, GameType.Default9x9)

        assertTrue(result is SolveResult.Success)
        result as SolveResult.Success
        assertEquals(1, result.solutionCount)
        assertValidSolvedBoard(result.solution, GameType.Default9x9)
        assertSolutionKeepsGivens(puzzle, result.solution)
    }

    @Test
    fun solve_CompletedBoard_ReturnsOneSolution() = runBlocking {
        val solved = parseBoardString(SOLUTION_6X6, GameType.Default6x6)
        val result = QQWingControllerV2(Random(0)).solve(solved, GameType.Default6x6)

        assertTrue(result is SolveResult.Success)
        result as SolveResult.Success
        assertEquals(1, result.solutionCount)
        assertArrayEquals(solved, result.solution)
    }

    @Test
    fun legacyDifficultyRating_DefaultAndKillerVariantsRetainSizeThresholds() {
        val variants = listOf(
            Triple(GameType.Default6x6, GameType.Killer6x6, 18),
            Triple(GameType.Default9x9, GameType.Killer9x9, 45),
            Triple(GameType.Default12x12, GameType.Killer12x12, 85)
        )

        variants.forEachIndexed { seed, (defaultType, killerType, targetEmpty) ->
            val puzzle = DlxGenerator.generate(defaultType, targetEmpty, Random(seed.toLong()))

            assertEquals(
                "Difficulty thresholds differ for $defaultType and $killerType",
                rateDifficulty(puzzle, defaultType),
                rateDifficulty(puzzle, killerType)
            )
        }
    }

    @Test
    fun constructor_NonPositiveAttemptLimit_Throws() {
        for (limit in intArrayOf(0, -1)) {
            try {
                QQWingControllerV2(Random(0), maxGenerationAttempts = limit)
                fail("Expected maxGenerationAttempts=$limit to be rejected")
            } catch (expected: IllegalArgumentException) {
            }
        }
    }

    private fun rateDifficulty(puzzle: IntArray, type: GameType): GameDifficulty {
        val solver = QQWing(type, GameDifficulty.Unspecified)
        assertTrue(solver.setPuzzle(puzzle))
        solver.setRecordHistory(true)
        assertTrue(solver.solve())
        return solver.getDifficulty()
    }
}
