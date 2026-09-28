package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.dlx.DlxGenerator
import com.kaajjo.libresudoku.core.generator.dlx.DlxSearchBudgetExceeded
import com.kaajjo.libresudoku.core.generator.rating.*
import com.kaajjo.libresudoku.core.qqwing.*
import com.kaajjo.libresudoku.core.qqwing.models.SolveResult
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.random.Random

class LogicalControllerIntegrationTest {
    @Test
    fun generationAssessmentAndSolveAgreeForEveryRegisteredGeometry() = runBlocking {
        for (type in listOf(GameType.Default6x6, GameType.Default9x9, GameType.Default12x12)) {
            val controller = QQWingControllerV2(Random(123), maxGenerationAttempts = 4,
                maxGenerationMillis = 60_000)
            for (difficulty in listOf(GameDifficulty.Easy, GameDifficulty.Unspecified)) {
                val result = controller.generate(type, difficulty)
                check(result.solutionCount == 1 && result.ratingMetadata?.logicallySolved == true)
                check(result.difficulty !in listOf(GameDifficulty.Unspecified, GameDifficulty.Custom))
                check(difficulty==GameDifficulty.Unspecified || result.difficulty==difficulty)
                val again = ClassicDifficultyAdapter().evaluateKnownUnique(type, result.puzzle)
                check(again.difficulty == result.difficulty && again.ratingVersion == result.ratingMetadata?.version)
                check(again.effortScore==result.ratingMetadata?.effortScore && again.scorePolicy==result.ratingMetadata?.scorePolicy)
                val solved = controller.solve(result.puzzle, type) as SolveResult.Success
                check(solved.solutionCount == 1 && solved.solution.contentEquals(result.solution))
            }
        }
    }

    @Test
    fun equalSequentialRngStateProducesEqualRequests() = runBlocking {
        val a = QQWingControllerV2(Random(2), maxGenerationAttempts = 3, maxGenerationMillis = 60_000)
        val b = QQWingControllerV2(Random(2), maxGenerationAttempts = 3, maxGenerationMillis = 60_000)
        repeat(3) {
            check(a.generate(GameType.Default6x6, GameDifficulty.Easy).puzzle.contentEquals(
                b.generate(GameType.Default6x6, GameDifficulty.Easy).puzzle))
        }
    }

    @Test
    fun oneControllerSupportsConcurrentMixedSizeRequests() = runBlocking {
        val controller = QQWingControllerV2(Random(47), maxGenerationAttempts = 2,
            maxGenerationMillis = 60_000)
        coroutineScope {
            List(12) { index ->
                async(Dispatchers.Default) {
                    val type = if (index % 2 == 0) GameType.Default6x6 else GameType.Default9x9
                    val result = controller.generate(type, GameDifficulty.Easy)
                    val solutions = IndependentOracle(ClassicGameTypes.geometryOf(type))
                        .solve(result.puzzle, limit = 2)
                    check(solutions.size == 1 && solutions.single().contentEquals(result.solution))
                }
            }.awaitAll()
        }
        Unit
    }

    @Test
    fun unsupportedVariantsAreRejectedAcrossAllEntryPoints() = runBlocking {
        val controller = QQWingControllerV2(Random(2), maxGenerationAttempts = 1)
        for (type in listOf(GameType.Unspecified, GameType.Killer6x6, GameType.Killer9x9, GameType.Killer12x12)) {
            expectFailure<IllegalArgumentException> { controller.generate(type, GameDifficulty.Easy) }
            expectFailure<IllegalArgumentException> { controller.solve(IntArray(type.size * type.size), type) }
            expectFailure<IllegalArgumentException> { DlxGenerator.generate(type, 1) }
            expectFailure<IllegalArgumentException> {
                ClassicDifficultyAdapter().evaluateKnownUnique(type, IntArray(type.size * type.size))
            }
        }
    }

