package com.kaajjo.libresudoku.core.generator.dlx

import com.kaajjo.libresudoku.core.qqwing.ClassicGameTypes
import com.kaajjo.libresudoku.core.qqwing.GameType
import kotlin.random.Random

object DlxGenerator {
    /**
     * Generates a unique classical puzzle.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     * @param targetEmptyCells Nonnegative desired hole count; uniqueness may prevent reaching it.
     * @param random Random source owned by this request.
     * @param topK Positive candidate pool size for clue removal.
     */
    fun generate(type: GameType, targetEmptyCells: Int, random: Random=Random.Default, topK: Int=4): IntArray =
        generateWithSolution(type,targetEmptyCells,random,topK).puzzle

    /**
     * Generates a unique classical puzzle together with its full solution.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     * @param targetEmptyCells Nonnegative desired hole count; uniqueness may prevent reaching it.
     * @param random Random source owned by this request.
     * @param topK Positive candidate pool size for clue removal.
     * @param maxSearchNodesPerCheck Positive node budget per DLX search.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     */
    fun generateWithSolution(
        type: GameType, targetEmptyCells: Int, random: Random=Random.Default, topK: Int=4,
        maxSearchNodesPerCheck: Long=2_000_000L, checkpoint: () -> Unit={}
    ): GeneratedPuzzle {
        return ClassicPuzzleGenerator.generate(ClassicGameTypes.geometryOf(type),
            targetEmptyCells,random,topK,maxSearchNodesPerCheck,checkpoint)
    }
}
