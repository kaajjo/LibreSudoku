package com.kaajjo.libresudoku.core.qqwing

/**
 * Clue-removal search hints, not definitions of difficulty or guaranteed generation times.
 *
 * @param ranges Target empty-cell ranges by requested difficulty; these guide search, not rating.
 * @param topK Positive number of highest-weight removal candidates considered at each step.
 * @param attemptLimits Positive candidate limits by difficulty; omitted entries use 250 attempts.
 */
class GenerationProfile(
    ranges: Map<GameDifficulty,IntRange>,
    val topK: Int=4,
    attemptLimits: Map<GameDifficulty,Int> = emptyMap()
) {
    private val ranges=ranges.toMap()
    private val attemptLimits=attemptLimits.toMap()
    init {
        require(topK>0)
        require(ranges.values.all { !it.isEmpty() && it.first>=0 && it.last<Int.MAX_VALUE })
        require(attemptLimits.values.all { it>0 })
    }
    /**
     * Returns the clue-removal target range for a requested category.
     *
     * @param difficulty Category with an explicitly registered range.
     */
    fun range(difficulty: GameDifficulty): IntRange = ranges[difficulty]
        ?: error("No generation profile for difficulty $difficulty")
    /**
     * Returns the candidate limit for a requested category.
     *
     * @param difficulty Category whose override is used; the default limit is 250.
     */
    fun attemptLimit(difficulty: GameDifficulty): Int = attemptLimits[difficulty] ?: 250
}

object ClassicGenerationProfiles {
    /**
     * Returns the production clue-removal profile for a classical variant.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     */
    fun forType(type: GameType): GenerationProfile {
        val ranges=when(type) {
            // Hard/Challenge remove clues until no further unique removal is possible.
            // A target of 36 is best-effort; it never permits a nonunique empty board.
            GameType.Default6x6 -> listOf(8..14,12..16,23..26,36..36,36..36,18..18)
            // BASIC techniques need a denser removal search; holes remain a search hint.
            GameType.Default9x9 -> listOf(20..34,34..43,48..56,46..56,52..62,45..45)
            GameType.Default12x12 -> listOf(40..60,55..75,72..94,88..108,100..120,85..85)
            else -> error("An explicit classical generation profile is required for $type")
        }
        return GenerationProfile(
            listOf(GameDifficulty.Simple,GameDifficulty.Easy,GameDifficulty.Moderate,
                GameDifficulty.Hard,GameDifficulty.Challenge,GameDifficulty.Unspecified).zip(ranges).toMap(),
            topK=if (type==GameType.Default6x6) 36 else 4,
            attemptLimits=if (type==GameType.Default6x6) mapOf(GameDifficulty.Challenge to 1000) else emptyMap()
        )
    }
}
