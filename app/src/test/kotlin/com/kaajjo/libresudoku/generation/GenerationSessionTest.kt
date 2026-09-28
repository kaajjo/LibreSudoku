package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.GenerationProfile
import com.kaajjo.libresudoku.core.qqwing.QQWingControllerV2
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import com.kaajjo.libresudoku.ui.components.generation.GenerationSession
import com.kaajjo.libresudoku.ui.components.generation.GenerationState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class GenerationSessionTest {
    @Test
    fun mismatch_AutomaticallyRetriesWithOriginalDifficulty() = sessionTest {
        val requested = mutableListOf<GameDifficulty>()
        val saved = mutableListOf<QQWingResult>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            requested += difficulty
            puzzle(if (requested.size == 1) GameDifficulty.Moderate else difficulty)
        })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Hard, save = { saved += it })
            // Start cannot replace an active request, including between automatic attempts.
            yield()
            assertTrue(session.state.value is GenerationState.Running)
            assertTrue(saved.isEmpty())
            session.start(GameType.Default9x9, GameDifficulty.Easy, save = { error("Duplicate request") })
            session.state.first { it == GenerationState.Completed }
            assertEquals(listOf(GameDifficulty.Hard, GameDifficulty.Hard), requested)
            assertEquals(GameDifficulty.Hard, saved.single().difficulty)
        } finally { session.cancel() }
    }

    @Test
    fun realController_ExhaustedSearchKeepsRunningUntilCancelled() = sessionTest {
        val controller = singleHoleController()
        val repeated = CompletableDeferred<Unit>()
        var calls = 0
        var saved = 0
        val errors = mutableListOf<Exception>()
        val session = GenerationSession(this, generate = { type, difficulty ->
            if (++calls == 3) {
                repeated.complete(Unit)
                awaitCancellation()
            }
            controller.generate(type, difficulty)
        }, onError = { errors += it })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Challenge, save = { saved++ })
            repeated.await()
            val state = session.state.value as GenerationState.Running
            assertEquals(GameDifficulty.Challenge, state.progress.difficulty)
            assertEquals(0, saved)
            assertTrue(errors.isEmpty())
            session.cancel()
            yield()
            assertEquals(GenerationState.Idle, session.state.value)
            assertEquals(3, calls)
        } finally { session.cancel() }
    }

    @Test
    fun batch_AutomaticRetryKeepsDifficultyAndSavedPrefix() = sessionTest {
        val requested = mutableListOf<GameDifficulty>()
        val saved = mutableListOf<GameDifficulty>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            requested += difficulty
            puzzle(if (requested.size == 2) GameDifficulty.Moderate else difficulty)
        })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Hard, count = 3, save = { saved += it.difficulty })
            session.state.first { it == GenerationState.Completed }
            assertEquals(List(4) { GameDifficulty.Hard }, requested)
            assertEquals(List(3) { GameDifficulty.Hard }, saved)
        } finally { session.cancel() }
    }

    @Test
    fun generationError_AutomaticallyRetriesRequest() = sessionTest {
        var calls = 0
        var saved = 0
        val errors = mutableListOf<Exception>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            if (++calls == 1) error("Generator failed")
            puzzle(difficulty)
        }, onError = { errors += it })
        try {
            session.start(GameType.Default9x9, GameDifficulty.Easy, save = { saved++ })
            session.state.first { it == GenerationState.Completed }
            assertEquals(2, calls)
            assertEquals(1, saved)
            assertEquals(1, errors.size)
        } finally { session.cancel() }
    }

    @Test
    fun realController_NoRatedPuzzleRetriesWithoutSavingOrReportingAnError() = sessionTest {
        val limitedController = singleHoleController(maxSearchNodesPerCheck = 1)
        val readyController = singleHoleController()
        val requested = mutableListOf<GameDifficulty>()
        val saved = mutableListOf<QQWingResult>()
        val errors = mutableListOf<Exception>()
        val session = GenerationSession(this, generate = { type, difficulty ->
            requested += difficulty
            val controller = if (requested.size == 1) limitedController else readyController
            controller.generate(type, difficulty)
        }, onError = { errors += it })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Easy, save = { saved += it })
            session.state.first { it == GenerationState.Completed }
            assertEquals(listOf(GameDifficulty.Easy, GameDifficulty.Easy), requested)
            assertEquals(GameDifficulty.Easy, saved.single().difficulty)
            assertEquals(true, saved.single().ratingMetadata?.logicallySolved)
            assertTrue(errors.isEmpty())
        } finally { session.cancel() }
    }

    @Test
    fun saveError_RetryUsesSamePuzzle_WithoutDuplicatingBatchPrefix() = sessionTest {
        var generated = 0
        var failOnce = true
        var failedPuzzle: QQWingResult? = null
        val saved = mutableListOf<QQWingResult>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            generated++
            puzzle(difficulty)
        })
        try {
            session.start(GameType.Default9x9, GameDifficulty.Easy, count = 3) { result ->
                if (saved.size == 1 && failOnce) {
                    failedPuzzle = result
                    failOnce = false
                    error("Database unavailable")
                }
                saved += result
            }
            val state = session.state.first { it is GenerationState.SaveFailed } as GenerationState.SaveFailed
            assertEquals(1, state.progress.completed)
            assertEquals(1, saved.size)
            session.retry()
            session.state.first { it == GenerationState.Completed }
            assertEquals(3, generated)
            assertEquals(3, saved.size)
            assertSame(failedPuzzle, saved[1])
        } finally { session.cancel() }
    }

    @Test
    fun cancelBetweenAutomaticAttempts_DoesNotGenerateOrSaveAgain() = sessionTest {
        val attempted = CompletableDeferred<Unit>()
        var calls = 0
        var saved = 0
        val session = GenerationSession(this, generate = { _, _ ->
            calls++
            attempted.complete(Unit)
            puzzle(GameDifficulty.Moderate)
        }, onError = { error("Cancellation is not an error") })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Hard, save = { saved++ })
            attempted.await()
            assertTrue(session.state.value is GenerationState.Running)
            session.cancel()
            yield()
            assertEquals(GenerationState.Idle, session.state.value)
            assertEquals(1, calls)
            assertEquals(0, saved)
        } finally { session.cancel() }
    }

    @Test
    fun cancelAndRestart_LateResultCannotSaveOrResetNewSession() = sessionTest {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val saved = mutableListOf<GameDifficulty>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            if (++calls == 1) {
                withContext(NonCancellable) {
                    started.complete(Unit)
                    release.await()
                }
            }
            puzzle(difficulty)
        })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Hard, save = { saved += it.difficulty })
            started.await()
            session.cancel()
            session.start(GameType.Default9x9, GameDifficulty.Easy, save = { saved += it.difficulty })
            session.state.first { it == GenerationState.Completed }
            release.complete(Unit)
            yield()
            assertEquals(listOf(GameDifficulty.Easy), saved)
            assertEquals(GenerationState.Completed, session.state.value)
        } finally {
            release.complete(Unit)
            session.cancel()
        }
    }

    @Test
    fun realController_CancelAndRestartDoesNotPublishOldRequest() = sessionTest {
        val oldStarted = CompletableDeferred<Unit>()
        val oldFinished = CompletableDeferred<Unit>()
        val releaseOld = CountDownLatch(1)
        val profileCalls = AtomicInteger()
        val controller = QQWingControllerV2(
            random = Random(4),
            maxGenerationAttempts = 1,
            profileFor = {
                if (profileCalls.incrementAndGet() == 1) {
                    oldStarted.complete(Unit)
                    check(releaseOld.await(5, TimeUnit.SECONDS)) { "Old request was not released" }
                }
                singleHoleProfile()
            }
        )
        var calls = 0
        val saved = mutableListOf<QQWingResult>()
        val errors = mutableListOf<Exception>()
        val session = GenerationSession(this, generate = { type, difficulty ->
            val firstRequest = ++calls == 1
            try {
                controller.generate(type, difficulty)
            } finally {
                if (firstRequest) oldFinished.complete(Unit)
            }
        }, onError = { errors += it })
        try {
            session.start(GameType.Default6x6, GameDifficulty.Challenge, save = { saved += it })
            // The first request is now inside the controller on its CPU dispatcher.
            oldStarted.await()
            session.cancel()
            session.start(GameType.Default9x9, GameDifficulty.Easy, save = { saved += it })
            session.state.first { it == GenerationState.Completed }
            releaseOld.countDown()
            oldFinished.await()
            yield()
            assertEquals(GenerationState.Completed, session.state.value)
            assertEquals(81, saved.single().puzzle.size)
            assertEquals(GameDifficulty.Easy, saved.single().difficulty)
            assertEquals(true, saved.single().ratingMetadata?.logicallySolved)
            assertTrue(errors.isEmpty())
        } finally {
            releaseOld.countDown()
            session.cancel()
        }
    }

    @Test
    fun unratedOrNonUniqueResults_CannotBeSaved() = sessionTest {
        val invalid = listOf(
            puzzle(GameDifficulty.Unspecified),
            puzzle(GameDifficulty.Custom),
            puzzle(GameDifficulty.Easy).copy(ratingMetadata = null),
            puzzle(GameDifficulty.Easy).let { it.copy(ratingMetadata = it.ratingMetadata!!.copy(logicallySolved = false)) },
            puzzle(GameDifficulty.Easy).copy(solutionCount = 2)
        )
        for (requested in listOf(GameDifficulty.Easy, GameDifficulty.Unspecified)) {
            for (candidate in invalid) {
                var saved = 0
                val retried = CompletableDeferred<Unit>()
                var calls = 0
                val session = GenerationSession(this, generate = { _, _ ->
                    if (++calls == 2) {
                        retried.complete(Unit)
                        awaitCancellation()
                    }
                    candidate
                })
                try {
                    session.start(GameType.Default9x9, requested, save = { saved++ })
                    retried.await()
                    assertTrue(session.state.value is GenerationState.Running)
                    assertEquals(0, saved)
                } finally { session.cancel() }
            }
        }
    }

    @Test
    fun unspecifiedRequest_AcceptsAnyFullyRatedDifficultyInBatch() = sessionTest {
        val requested = mutableListOf<GameDifficulty>()
        val available = listOf(GameDifficulty.Easy, GameDifficulty.Hard, GameDifficulty.Challenge)
        val saved = mutableListOf<GameDifficulty>()
        val session = GenerationSession(this, generate = { _, difficulty ->
            requested += difficulty
            puzzle(available[requested.lastIndex])
        })
        try {
            session.start(GameType.Default9x9, GameDifficulty.Unspecified, count = 3, save = { saved += it.difficulty })
            session.state.first { it == GenerationState.Completed }
            assertEquals(List(3) { GameDifficulty.Unspecified }, requested)
            assertEquals(available, saved)
        } finally { session.cancel() }
    }

    private fun singleHoleProfile() = GenerationProfile(
        GameDifficulty.entries.associateWith { 1..1 }
    )

    private fun singleHoleController(maxSearchNodesPerCheck: Long = 2_000_000L) = QQWingControllerV2(
        random = Random(4),
        maxGenerationAttempts = 1,
        maxSearchNodesPerCheck = maxSearchNodesPerCheck,
        profileFor = { singleHoleProfile() }
    )

    private fun puzzle(difficulty: GameDifficulty) = QQWingResult(
        intArrayOf(0), intArrayOf(1), difficulty, 1,
        RatingMetadata("test", LogicTier.NAKED_SINGLES, emptyMap())
    )

    private fun sessionTest(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        withTimeout(5_000, block)
    }
}
