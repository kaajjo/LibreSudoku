package com.kaajjo.libresudoku.core.qqwing
import android.util.Log
import com.kaajjo.libresudoku.core.generator.dlx.DlxGenerator
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.qqwing.models.SolveResult
import kotlinx.coroutines.*
import kotlin.random.Random

class QQWingControllerV2 {

    suspend fun generate(
        type: GameType,
        targetDifficulty: GameDifficulty
    ): QQWingResult = withContext(Dispatchers.Default) {

        // Эвристика: сколько клеток стирать в зависимости от размера и сложности
        // Это нужно будет подогнать под себя экспериментально
        val targetEmpty = when(type) {
            GameType.Default12x12 -> when(targetDifficulty) {
                GameDifficulty.Easy -> 65
                GameDifficulty.Moderate -> 85
                GameDifficulty.Hard -> 100
                else -> 80
            }
            else -> 45 // Заглушка для 9x9
        }

        while (isActive) {
            // 1. DLX Генерирует сетку за миллисекунды
            val rawPuzzle = DlxGenerator.generate(type, targetEmpty)

            // 2. Старый QQWing (очищенный до роли оценщика) решает её и выдает вердикт
            val evaluator = QQWing(type, GameDifficulty.Unspecified)
            evaluator.setPuzzle(rawPuzzle)
            evaluator.setRecordHistory(true)
            evaluator.solve()

            val actualDifficulty = evaluator.getDifficulty()

            // 3. Бинго?
            if (targetDifficulty == GameDifficulty.Unspecified || actualDifficulty == targetDifficulty) {
                return@withContext QQWingResult(
                    puzzle = rawPuzzle,
                    solution = evaluator.solution,
                    difficulty = actualDifficulty,
                    solutionCount = 1
                )
            }
        }
        throw CancellationException("Generation was cancelled")
    }

    /**
     * Решает переданное судоку.
     */
    suspend fun solve(
        gameBoard: IntArray,
        type: GameType
    ): SolveResult = withContext(Dispatchers.Default) {
        val solver = createQQWing(type, GameDifficulty.Unspecified)
        val isValidPuzzle = solver.setPuzzle(gameBoard)

        if (!isValidPuzzle) {
            return@withContext SolveResult.Impossible
        }

        val solutionCount = solver.countSolutionsLimited()
        if (solver.solve()) {
            return@withContext SolveResult.Success(
                solution = solver.solution,
                solutionCount = solutionCount
            )
        } else {
            return@withContext SolveResult.Impossible
        }
    }

    // Фабричный метод без побочных эффектов
    private fun createQQWing(type: GameType, difficulty: GameDifficulty): QQWing {
        val qqwing = QQWing(type, difficulty)
        // Настраиваем только то, что действительно нужно для генерации/решения
        qqwing.setRecordHistory(difficulty != GameDifficulty.Unspecified)
        qqwing.setLogHistory(false)
        return qqwing
    }
}
