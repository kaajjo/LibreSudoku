package com.kaajjo.libresudoku.generation

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaajjo.libresudoku.core.generator.dlx.DlxEngine
import com.kaajjo.libresudoku.core.generator.rating.LogicalRating
import com.kaajjo.libresudoku.core.generator.rating.QqWingLogicalRater
import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import com.kaajjo.libresudoku.core.qqwing.QQWingControllerV2
import com.kaajjo.libresudoku.core.qqwing.Symmetry
import com.kaajjo.libresudoku.core.qqwing.models.SolveResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/** Small ART integration checks. Timings are diagnostics for the device running this test, not an SLA. */
@RunWith(AndroidJUnit4::class)
class ClassicGenerationAndroidTest {
    @Test
    fun legacyRandomSymmetryWorksOnSupportedAndroidVersions() = androidTest {
        withContext(Dispatchers.Default) {
            val solver = QQWing(GameType.Default9x9, GameDifficulty.Unspecified)
            solver.setRandom(42)
            assertTrue(solver.generatePuzzleSymmetry(Symmetry.RANDOM))
            assertTrue(solver.solve())
            assertValidSolution(solver.solution, GameType.Default9x9)
            assertPreservesClues(solver.puzzle, solver.solution)
        }
    }

    @Test
    fun generate6x6AndVerifySolutionOnArt() = androidTest {
        generateAndVerify(GameType.Default6x6)
    }

    @Test
    fun generate9x9AndVerifySolutionOnArt() = androidTest {
        generateAndVerify(GameType.Default9x9)
    }

    @Test
    fun generate12x12AndVerifySolutionOnArt() = androidTest {
        generateAndVerify(GameType.Default12x12)
    }

    @Test
    fun cancelDlxDuringSearchAndReuseTheEngineOnArt() = androidTest {
        lateinit var engine: DlxEngine
        var cancelledDuringSearch = false
        try {
            withContext(Dispatchers.Default) {
                val context = currentCoroutineContext()
                engine = DlxEngine(12, 3, 4, Random(42))
                engine.solve(IntArray(144), maxSolutions = 2, checkpoint = {
                    // Search checkpoints run every 128 nodes. A 12x12 solution needs at least
                    // 145 nodes, so this checkpoint is reached before the first solution.
                    if (engine.searchNodes >= 129) {
                        cancelledDuringSearch = true
                        checkNotNull(context[Job]).cancel(CancellationException("ART DLX cancellation"))
                    }
                    context.ensureActive()
                })
                fail("DLX returned a result after cancellation")
            }
        } catch (_: CancellationException) {
            // A timeout is not a successful cooperative-cancellation test.
            currentCoroutineContext().ensureActive()
        }
        assertTrue(cancelledDuringSearch)
        assertEquals(129L, engine.searchNodes)
        assertEquals(0, engine.solutionCount)
        assertTrue(engine.solvedBoard.all { it == 0 })
        withContext(Dispatchers.Default) {
            val context = currentCoroutineContext()
            val checkpoint = { context.ensureActive() }
            assertEquals(1, engine.solve(IntArray(144), maxSolutions = 1, checkpoint = checkpoint))
            val fresh = DlxEngine(12, 3, 4, Random(42), checkpoint)
            assertEquals(1, fresh.solve(IntArray(144), maxSolutions = 1, checkpoint = checkpoint))
            assertArrayEquals(fresh.solvedBoard, engine.solvedBoard)
            assertValidSolution(engine.solvedBoard, GameType.Default12x12)
        }
    }

