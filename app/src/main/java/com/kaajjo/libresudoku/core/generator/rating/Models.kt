package com.kaajjo.libresudoku.core.generator.rating

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Classical rectangular-box Sudoku. Bit masks deliberately cap the size at 25.
 *
 * @param size Grid side length in 1..25.
 * @param boxHeight Positive box height in cells; its product with boxWidth must equal size.
 * @param boxWidth Positive box width in cells; its product with boxHeight must equal size.
 */
data class SudokuGeometry(val size: Int, val boxHeight: Int, val boxWidth: Int) {
    init {
        require(size in 1..25) { "Supported side lengths are 1..25" }
        require(boxHeight > 0 && boxWidth > 0 && boxHeight.toLong() * boxWidth == size.toLong()) {
            "A rectangular box must contain exactly size cells"
        }
    }
    val cellCount: Int get() = size * size
}

/**
 * Cumulative, versioned APP policy, not a universal psychometric difficulty scale.
 * PAIRS and LOCKED_CANDIDATES are retained for persisted v2 records. Enum order is NOT
 * difficulty order; v3 passes are defined explicitly by [RatingPolicy.activeTiers].
 */
enum class LogicTier { NAKED_SINGLES, SINGLES, PAIRS, LOCKED_CANDIDATES, ADVANCED, CHAINS, BASIC }

enum class Technique(val tier: LogicTier) {
    SINGLE(LogicTier.NAKED_SINGLES),
    HIDDEN_SINGLE_ROW(LogicTier.SINGLES), HIDDEN_SINGLE_COLUMN(LogicTier.SINGLES),
    HIDDEN_SINGLE_SECTION(LogicTier.SINGLES),
    NAKED_PAIR_ROW(LogicTier.BASIC), NAKED_PAIR_COLUMN(LogicTier.BASIC), NAKED_PAIR_SECTION(LogicTier.BASIC),
    HIDDEN_PAIR_ROW(LogicTier.BASIC), HIDDEN_PAIR_COLUMN(LogicTier.BASIC), HIDDEN_PAIR_SECTION(LogicTier.BASIC),
    POINTING_PAIR_TRIPLE_ROW(LogicTier.BASIC),
    POINTING_PAIR_TRIPLE_COLUMN(LogicTier.BASIC),
    ROW_BOX(LogicTier.BASIC), COLUMN_BOX(LogicTier.BASIC),
    NAKED_TRIPLE(LogicTier.BASIC), HIDDEN_TRIPLE(LogicTier.BASIC),
    NAKED_QUAD(LogicTier.ADVANCED), HIDDEN_QUAD(LogicTier.ADVANCED),
    X_WING(LogicTier.ADVANCED), SWORDFISH(LogicTier.ADVANCED), JELLYFISH(LogicTier.ADVANCED),
    XY_WING(LogicTier.ADVANCED), XYZ_WING(LogicTier.ADVANCED), W_WING(LogicTier.ADVANCED),
    SKYSCRAPER(LogicTier.ADVANCED), TWO_STRING_KITE(LogicTier.ADVANCED),
    FINNED_X_WING(LogicTier.ADVANCED), EMPTY_RECTANGLE(LogicTier.ADVANCED),
    COLOR_WRAP(LogicTier.ADVANCED), COLOR_TRAP(LogicTier.ADVANCED),
    X_CHAIN(LogicTier.CHAINS), XY_CHAIN(LogicTier.CHAINS), AIC(LogicTier.CHAINS),
    ALS_XZ(LogicTier.CHAINS),
    FULL_HOUSE(LogicTier.NAKED_SINGLES)
}

enum class LogicStatus { SOLVED, STALLED, CONTRADICTION }
data class Candidate(val cell: Int, val value: Int)
enum class LinkKind { STRONG, WEAK }
/**
 * unit = -1 for an intra-cell link; otherwise rows 0..N-1, columns N..2N-1, boxes 2N..3N-1.
 *
 * @param from Candidate at the source of the inference.
 * @param to Candidate at the destination of the inference.
 * @param kind Strong or weak relationship used by the logical witness.
 * @param unit Unit index: rows 0..N-1, columns N..2N-1, boxes 2N..3N-1; -1 for an intra-cell link.
 */
data class InferenceLink(val from: Candidate, val to: Candidate, val kind: LinkKind, val unit: Int)
data class ColoredCandidate(val candidate: Candidate, val color: Int)

internal fun <T> frozen(items: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(items))

