package com.kaajjo.libresudoku.generation.validation

import org.junit.Test

import com.kaajjo.libresudoku.core.generator.rating.*
import kotlin.random.Random

class SmallDomainSoundnessTest {
    @Test
    fun allCompletionsAndNearMissesRemainSound() {
        val g=SudokuGeometry(4,2,2)
        val all=IndependentOracle(g).solve(limit=1000)
        check(all.size==288)
        val rng=Random(1927);var states=0;var moves=0;var effects=0
        val rater=QqWingLogicalRater(EvaluationLimits(maxOperations=100_000_000))
        repeat(700) { trial ->
            val witness=all[rng.nextInt(all.size)]
            val s=fresh(g)
            if (trial%2==0) {
                val board=IntArray(16) { if (rng.nextInt(100)<45) witness[it] else 0 }
                check(s.initialize(board))
            } else {
                // Candidate states are not claimed to be reachable by previous named techniques.
                // They are satisfiable, and the oracle retains ALL their completions.
                for (c in 0 until 16) for (v in 1..4) if (v!=witness[c] && rng.nextBoolean()) s.masks[c]=s.masks[c] and bit(v).inv()
            }
            val completions=all.filter { solution -> solution.indices.all { c ->
                if (s.board[c]!=0) s.board[c]==solution[c] else s.masks[c] and bit(solution[c])!=0
            } }
            check(completions.isNotEmpty())
            for (iteration in 0 until 65) {
                if (s.board.all { it!=0 }) break
                check(!s.contradiction())
                val step=rater.nextStep(s,LogicTier.CHAINS) ?: break
                for (solution in completions) {
                    for (e in step.eliminations) check(solution[e.cell]!=e.value) { "Lost completion in ${step.technique}" }
                    for (p in step.placements) check(solution[p.cell]==p.value)
                }
                s.apply(step)
                for (solution in completions) for (c in solution.indices) check(
                    if (s.board[c]!=0) s.board[c]==solution[c] else s.masks[c] and bit(solution[c])!=0
                )
                moves++;effects+=step.eliminations.size+step.placements.size
            }
            states++
        }
        // Negative/near-miss states: a required support condition is deliberately broken.
        var nearMiss=0;var nearEffects=0
        fun checkNear(name:String,tech:Technique,alter:(LogicalState)->Unit) {
            val s=fixtures().single { it.name==name }.state; alter(s)
            check(IndependentOracle(s.topology.geometry).solve(s.board,s.masks).isNotEmpty())
            val step=findTechnique(s,tech)
            if (step!=null) nearEffects+=certify(s,step)
            nearMiss++
        }
        checkNear("x-wing",Technique.X_WING) { it.masks[8]=it.masks[8] or bit(1) }
        checkNear("swordfish",Technique.SWORDFISH) { it.masks[8]=it.masks[8] or bit(1) }
        checkNear("w-wing",Technique.W_WING) { it.masks[20]=it.masks[20] or bit(1) }
        checkNear("two-string-kite",Technique.TWO_STRING_KITE) { it.masks[19]=it.masks[19] or bit(1) }
        checkNear("finned-x-wing",Technique.FINNED_X_WING) { it.masks[35]=it.masks[35] or bit(1) }
        checkNear("als-xz-two-plus-two",Technique.ALS_XZ) { it.masks[13]=it.masks[13] or bit(1) }
        // This cell sees both XYZ wings but NOT the pivot. Eliminating z there is invalid.
        val xyz=fixtures().single { it.name=="xyz-wing" }.state
        val step=checkNotNull(findTechnique(xyz,Technique.XYZ_WING))
        check(Candidate(13,3) !in step.eliminations)
        val force=xyz.masks.copyOf();force[13]=bit(3)
        check(IndependentOracle(xyz.topology.geometry).solve(xyz.board,force).isNotEmpty())
        // Budget/cancellation is never a Rated/BeyondSupported result; the stateless rater is reusable.
        val board=IntArray(81)
        try { QqWingLogicalRater(EvaluationLimits(maxOperations=1)).rate(board,SudokuGeometry(9,3,3));error("Budget swallowed") }
        catch (_:EvaluationBudgetExceeded) { }
        var callbacks=0
        QqWingLogicalRater().rate(board,SudokuGeometry(9,3,3),checkpoint={callbacks++})
        var cancelled=0
        for (at in listOf(1,2,4,callbacks/2,callbacks).distinct().filter { it>0 }) {
            var calls=0;val marker=IllegalStateException("injected-$at")
            try { rater.rate(board,SudokuGeometry(9,3,3),checkpoint={if(++calls==at) throw marker});error("Checkpoint swallowed") }
            catch (e:IllegalStateException) { check(e===marker) }
            check(rater.rate(all[0],g) is LogicalRating.AlreadySolved);cancelled++
        }
        println("PASS independent full 4x4 completion space=288; candidate/givens states=$states")
        println("PASS all-completion-preserving moves=$moves; effects=$effects")
        println("PASS near-miss states=$nearMiss; certified residual effects=$nearEffects; XYZ false-target counterexample=1")
        println("PASS logical budget propagation; cancellation/reuse points=$cancelled")
    }
}
