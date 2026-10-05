package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Routes the variants offered by the app without widening the classical solver's contract.
 * Killer retains the app's existing format: a unique classical puzzle with givens, plus cages
 * derived from its solution. Its difficulty describes that classical base, not cage techniques.
 *
 * @param classicalController Generator for the unique classical base, including the base used for Killer cages.
 */
class GamePuzzleGenerator(
    private val classicalController: QQWingControllerV2 = QQWingControllerV2()
) {
    /**
     * Generates the requested variant, adding solution-derived cages for Killer puzzles.
     *
     * @param type Concrete classical or Killer variant; Unspecified is rejected.
     * @param targetDifficulty Required category of the classical base; Unspecified accepts any completed rating.
     */
    suspend fun generate(type: GameType, targetDifficulty: GameDifficulty): QQWingResult {
        val baseType = when (type) {
            GameType.Killer6x6 -> GameType.Default6x6
            GameType.Killer9x9 -> GameType.Default9x9
            GameType.Killer12x12 -> GameType.Default12x12
            GameType.Default6x6, GameType.Default9x9, GameType.Default12x12 -> type
            GameType.Unspecified -> throw IllegalArgumentException("Concrete game type required")
        }
        val base = classicalController.generate(baseType, targetDifficulty)
        if (baseType == type) return base

        return withContext(Dispatchers.Default) {
            currentCoroutineContext().ensureActive()
            val solution = List(type.size) { row ->
                List(type.size) { col -> Cell(row, col, base.solution[row * type.size + col]) }
            }
            val cages = CageGenerator(solution, type).generate(2, 5)
            currentCoroutineContext().ensureActive()
            base.copy(
                killerCages = cages,
                ratingMetadata = base.ratingMetadata?.let {
                    it.copy(version = "killer-classic-base-v1:${it.version}")
                }
            )
        }
    }
}
