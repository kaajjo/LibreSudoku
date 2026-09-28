package com.kaajjo.libresudoku.generation

import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.ClassicGameTypes
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GamePuzzleGenerator
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.GenerationProfile
import com.kaajjo.libresudoku.core.qqwing.QQWingControllerV2
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.generation.validation.IndependentOracle
import com.kaajjo.libresudoku.ui.components.generation.GenerationSession
import com.kaajjo.libresudoku.ui.components.generation.GenerationState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class KillerGenerationTest {
    @Test
    fun everyKillerSize_ReachesSaveWithUniqueGivensAndValidSerializableCages() = generationTest {
        for ((killer, classic) in variants) {
            val expected = controller(123).generate(classic, GameDifficulty.Easy)
            val generator = GamePuzzleGenerator(controller(123))
            val saved = mutableListOf<QQWingResult>()
            val errors = mutableListOf<Exception>()
            val session = GenerationSession(this, generate = generator::generate,
                onError = { errors += it })
            try {
                session.start(killer, GameDifficulty.Easy) { saved += it }
                assertEquals("$killer failed: $errors", GenerationState.Completed,
                    session.state.first { it is GenerationState.Completed || it is GenerationState.SaveFailed })
                val result = saved.single()
                // Cages complement the existing unique puzzle; removing its givens would lose
                // the uniqueness guarantee supplied by the classical generator.
                assertArrayEquals(expected.puzzle, result.puzzle)
                assertArrayEquals(expected.solution, result.solution)
                assertTrue(result.puzzle.any { it != 0 })
                assertTrue(result.puzzle.any { it == 0 })
                assertEquals(GameDifficulty.Easy, result.difficulty)
                assertEquals(1, result.solutionCount)
                assertSolutionKeepsGivens(result.puzzle, result.solution)
                val independent = IndependentOracle(ClassicGameTypes.geometryOf(classic))
                    .solve(result.puzzle, limit = 2)
                assertEquals(1, independent.size)
                assertArrayEquals(independent.single(), result.solution)
                val baseRating = checkNotNull(expected.ratingMetadata)
                assertEquals(baseRating.copy(
                    version = "killer-classic-base-v1:${baseRating.version}"
                ), result.ratingMetadata)
                assertValidCages(result, killer)
                val parser = SudokuParser()
                val encoded = parser.killerSudokuCagesToString(checkNotNull(result.killerCages))
                assertEquals(result.killerCages, parser.parseKillerSudokuCages(encoded))
                assertTrue(errors.isEmpty())
            } finally {
                session.cancel()
            }
        }
    }

    @Test
    fun defaultSessionGenerator_AcceptsKillerWithoutAnInjectedController() = generationTest {
        val saved = mutableListOf<QQWingResult>()
        val errors = mutableListOf<Exception>()
        // This is the constructor used by the screens. Testing only an injected router would
        // miss the original integration bug where this default was classic-only.
        val session = GenerationSession(this, onError = { errors += it })
        try {
            session.start(GameType.Killer6x6, GameDifficulty.Easy) { saved += it }
            assertEquals("Default session failed: $errors", GenerationState.Completed,
                session.state.first { it is GenerationState.Completed || it is GenerationState.SaveFailed })
            assertValidCages(saved.single(), GameType.Killer6x6)
            assertTrue(errors.isEmpty())
        } finally {
            session.cancel()
        }
    }

    @Test
    fun killerRouting_PreservesAllFourRequestedDifficultyLabels() = generationTest {
        val categories = listOf(GameDifficulty.Easy, GameDifficulty.Moderate,
            GameDifficulty.Hard, GameDifficulty.Challenge)
        for ((index, category) in categories.withIndex()) {
            val seed = 601509000L + index * 1000003L
            val result = GamePuzzleGenerator(controller(seed))
                .generate(GameType.Killer6x6, category)
            assertEquals(category, result.difficulty)
            assertEquals(true, result.ratingMetadata?.logicallySolved)
            assertTrue(checkNotNull(result.ratingMetadata).version.startsWith("killer-classic-base-v1:"))
            assertValidCages(result, GameType.Killer6x6)
        }
    }

    @Test
    fun classicRouting_PreservesResultsAndDoesNotAddKillerMetadataOrCages() = generationTest {
        for ((_, classic) in variants) {
            val expected = controller(123).generate(classic, GameDifficulty.Easy)
            val actual = GamePuzzleGenerator(controller(123)).generate(classic, GameDifficulty.Easy)
            assertArrayEquals(expected.puzzle, actual.puzzle)
            assertArrayEquals(expected.solution, actual.solution)
            assertEquals(expected.difficulty, actual.difficulty)
            assertEquals(expected.solutionCount, actual.solutionCount)
            assertEquals(expected.ratingMetadata, actual.ratingMetadata)
            assertNull(actual.killerCages)
        }
    }

    @Test
    fun killerSearchExhaustion_RetriesWithoutSavingAnotherDifficulty() = generationTest {
        val profile = GenerationProfile(GameDifficulty.entries.associateWith { 1..1 })
        val generator = GamePuzzleGenerator(QQWingControllerV2(
            random = Random(4), maxGenerationAttempts = 1,
            maxGenerationMillis = 60_000, profileFor = { profile }
        ))
        val saved = mutableListOf<QQWingResult>()
        val errors = mutableListOf<Exception>()
        val retried = CompletableDeferred<Unit>()
        var calls = 0
        val session = GenerationSession(this, generate = { type, difficulty ->
            if (++calls == 2) {
                retried.complete(Unit)
                awaitCancellation()
            }
            generator.generate(type, difficulty)
        }, onError = { errors += it })
        try {
            session.start(GameType.Killer6x6, GameDifficulty.Challenge) { saved += it }
            retried.await()
            val state = session.state.value as GenerationState.Running
            assertEquals(GameType.Killer6x6, state.progress.type)
            assertEquals(GameDifficulty.Challenge, state.progress.difficulty)
            assertTrue(saved.isEmpty())
            assertTrue(errors.isEmpty())
        } finally {
            session.cancel()
        }
    }

    @Test
    fun killerSaveRetry_PreservesTheSamePuzzleAndCages() = generationTest {
        val generator = GamePuzzleGenerator(controller(123))
        val parser = SudokuParser()
        val attempted = mutableListOf<QQWingResult>()
        val payloads = mutableListOf<String>()
        var generationCalls = 0
        val session = GenerationSession(this, generate = { type, difficulty ->
            generationCalls++
            generator.generate(type, difficulty)
        })
        try {
            session.start(GameType.Killer6x6, GameDifficulty.Easy) { result ->
                attempted += result
                payloads += parser.killerSudokuCagesToString(checkNotNull(result.killerCages))
                if (attempted.size == 1) error("Database unavailable")
            }
            session.state.first { it is GenerationState.SaveFailed }
            session.retry()
            assertEquals(GenerationState.Completed,
                session.state.first { it is GenerationState.Completed })
            assertEquals(1, generationCalls)
            assertEquals(2, attempted.size)
            assertSame(attempted[0], attempted[1])
            assertSame(attempted[0].killerCages, attempted[1].killerCages)
            assertEquals(payloads[0], payloads[1])
        } finally {
            session.cancel()
        }
    }

    @Test
    fun unspecifiedGameType_IsRejected() = generationTest {
        try {
            GamePuzzleGenerator(controller(123)).generate(GameType.Unspecified, GameDifficulty.Easy)
            fail("An unspecified game type must not silently select a variant")
        } catch (_: IllegalArgumentException) {
            // A caller must select an explicit supported variant.
        }
    }

    private fun assertValidCages(result: QQWingResult, type: GameType) {
        val cages = checkNotNull(result.killerCages) { "$type has no cages" }
        assertTrue(cages.isNotEmpty())
        assertEquals(cages.size, cages.map { it.id }.distinct().size)
        val coordinates = cages.flatMap { cage -> cage.cells.map { it.row to it.col } }
        assertEquals(type.size * type.size, coordinates.size)
        assertEquals((0 until type.size).flatMap { row ->
            (0 until type.size).map { col -> row to col }
        }.toSet(), coordinates.toSet())
        for (cage in cages) {
            assertTrue("Unexpected cage size ${cage.cells.size}", cage.cells.size in 1..5)
            val values = cage.cells.map { cell ->
                val value = result.solution[cell.row * type.size + cell.col]
                assertEquals(value, cell.value)
                value
            }
            assertEquals(values.size, values.distinct().size)
            assertEquals(values.sum(), cage.sum)
            assertConnected(cage)
        }
    }

    private fun assertConnected(cage: Cage) {
        val remaining = cage.cells.map { it.row to it.col }.toMutableSet()
        val connected = mutableSetOf(remaining.first())
        remaining.removeAll(connected)
        while (remaining.isNotEmpty()) {
            val neighbors = remaining.filter { candidate ->
                connected.any { cell -> abs(cell.first - candidate.first) + abs(cell.second - candidate.second) == 1 }
            }
            assertTrue("Disconnected cage ${cage.id}", neighbors.isNotEmpty())
            connected.addAll(neighbors)
            remaining.removeAll(neighbors.toSet())
        }
    }

    private fun controller(seed: Long) = QQWingControllerV2(
        random = Random(seed), maxGenerationMillis = 60_000
    )

    private fun generationTest(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        withTimeout(60_000, block)
    }

    private val variants = listOf(
        GameType.Killer6x6 to GameType.Default6x6,
        GameType.Killer9x9 to GameType.Default9x9,
        GameType.Killer12x12 to GameType.Default12x12
    )
}
