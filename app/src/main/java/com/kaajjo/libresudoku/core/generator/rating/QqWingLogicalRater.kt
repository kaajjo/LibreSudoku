package com.kaajjo.libresudoku.core.generator.rating

/**
 * Independent of the mutable legacy QQWing class. Every stronger pass starts from the ORIGINAL givens.
 * A completed trace proves one solution and, since all rules preserve ALL completions, uniqueness
 * if the input was satisfiable. A stalled trace alone proves neither solvability nor uniqueness.
 *
 * Stateless/thread-safe; input must remain stable while its entry snapshot is copied.
 * checkpoint is cooperative and must not swallow cancellation. It must not reenter mutable engines.
 *
 * @param limits Chain length, ALS size and operation limits for logical evaluation.
 */
class QqWingLogicalRater(private val limits: EvaluationLimits = EvaluationLimits()) {
    companion object { const val RATING_VERSION="libresudoku-logic-v3.0.0" }
    val version: String = "$RATING_VERSION:chain${limits.maxChainLinks}:als${limits.maxAlsSize}:${RatingPolicy.VERSION}"

    /**
     * Evaluates independent logical passes from the original givens.
     *
     * @param board Row-major givens, with zero for empty cells and 1..N for values. Must remain stable until its entry snapshot is copied.
     * @param geometry Grid and box dimensions matching the board.
     * @param distinguishNakedSingles Whether to try a naked-singles-only pass before combined singles.
     * @param captureSteps Whether to retain the ordered logical steps in the result.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @return A completed rating, a stalled report, or an explicit invalid/contradictory/already-solved result.
     * @throws EvaluationBudgetExceeded if the operation budget is exhausted; no rating is produced.
     */
    fun rate(
        board: IntArray,
        geometry: SudokuGeometry,
        distinguishNakedSingles: Boolean = false,
        captureSteps: Boolean = false,
        checkpoint: () -> Unit = {}
    ): LogicalRating = rateObserved(board,geometry,distinguishNakedSingles,captureSteps,checkpoint,null)

    /**
     * Test hook: full state snapshots after each move, not an input oracle for technique selection.
     *
     * @param board Row-major givens, with zero for empty cells; copied before evaluation.
     * @param geometry Grid side length and rectangular box dimensions.
     * @param distinguishNakedSingles Whether to try a naked-singles-only pass before the combined singles pass.
     * @param captureSteps Whether to retain the ordered logical steps in the returned report.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @param observer Optional callback after each applied move, receiving the tier, step and independent board/mask snapshots.
     */
    internal fun rateObserved(
        board: IntArray, geometry: SudokuGeometry, distinguishNakedSingles: Boolean,
        captureSteps: Boolean, checkpoint: () -> Unit,
        observer: ((LogicTier,LogicalStep,IntArray,IntArray) -> Unit)?
    ): LogicalRating {
        checkpoint()
        if (board.size!=geometry.cellCount) return LogicalRating.InvalidInput("Wrong board length")
        val input=board.copyOf()
        if (input.any { it !in 0..geometry.size }) return LogicalRating.InvalidInput("Values must be in 0..size")
        val t=Topology.of(geometry); val control=EvaluationControl(limits,checkpoint)
        val tiers=RatingPolicy.activeTiers(distinguishNakedSingles)
        val stalled=ArrayList<LogicTier>(); var last: LogicReport?=null
        for (tier in tiers) {
            control.check()
            val state=LogicalState(t,control)
            if (!state.initialize(input)) return LogicalRating.InvalidInput("Conflicting givens")
            val report=run(state,tier,captureSteps,observer)
            when (report.status) {
                LogicStatus.SOLVED -> return if (input.none { it==0 }) LogicalRating.AlreadySolved(report)
                    else LogicalRating.Rated(report,frozen(stalled))
                LogicStatus.CONTRADICTION -> return LogicalRating.Contradiction(report)
                LogicStatus.STALLED -> { stalled+=tier; last=report }
            }
        }
        return LogicalRating.BeyondSupported(checkNotNull(last))
    }