    @Test
    fun cancelRaterAfterLogicalStepAndRateAgainOnArt() = androidTest {
        val rater = QqWingLogicalRater()
        val board = CLASSIC_9X9.map { it.digitToInt() }.toIntArray()
        val geometry = SudokuGeometry(9, 3, 3)
        var appliedSteps = 0
        try {
            withContext(Dispatchers.Default) {
                val context = currentCoroutineContext()
                rater.rateObserved(
                    board = board,
                    geometry = geometry,
                    distinguishNakedSingles = false,
                    captureSteps = true,
                    checkpoint = { context.ensureActive() },
                    observer = { _, _, _, _ ->
                        appliedSteps++
                        checkNotNull(context[Job]).cancel(CancellationException("ART rater cancellation"))
                    }
                )
                fail("Rater returned an assessment after cancellation")
            }
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
        }
        assertEquals(1, appliedSteps)
        assertArrayEquals(CLASSIC_9X9.map { it.digitToInt() }.toIntArray(), board)
        withContext(Dispatchers.Default) {
            val context = currentCoroutineContext()
            val rating = rater.rate(board, geometry, checkpoint = { context.ensureActive() })
            assertTrue(rating is LogicalRating.Rated)
            val solution = (rating as LogicalRating.Rated).report.solutionSnapshot()
            assertValidSolution(solution, GameType.Default9x9)
            assertPreservesClues(board, solution)
        }
    }

    private suspend fun generateAndVerify(type: GameType) {
        val controller = QQWingControllerV2(
            random = Random(42),
            maxGenerationAttempts = 3,
            maxGenerationMillis = 5_000
        )
        val started = SystemClock.elapsedRealtimeNanos()
        val generated = controller.generate(type, GameDifficulty.Easy)
        val generationFinished = SystemClock.elapsedRealtimeNanos()
        assertEquals(type.size * type.size, generated.puzzle.size)
        assertTrue(generated.puzzle.any { it == 0 })
        assertEquals(1, generated.solutionCount)
        assertNotEquals(GameDifficulty.Unspecified, generated.difficulty)
        val metadata = generated.ratingMetadata
        assertNotNull(metadata)
        assertTrue(checkNotNull(metadata).logicallySolved)
        assertTrue(metadata.version.startsWith(QqWingLogicalRater.RATING_VERSION))
        assertValidSolution(generated.solution, type)
        assertPreservesClues(generated.puzzle, generated.solution)

        val puzzleBeforeSolve = generated.puzzle.copyOf()
        val solved = controller.solve(generated.puzzle, type)
        assertTrue(solved is SolveResult.Success)
        solved as SolveResult.Success
        assertEquals(1, solved.solutionCount)
        assertArrayEquals(generated.solution, solved.solution)
        assertArrayEquals(puzzleBeforeSolve, generated.puzzle)
        val finished = SystemClock.elapsedRealtimeNanos()
        Log.i(
            LOG_TAG,
            "ART smoke type=${type.name}, requested=Easy, actual=${generated.difficulty.name}, " +
                "generationMs=${(generationFinished - started) / 1_000_000}, " +
                "verificationMs=${(finished - generationFinished) / 1_000_000}, " +
                "device=${Build.MODEL}, api=${Build.VERSION.SDK_INT}; diagnostic only, not a performance SLA"
        )
    }

    private fun assertValidSolution(board: IntArray, type: GameType) {
        val size = type.size
        assertEquals(size * size, board.size)
        val symbols = (1..size).toList()
        for (row in 0 until size) {
            assertEquals(symbols, (0 until size).map { board[row * size + it] }.sorted())
        }
        for (column in 0 until size) {
            assertEquals(symbols, (0 until size).map { board[it * size + column] }.sorted())
        }
        for (boxRow in 0 until size step type.sectionHeight) {
            for (boxColumn in 0 until size step type.sectionWidth) {
                val values = buildList {
                    for (row in boxRow until boxRow + type.sectionHeight) {
                        for (column in boxColumn until boxColumn + type.sectionWidth) {
                            add(board[row * size + column])
                        }
                    }
                }
                assertEquals(symbols, values.sorted())
            }
        }
    }

    private fun assertPreservesClues(puzzle: IntArray, solution: IntArray) {
        for (cell in puzzle.indices) {
            if (puzzle[cell] != 0) assertEquals(puzzle[cell], solution[cell])
        }
    }

    private fun androidTest(block: suspend () -> Unit) = runBlocking {
        withTimeout(15_000) { block() }
    }

    private companion object {
        const val LOG_TAG = "ClassicGenerationART"
        const val CLASSIC_9X9 =
            "530070000600195000098000060800060003400803001700020006060000280000419005000080079"
    }
}
