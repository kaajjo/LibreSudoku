package com.kaajjo.libresudoku.core.generator.rating

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.ClassicGameTypes

/**
 * Combines the logical report with the application-facing category and policy identifiers.
 *
 * @param difficulty Application difficulty, or null when the input has no usable completed rating.
 * @param rating Detailed logical outcome.
 * @param ratingVersion Evaluator and geometry-specific label-policy identifier.
 * @param effortScore Completed-pass score, or null for outcomes without a usable rating.
 * @param scorePolicy Score-policy identifier, present only for completed ratings.
 */
data class ClassicAssessment(
    val difficulty: GameDifficulty?,
    val rating: LogicalRating,
    val ratingVersion: String,
    /** Only a completed, nonempty logical solution has a usable difficulty score. */
    val effortScore: Int? = null,
    val scorePolicy: String? = null
)

/**
 * Adapter to the application's existing enums. This does NOT call legacy QQWing.
 * A distinct Killer evaluator needs cage membership, sums and rules; a size alone is insufficient.
 *
 * @param exposeSimple Whether naked-single-only puzzles may use the separate Simple category.
 * @param captureSteps Whether to retain the ordered logical steps in the returned report.
 * @param limits Chain length, ALS size and operation limits for logical evaluation.
 */
class ClassicDifficultyAdapter(
    private val exposeSimple: Boolean = false,
    private val captureSteps: Boolean = false,
    limits: EvaluationLimits = EvaluationLimits()
) {
    private val rater = QqWingLogicalRater(limits)
    /** Shared rating version; [ratingVersion] with a type adds its geometry-specific policy. */
    val ratingVersion: String = rater.version +
        (if (exposeSimple) ":five-labels" else ":four-labels")

    /**
     * Returns the evaluator version including the label policy for this geometry.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     */
    fun ratingVersion(type: GameType): String =
        RatingPolicy.labelPolicyVersion(ClassicGameTypes.geometryOf(type))
            ?.let { "$ratingVersion:$it" } ?: ratingVersion

    /**
     * PRECONDITION: the caller has independently established that the puzzle has exactly one
     * solution under classical row/column/box constraints (e.g. by the DLX generator).
     * This method trusts, rather than checks, that precondition. For imported puzzles, run DLX first.
     * Budget/cancellation exceptions from checkpoint propagate; they are never converted to a tier.
     *
     * @param type Explicit classical variant; Killer and Unspecified are rejected.
     * @param puzzle Row-major givens, with zero for empty cells, already independently proven unique.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     */
    fun evaluateKnownUnique(
        type: GameType,
        puzzle: IntArray,
        checkpoint: () -> Unit = {}
    ): ClassicAssessment {
        val geometry = ClassicGameTypes.geometryOf(type)
        val raw = rater.rate(
            puzzle,
            geometry,
            distinguishNakedSingles = exposeSimple,
            captureSteps = captureSteps,
            checkpoint = checkpoint
        )
        val difficulty = when (raw) {
            is LogicalRating.Rated -> RatingPolicy.difficulty(
                geometry, checkNotNull(raw.report.techniqueFloor), raw.report.effortScore, exposeSimple
            )
            is LogicalRating.BeyondSupported,
            is LogicalRating.AlreadySolved,
            is LogicalRating.InvalidInput,
            is LogicalRating.Contradiction -> null
        }
        return ClassicAssessment(
            difficulty, raw, ratingVersion(type),
            effortScore = (raw as? LogicalRating.Rated)?.report?.effortScore,
            scorePolicy = if (raw is LogicalRating.Rated) RatingPolicy.identifier(geometry) else null
        )
    }
}