/**
 * A replayable logical operation. Evidence is a structured witness, not a localized UI sentence.
 * Eliminations includes the direct peer propagation of a placement; solved cells have mask zero.
 * A chain is ordered. Support cells are ordered for wings/ALS.
 *
 * @param technique Technique responsible for this move.
 * @param boardSize Grid side length used to derive row and column coordinates.
 * @param placements Values to place at zero-based cell indices.
 * @param eliminations Candidates to remove, including peer propagation caused by placements.
 * @param supportCells Ordered witness cells; their roles depend on the technique.
 * @param symbols One-based symbols participating in the witness.
 * @param units Witness unit indices: rows, then columns, then boxes.
 * @param links Ordered strong/weak inference links for a chain witness.
 * @param colors Candidate color assignments supporting a coloring deduction.
 * @param explanationKey Stable explanation identifier, defaulting to the technique name.
 * @param supportSplit Number of cells in the first ALS; splits supportCells into two ALS witnesses.
 */
class LogicalStep internal constructor(
    val technique: Technique,
    val boardSize: Int,
    placements: Collection<Candidate> = emptyList(),
    eliminations: Collection<Candidate> = emptyList(),
    supportCells: Collection<Int> = emptyList(),
    symbols: Collection<Int> = emptyList(),
    units: Collection<Int> = emptyList(),
    links: Collection<InferenceLink> = emptyList(),
    colors: Collection<ColoredCandidate> = emptyList(),
    val explanationKey: String = technique.name,
    /** ALS split point; other techniques leave this zero. */
    val supportSplit: Int = 0
) {
    val placements = frozen(placements)
    val eliminations = frozen(eliminations.distinct())
    val supportCells = frozen(supportCells)
    val symbols = frozen(symbols)
    val units = frozen(units)
    val links = frozen(links)
    val colors = frozen(colors)
    val position: Int get() = placements.firstOrNull()?.cell ?: supportCells.firstOrNull() ?: -1
    val value: Int get() = placements.firstOrNull()?.value ?: symbols.firstOrNull() ?: 0
    val row: Int get() = if (position < 0) -1 else position / boardSize
    val column: Int get() = if (position < 0) -1 else position % boardSize
    fun signature(): String = "$technique|$placements|$eliminations|$supportCells|$symbols|$units|$links|$colors|$supportSplit"
}

/**
 * A computational cap is NEVER converted into a completed rating.
 *
 * @param operations Number of operations completed before the evaluation budget was exhausted.
 */
class EvaluationBudgetExceeded(val operations: Long) : RuntimeException("Logical evaluation work budget exhausted at $operations")

/**
 * Limits the supported logical search and its computational work.
 *
 * @param maxChainLinks Maximum chain length in 3..64 links.
 * @param maxAlsSize Maximum cells per disjoint almost-locked set, in 1..4.
 * @param maxOperations Positive shared operation budget across all passes of one evaluation.
 */
data class EvaluationLimits(
    val maxChainLinks: Int = 12,
    /** Disjoint ALS-XZ only. Raising this can be very expensive. */
    val maxAlsSize: Int = 3,
    val maxOperations: Long = 10_000_000L
) {
    init {
        require(maxChainLinks in 3..64)
        require(maxAlsSize in 1..4)
        require(maxOperations > 0)
    }
}

internal class EvaluationControl(val limits: EvaluationLimits, private val checkpoint: () -> Unit) {
    var operations: Long = 0L; private set
    fun check() = checkpoint()
    fun tick() {
        if (operations >= limits.maxOperations) throw EvaluationBudgetExceeded(operations)
        operations++
        if (operations and 127L == 0L) checkpoint()
    }
}

/**
 * Immutable snapshot of one completed, stalled or contradictory logical pass.
 *
 * @param geometry Grid and box dimensions.
 * @param tier Cumulative technique catalogue enabled in this pass.
 * @param status Termination status of this pass.
 * @param board Final row-major values; copied on construction.
 * @param candidates Final per-cell candidate bit masks; copied on construction.
 * @param steps Applied steps when trace capture is enabled; otherwise empty.
 * @param counts Technique usage counts indexed by Technique.ordinal; copied on construction.
 * @param operations Cumulative evaluation work, including earlier passes sharing the same control.
 * @param effortScore Weighted move total from this pass only.
 * @param techniqueFloor Most demanding technique used in this pass, or null when no move was applied.
 */
