package com.kaajjo.libresudoku.core.generator.rating

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty

/**
 * HoDoKu-inspired APP policy, not a reproduction of HoDoKu's solver or a human-time estimate.
 * Initial weights and 9x9 thresholds come from HoDoKu 2.2.0 compiled defaults. Our catalogue and
 * selection order differ, and one charged move is one LogicalStep (not one elimination).
 * Recalibrating weights, thresholds or this step convention requires a new VERSION.
 *
 * Scores are reported for every supported geometry, but only ordinary 9x9/3x3 puzzles use
 * score-based promotion. The 6x6 label policy separates naked and hidden singles; other
 * sizes retain their existing technique-floor labels until separately calibrated.
 */
object RatingPolicy {
    const val VERSION = "libresudoku-effort-v1"
    const val SIX_BY_SIX_LABEL_VERSION = "libresudoku-6x6-labels-v1"
    const val EASY_MAX_SCORE = 800
    const val MODERATE_MAX_SCORE = 1000
    const val HARD_MAX_SCORE = 1600

    /**
     * Lists the cumulative passes in evaluation order.
     *
     * @param distinguishNakedSingles Whether a naked-singles-only pass precedes the combined singles pass.
     */
    fun activeTiers(distinguishNakedSingles: Boolean = false): List<LogicTier> =
        if (distinguishNakedSingles) listOf(
            LogicTier.NAKED_SINGLES, LogicTier.SINGLES, LogicTier.BASIC,
            LogicTier.ADVANCED, LogicTier.CHAINS
        ) else listOf(LogicTier.SINGLES, LogicTier.BASIC, LogicTier.ADVANCED, LogicTier.CHAINS)

    /**
     * Checks whether calibrated effort-score thresholds apply.
     *
     * @param geometry Grid and box dimensions; only ordinary 9x9 with 3x3 boxes uses thresholds.
     */
    fun usesScoreThresholds(geometry: SudokuGeometry): Boolean =
        geometry == SudokuGeometry(9, 3, 3)

    /**
     * A geometry-specific label change does not invalidate ratings for the other sizes.
     *
     * @param geometry Grid side length and rectangular box dimensions.
     */
    fun labelPolicyVersion(geometry: SudokuGeometry): String? =
        SIX_BY_SIX_LABEL_VERSION.takeIf { geometry == SudokuGeometry(6, 2, 3) }

    /**
     * Builds the persisted identifier for the score and geometry-specific label policy.
     *
     * @param geometry Grid and box dimensions used to choose the policy.
     */
    fun identifier(geometry: SudokuGeometry): String {
        val scorePolicy = VERSION + if (usesScoreThresholds(geometry)) ":9x9" else ":technique-only"
        return labelPolicyVersion(geometry)?.let { "$scorePolicy:$it" } ?: scorePolicy
    }

    /**
     * Exhaustive mapping: adding a technique cannot silently contribute zero points.
     *
     * @param technique Logical technique whose application contributes one move to the effort score.
     */
    fun weight(technique: Technique): Int = when (technique) {
        Technique.FULL_HOUSE, Technique.SINGLE -> 4
        Technique.HIDDEN_SINGLE_ROW, Technique.HIDDEN_SINGLE_COLUMN,
        Technique.HIDDEN_SINGLE_SECTION -> 14
        Technique.POINTING_PAIR_TRIPLE_ROW, Technique.POINTING_PAIR_TRIPLE_COLUMN,
        Technique.ROW_BOX, Technique.COLUMN_BOX -> 50
        Technique.NAKED_PAIR_ROW, Technique.NAKED_PAIR_COLUMN, Technique.NAKED_PAIR_SECTION -> 60
        Technique.HIDDEN_PAIR_ROW, Technique.HIDDEN_PAIR_COLUMN, Technique.HIDDEN_PAIR_SECTION -> 70
        Technique.NAKED_TRIPLE -> 80
        Technique.HIDDEN_TRIPLE -> 100
        Technique.NAKED_QUAD -> 120
        Technique.HIDDEN_QUAD -> 150
        Technique.X_WING -> 140
        Technique.SWORDFISH -> 150
        Technique.JELLYFISH -> 160
        Technique.XY_WING -> 160
        Technique.XYZ_WING -> 180
        Technique.W_WING -> 150
        Technique.SKYSCRAPER -> 130
        Technique.TWO_STRING_KITE -> 150
        Technique.FINNED_X_WING -> 130
        Technique.EMPTY_RECTANGLE -> 120
        Technique.COLOR_WRAP, Technique.COLOR_TRAP -> 150
        Technique.X_CHAIN, Technique.XY_CHAIN -> 260
        Technique.AIC -> 280
        Technique.ALS_XZ -> 300
    }

    /**
     * Explicit order also makes old stored v2 tier identifiers readable without ordinal tests.
     *
     * @param tier Logical tier to compare, including historical persisted identifiers.
     */
    internal fun strength(tier: LogicTier): Int = when (tier) {
        LogicTier.NAKED_SINGLES -> 0
        LogicTier.SINGLES -> 1
        LogicTier.PAIRS, LogicTier.LOCKED_CANDIDATES, LogicTier.BASIC -> 2
        LogicTier.ADVANCED -> 3
        LogicTier.CHAINS -> 4
    }

    internal fun stronger(current: LogicTier?, next: LogicTier): LogicTier =
        if (current == null || strength(next) > strength(current)) next else current

    /**
     * Score can promote a completed puzzle, but can never lower its technique floor.
     *
     * @param geometry Grid side length and rectangular box dimensions.
     * @param techniqueFloor Most demanding technique tier actually used in the completed pass.
     * @param effortScore Nonnegative weighted move total from that completed pass.
     * @param exposeSimple Whether naked-single-only puzzles may use the separate Simple category.
     */
    fun difficulty(
        geometry: SudokuGeometry,
        techniqueFloor: LogicTier,
        effortScore: Int,
        exposeSimple: Boolean = false
    ): GameDifficulty {
        require(effortScore >= 0)
        val floor = when (techniqueFloor) {
            LogicTier.NAKED_SINGLES -> if (exposeSimple) GameDifficulty.Simple else GameDifficulty.Easy
            // With five labels, hidden singles must remain Easy between Simple and BASIC.
            LogicTier.SINGLES -> if (!exposeSimple && labelPolicyVersion(geometry) != null) GameDifficulty.Moderate
                else GameDifficulty.Easy
            LogicTier.PAIRS, LogicTier.LOCKED_CANDIDATES, LogicTier.BASIC -> GameDifficulty.Moderate
            LogicTier.ADVANCED -> GameDifficulty.Hard
            LogicTier.CHAINS -> GameDifficulty.Challenge
        }
        if (!usesScoreThresholds(geometry)) return floor
        val scoreLevel = when {
            effortScore <= EASY_MAX_SCORE -> GameDifficulty.Easy
            effortScore <= MODERATE_MAX_SCORE -> GameDifficulty.Moderate
            effortScore <= HARD_MAX_SCORE -> GameDifficulty.Hard
            else -> GameDifficulty.Challenge
        }
        // A naked-single-only 9x9 board stays eligible for the optional Simple label.
        if (floor == GameDifficulty.Simple && scoreLevel == GameDifficulty.Easy) return floor
        return if (scoreLevel.ordinal > floor.ordinal) scoreLevel else floor
    }
}
