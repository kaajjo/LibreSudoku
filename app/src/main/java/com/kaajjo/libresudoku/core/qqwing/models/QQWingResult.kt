package com.kaajjo.libresudoku.core.qqwing.models

import com.kaajjo.libresudoku.core.generator.rating.LogicTier
import com.kaajjo.libresudoku.core.generator.rating.Technique
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty

/**
 * Persisted description of the rating assigned to a puzzle.
 *
 * @param version Evaluator and label-policy identifier; retained verbatim on restore.
 * @param tier Cumulative logical pass used for the assessment.
 * @param techniqueCounts Counts keyed by stable technique names during serialization.
 * @param logicallySolved Whether the assessment completed a logical solution.
 * @param effortScore Nonnegative weighted score, or null when not recorded.
 * @param scorePolicy Identifier defining the score interpretation, or null when not recorded.
 */
data class RatingMetadata(
    val version: String,
    val tier: LogicTier,
    val techniqueCounts: Map<Technique,Int>,
    val logicallySolved: Boolean = true,
    /** Absent for historical ratings; a score must come from an actual rating pass. */
    val effortScore: Int? = null,
    val scorePolicy: String? = null
) {
    init {
        require(effortScore == null || effortScore >= 0) { "Effort score must be nonnegative" }
    }
}

/**
 * Arrays are OWNED by the recipient.
 *
 * @param puzzle Row-major givens, with zero for empty cells.
 * @param solution Full solution in the same row-major order.
 * @param difficulty Category assigned to this puzzle by its evaluator.
 * @param solutionCount Count capped at two; two means at least two solutions.
 * @param ratingMetadata Completed logical assessment details, or null when unavailable.
 * @param killerCages Cages derived from the solution for a Killer variant; null for a classical puzzle.
 */
data class QQWingResult(
    val puzzle: IntArray,
    val solution: IntArray,
    val difficulty: GameDifficulty,
    /** Saturated at 2: 2 means "at least two", not an exact count. */
    val solutionCount: Int,
    val ratingMetadata: RatingMetadata?=null,
    val killerCages: List<Cage>? = null
)