class LogicReport internal constructor(
    val geometry: SudokuGeometry,
    val tier: LogicTier,
    val status: LogicStatus,
    board: IntArray,
    candidates: IntArray,
    steps: List<LogicalStep>,
    counts: IntArray,
    val operations: Long,
    /** Weighted logical moves in THIS pass, excluding work in earlier stalled passes. */
    val effortScore: Int,
    /** Most demanding technique actually applied in THIS pass; null when there were no moves. */
    val techniqueFloor: LogicTier?
) {
    private val board = board.copyOf()
    private val candidates = candidates.copyOf()
    private val uses = counts.copyOf()
    val steps: List<LogicalStep> = frozen(steps)
    val remainingCells: Int = board.count { it == 0 }
    val moveCount: Int = uses.sum()
    fun solutionSnapshot(): IntArray = board.copyOf()
    fun candidateMasksSnapshot(): IntArray = candidates.copyOf()
    fun uses(technique: Technique): Int = uses[technique.ordinal]
    fun counts(): Map<Technique, Int> = Collections.unmodifiableMap(Technique.values().associateWith { uses(it) })
}

sealed class LogicalRating {
    data class Rated(val report: LogicReport, val lowerTiersStalled: List<LogicTier>) : LogicalRating()
    /**
     * STALLED under the configured catalogue/chain/ALS ceilings, not proof of a need to guess.
     *
     * @param report Final stalled pass under the configured catalogue and evaluation limits.
     */
    data class BeyondSupported(val report: LogicReport) : LogicalRating()
    data class InvalidInput(val reason: String) : LogicalRating()
    data class Contradiction(val report: LogicReport) : LogicalRating()
    data class AlreadySolved(val report: LogicReport) : LogicalRating()
}

/**
 * Immutable topology, never exposed outside the core. Mutable candidate state is NEVER cached.
 *
 * @param geometry Grid side length and rectangular box dimensions.
 */
internal class Topology(val geometry: SudokuGeometry) {
    val n = geometry.size
    val cells = geometry.cellCount
    val fullMask = (1 shl n) - 1
    val row = IntArray(cells) { it / n }
    val col = IntArray(cells) { it % n }
    val box = IntArray(cells) { row[it] / geometry.boxHeight * (n / geometry.boxWidth) + col[it] / geometry.boxWidth }
    val units = Array(3 * n) { u -> when {
        u < n -> IntArray(n) { u * n + it }
        u < 2 * n -> IntArray(n) { it * n + u - n }
        else -> (0 until cells).filter { box[it] == u - 2 * n }.toIntArray()
    } }
    val cellUnits = Array(cells) { intArrayOf(row[it], n + col[it], 2 * n + box[it]) }
    fun sees(a: Int, b: Int): Boolean = a != b && (row[a] == row[b] || col[a] == col[b] || box[a] == box[b])
    val peers = Array(cells) { a -> (0 until cells).filter { sees(a, it) }.toIntArray() }
    fun sharedUnit(a: Int, b: Int): Int = when {
        row[a] == row[b] -> row[a]
        col[a] == col[b] -> n + col[a]
        else -> 2 * n + box[a]
    }
    companion object {
        private val cache = ConcurrentHashMap<SudokuGeometry, Topology>()
        fun of(g: SudokuGeometry): Topology {
            cache[g]?.let { return it }
            val created = Topology(g)
            return cache.putIfAbsent(g, created) ?: created
        }
    }
}

internal fun bit(value: Int): Int = 1 shl (value - 1)
internal fun population(mask: Int): Int = Integer.bitCount(mask)
internal fun firstValue(mask: Int): Int = Integer.numberOfTrailingZeros(mask) + 1
internal fun values(mask: Int): List<Int> {
    val out = ArrayList<Int>(); var m = mask
    while (m != 0) { out += firstValue(m); m = m and (m - 1) }
    return out
}

/**
 * Iterative lexicographic combinations, a single reused index buffer, early exit on success.
 *
 * @param count Number of available elements, indexed from zero.
 * @param k Number of distinct indices in each combination.
 * @param control Operation budget and cooperative cancellation checks shared by this evaluation.
 * @param visit Receives a reused index buffer; return true to stop enumeration. Copy it before retaining it.
 */
internal inline fun combinations(count: Int, k: Int, control: EvaluationControl, visit: (IntArray) -> Boolean): Boolean {
    if (k > count || k <= 0) return false
    val indices = IntArray(k) { it }
    while (true) {
        control.tick()
        if (visit(indices)) return true
        var p = k - 1
        while (p >= 0 && indices[p] == count - k + p) p--
        if (p < 0) return false
        indices[p]++
        for (j in p + 1 until k) indices[j] = indices[j - 1] + 1
    }
}
