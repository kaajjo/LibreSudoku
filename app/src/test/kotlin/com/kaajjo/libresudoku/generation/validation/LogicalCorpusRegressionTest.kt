package com.kaajjo.libresudoku.generation.validation

import org.junit.Test

import com.kaajjo.libresudoku.core.generator.rating.*
import com.kaajjo.libresudoku.core.generator.dlx.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.random.Random

internal fun reportOf(r: LogicalRating): LogicReport?=when(r) {
    is LogicalRating.Rated -> r.report;is LogicalRating.BeyondSupported -> r.report
    is LogicalRating.AlreadySolved -> r.report;is LogicalRating.Contradiction -> r.report
    is LogicalRating.InvalidInput -> null
}
internal fun validateTrace(report: LogicReport) {
    // A report does not store the input; callers test replay where the puzzle is available.
    check(report.steps.size==report.moveCount)
    check(report.counts().values.sum()==report.moveCount)
}
internal fun verifyLinks(s: LogicalState,step: LogicalStep) {
    for (link in step.links) {
        val a=link.from;val b=link.to
        check(s.has(a.cell,a.value) && s.has(b.cell,b.value))
        check(a!=b)
        if (link.unit==-1) {
            check(a.cell==b.cell && a.value!=b.value)
            if (link.kind==LinkKind.STRONG) check(population(s.masks[a.cell])==2)
        } else {
            check(a.value==b.value && link.unit in s.topology.cellUnits[a.cell] && link.unit in s.topology.cellUnits[b.cell])
            if (link.kind==LinkKind.STRONG) check(s.positions(link.unit,a.value).size==2)
        }
    }
}
class LogicalCorpusRegressionTest {
    @Test
    fun all420PuzzlesHaveSoundReplayAndConcurrentDeterminism() {
        val rows=corpusCases()
        var nodes=0L;var moves=0L;var effects=0L;var solved=0;var stillBeyond=0
        val used=mutableMapOf<Technique,Int>();val distribution=mutableMapOf<String,Int>();val samples=ArrayList<Triple<IntArray,SudokuGeometry,String>>()
        val rater=QqWingLogicalRater()
        val start=System.nanoTime()
        for ((idx,case) in rows.withIndex()) {
            val g=case.geometry; val n=g.size; val seed=case.seed
            val puzzle=case.puzzle
            val engine=DlxEngine(n,g.boxHeight,g.boxWidth,Random(99))
            check(engine.solve(puzzle,2,maxSearchNodes=10_000_000)==1) { "Corpus not unique: $n/$seed" }
            val solution=engine.solvedBoard;nodes+=engine.searchNodes
            val independent=IndependentOracle(g,10_000_000).solve(puzzle,limit=2)
            check(independent.size==1 && independent.single().contentEquals(solution)) { "Oracle disagreement: $n/$seed" }
            val movesBefore=moves; val effectsBefore=effects
            val rating=rater.rateObserved(puzzle,g,false,true,{}) { _,step,board,masks ->
                    moves++;effects+=step.eliminations.size+step.placements.size
                    for (p in step.placements) check(p.value==solution[p.cell]) { "Wrong placement $p ${step.technique}" }
                    for (e in step.eliminations) check(e.value!=solution[e.cell]) { "Wrong elimination $e ${step.technique}" }
                    for (c in board.indices) check(if (board[c]!=0) board[c]==solution[c] else masks[c] and bit(solution[c])!=0)
                }
            check(rating !is LogicalRating.InvalidInput && rating !is LogicalRating.Contradiction)
            val report=checkNotNull(reportOf(rating));validateTrace(report)
            val state=fresh(g);check(state.initialize(puzzle))
            for (step in report.steps) { verifyLinks(state,step);state.apply(step) }
            check(state.board.contentEquals(report.solutionSnapshot()) && state.masks.contentEquals(report.candidateMasksSnapshot()))
            val label=if(rating is LogicalRating.Rated) report.tier.name else "BEYOND"
            check(label==case.expectedTier) { "Rating regression for $n/$seed: expected ${case.expectedTier}, got $label" }
            check((if(rating is LogicalRating.Rated) report.effortScore else null)==case.expectedScore) { "Score regression: $n/$seed" }
            check(report.moveCount==case.expectedMoves) { "Returned-pass trace length changed: $n/$seed" }
            check(moves-movesBefore==case.expectedAllPassMoves && effects-effectsBefore==case.expectedAllPassEffects) {
                "Review changed logical traces before updating the baseline: $n/$seed"
            }
            distribution[label]=(distribution[label] ?: 0)+1
            if (rating is LogicalRating.Rated) {
                solved++
            } else stillBeyond++
            for (tech in Technique.values()) if(report.uses(tech)>0) used[tech]=(used[tech] ?: 0)+report.uses(tech)
            if(idx%7==0) samples+=Triple(puzzle,g,label+":"+report.effortScore+":"+report.steps.joinToString { it.signature() })
            if(idx%50==0) println("Corpus ${idx+1}/${rows.size}: $distribution")
        }
        check(solved==rows.count { it.expectedTier!="BEYOND" } && stillBeyond==rows.count { it.expectedTier=="BEYOND" })
        check(moves==rows.sumOf { it.expectedAllPassMoves } && effects==rows.sumOf { it.expectedAllPassEffects })
        val pool=Executors.newFixedThreadPool(8)
        try {
            val futures=pool.invokeAll((0 until 4).flatMap { samples.map { (p,g,expected) -> Callable {
                val result=rater.rate(p,g,captureSteps=true);val rep=checkNotNull(reportOf(result))
                val label=if(result is LogicalRating.Rated) rep.tier.name else "BEYOND"
                check(label+":"+rep.effortScore+":"+rep.steps.joinToString { it.signature() }==expected)
            } } })
            futures.forEach { it.get() }
            println("Concurrent deterministic comparisons: ${futures.size}")
        } finally { pool.shutdownNow() }
        println("TOTAL corpus=${rows.size} rated=$solved beyond=$stillBeyond")
        println("All-pass moves=$moves, checked effects=$effects, DLX nodes=$nodes")
        println("Returned-pass technique uses: $used")
        println("Distribution=$distribution")
        println("Harness elapsed ms=${(System.nanoTime()-start)/1_000_000} (includes validation and concurrency; NOT an Android benchmark)")
    }
}