    /**
     * Runs one cumulative technique tier against an initialized logical state.
     *
     * @param s Mutable candidate state owned by this pass.
     * @param tier Maximum technique catalogue enabled for the pass.
     * @param captureSteps Whether to retain the applied steps.
     * @param observer Optional callback after each move with independent board and candidate-mask snapshots.
     */
    internal fun run(
        s: LogicalState, tier: LogicTier, captureSteps: Boolean,
        observer: ((LogicTier,LogicalStep,IntArray,IntArray) -> Unit)?=null
    ): LogicReport {
        val counts=IntArray(Technique.values().size); val steps=ArrayList<LogicalStep>()
        var effortScore=0
        var techniqueFloor: LogicTier?=null
        fun report(status: LogicStatus)=LogicReport(
            s.topology.geometry,tier,status,s.board,s.masks,steps,counts,s.control.operations,
            effortScore,techniqueFloor
        )
        while (true) {
            s.control.check()
            if (s.contradiction()) return report(LogicStatus.CONTRADICTION)
            if (s.board.all { it!=0 }) return report(LogicStatus.SOLVED)
            val step=nextStep(s,tier) ?: return report(LogicStatus.STALLED)
            s.apply(step); counts[step.technique.ordinal]++
            effortScore+=RatingPolicy.weight(step.technique)
            techniqueFloor=RatingPolicy.stronger(techniqueFloor,step.technique.tier)
            if (captureSteps) steps+=step
            observer?.invoke(tier,step,s.board.copyOf(),s.masks.copyOf())
        }
    }

    /**
     * Finds the first available deduction in the configured technique order.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param tier Cumulative technique tier to search.
     * @return A supported deduction, or null if this tier cannot progress.
     */
    internal fun nextStep(s: LogicalState,tier: LogicTier): LogicalStep? {
        BasicTechniques.singles(s,tier!=LogicTier.NAKED_SINGLES)?.let { return it }
        if (tier==LogicTier.NAKED_SINGLES || tier==LogicTier.SINGLES) return null
        // Retain the old internal passes for replay/debug callers, but do not rate with them.
        if (tier!=LogicTier.PAIRS) BasicTechniques.locked(s)?.let { return it }
        BasicTechniques.subset(s,2,false)?.let { return it }
        BasicTechniques.subset(s,2,true)?.let { return it }
        if (tier==LogicTier.PAIRS || tier==LogicTier.LOCKED_CANDIDATES) return null
        BasicTechniques.subset(s,3,false)?.let { return it }
        BasicTechniques.subset(s,3,true)?.let { return it }
        if (tier==LogicTier.BASIC) return null
        BasicTechniques.subset(s,4,false)?.let { return it }
        BasicTechniques.subset(s,4,true)?.let { return it }
        FishTechniques.basic(s,2)?.let { return it }
        WingTechniques.xyOrXyz(s,false)?.let { return it }
        WingTechniques.xyOrXyz(s,true)?.let { return it }
        SingleDigitPatterns.shortChain(s,false)?.let { return it }
        SingleDigitPatterns.shortChain(s,true)?.let { return it }
        WingTechniques.wWing(s)?.let { return it }
        FishTechniques.basic(s,3)?.let { return it }
        FishTechniques.basic(s,4)?.let { return it }
        FishTechniques.finnedXWing(s)?.let { return it }
        SingleDigitPatterns.emptyRectangle(s)?.let { return it }
        SingleDigitPatterns.coloring(s)?.let { return it }
        if (tier!=LogicTier.CHAINS) return null
        ChainTechniques.find(s,Technique.X_CHAIN)?.let { return it }
        ChainTechniques.find(s,Technique.XY_CHAIN)?.let { return it }
        AlsTechniques.xz(s)?.let { return it }
        return ChainTechniques.find(s,Technique.AIC)
    }
}