    @Test
    fun simpleRequiresOptInAndProducesNakedSinglesRating() = runBlocking {
        expectFailure<IllegalArgumentException> {
            QQWingControllerV2(Random(2), 1).generate(GameType.Default6x6, GameDifficulty.Simple)
        }
        val result = QQWingControllerV2(Random(22), 5, exposeSimple = true,
            maxGenerationMillis = 60_000).generate(GameType.Default6x6, GameDifficulty.Simple)
        check(result.difficulty == GameDifficulty.Simple)
        check(result.ratingMetadata?.tier == LogicTier.NAKED_SINGLES)
    }

    @Test
    fun incompleteBudgetsNeverBecomeDifficultyOrImpossible() = runBlocking {
        expectFailure<NoRatedPuzzleException> {
            QQWingControllerV2(Random(1), 1, evaluationLimits = EvaluationLimits(maxOperations = 1),
                maxGenerationMillis = 60_000).generate(GameType.Default6x6, GameDifficulty.Easy)
        }
        expectFailure<DlxSearchBudgetExceeded> {
            QQWingControllerV2(maxSearchNodesPerCheck = 1).solve(IntArray(36), GameType.Default6x6)
        }
    }

    @Test
    fun cancellationAfterRequestStartsPropagatesAndControllerCanBeReused() = runBlocking {
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val controller = QQWingControllerV2(Random(3), 1, profileFor = { type ->
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            ClassicGenerationProfiles.forType(type)
        })
        var returned = false
        val job = launch(Dispatchers.Default) {
            controller.generate(GameType.Default9x9, GameDifficulty.Easy)
            returned = true
        }
        try {
            withContext(Dispatchers.IO) { check(started.await(5, TimeUnit.SECONDS)) }
            job.cancel()
        } finally {
            release.countDown()
            job.cancelAndJoin()
        }
        check(job.isCancelled && !returned)
        val result = controller.generate(GameType.Default6x6, GameDifficulty.Easy)
        check(result.solutionCount == 1 && result.ratingMetadata?.logicallySolved == true)
    }

    @Test
    fun advancedAndChainsHaveCompleteTraceWhileBeyondStaysUnrated() {
        for (tier in listOf("ADVANCED", "CHAINS", "BEYOND")) {
            val case = corpusCases().first { it.expectedTier == tier }
            val type = when (case.geometry.size) {
                6 -> GameType.Default6x6
                9 -> GameType.Default9x9
                else -> GameType.Default12x12
            }
            check(IndependentOracle(case.geometry, 10_000_000).solve(case.puzzle, limit = 2).size == 1)
            val assessment = ClassicDifficultyAdapter(captureSteps = true).evaluateKnownUnique(type, case.puzzle)
            val report = checkNotNull(reportOf(assessment.rating))
            if (tier == "BEYOND") {
                check(assessment.difficulty == null && assessment.rating is LogicalRating.BeyondSupported)
                check(report.remainingCells > 0)
                check(assessment.effortScore == null && assessment.scorePolicy == null)
            } else {
                check(assessment.rating is LogicalRating.Rated)
                check(assessment.difficulty == RatingPolicy.difficulty(case.geometry,
                    checkNotNull(report.techniqueFloor),report.effortScore))
                check(assessment.effortScore==report.effortScore && assessment.scorePolicy==RatingPolicy.identifier(case.geometry))
                check(report.tier.name == tier && report.remainingCells == 0 && report.steps.isNotEmpty())
                val state = fresh(case.geometry)
                check(state.initialize(case.puzzle))
                for (step in report.steps) {
                    verifyLinks(state, step)
                    state.apply(step)
                }
                check(state.board.contentEquals(report.solutionSnapshot()))
            }
        }
    }

    private inline fun <reified T : Throwable> expectFailure(block: () -> Unit) {
        try {
            block()
        } catch (error: Throwable) {
            if (error is T) return
            throw error
        }
        error("Expected ${T::class.java.simpleName}")
    }
}
