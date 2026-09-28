package com.kaajjo.libresudoku.qqwing

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyQQWingIsolationTest {
    private val types = listOf(GameType.Default6x6, GameType.Default9x9, GameType.Default12x12)

    private fun solution(type: GameType) = IntArray(type.size * type.size) { cell ->
        val row = cell / type.size
        (row * type.sectionWidth + row / type.sectionHeight + cell % type.size) % type.size + 1
    }

    @Test
    fun interleavedSizesKeepGeometryAndHistoryCoordinates() {
        val solvers = types.map { type ->
            QQWing(type, GameDifficulty.Unspecified).apply {
                setRandom(type.size)
                assertTrue(setPuzzle(solution(type).also { it[it.lastIndex] = 0 }))
            }
        }
        solvers.zip(types).forEach { (solver, type) ->
            assertTrue(solver.solve())
            assertArrayEquals(solution(type), solver.solution)
            val lastCellStep = solver.getSolveInstructions().filterNotNull()
                .last { it.position == type.size * type.size - 1 }
            // A new solver must not change coordinates in an already recorded history.
            QQWing(GameType.Default9x9, GameDifficulty.Unspecified)
            assertEquals(type.size, lastCellStep.row)
            assertEquals(type.size, lastCellStep.column)
        }
    }

    @Test
    fun seededSolvesRemainIndependentAcrossParallelRequests() {
        fun solve(type: GameType, seed: Int): IntArray {
            return QQWing(type, GameDifficulty.Unspecified).apply {
                setRandom(seed)
                assertTrue(setPuzzle(IntArray(type.size * type.size)))
                assertTrue(solve())
            }.solution
        }
        val requests = (0 until 18).map { types[it % types.size] to it }
        val baseline = requests.map { (type, seed) -> solve(type, seed) }
        val executor = Executors.newFixedThreadPool(6)
        try {
            val results = executor.invokeAll(requests.map { (type, seed) ->
                Callable { solve(type, seed) }
            })
            results.forEachIndexed { index, future ->
                assertArrayEquals(baseline[index], future.get())
            }
        } finally {
            executor.shutdownNow()
        }
    }
}
