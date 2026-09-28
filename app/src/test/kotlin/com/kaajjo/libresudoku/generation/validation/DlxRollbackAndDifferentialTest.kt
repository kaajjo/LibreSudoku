package com.kaajjo.libresudoku.generation.validation

import org.junit.Test

import com.kaajjo.libresudoku.core.generator.dlx.*
import com.kaajjo.libresudoku.core.generator.rating.*
import kotlin.random.Random

private class InjectedCancellation : RuntimeException()
private fun structuralSnapshot(engine: DlxEngine): Map<String,IntArray> =
    listOf("left","right","up","down","columnSize","columnOfNode","rowOfNode","firstNodeOfRowId").associateWith { name ->
        val field=engine.javaClass.getDeclaredField(name); field.isAccessible=true
        (field.get(engine) as IntArray).copyOf()
    }
private fun assertStructure(engine: DlxEngine,expected: Map<String,IntArray>) {
    val actual=structuralSnapshot(engine)
    for ((name,values) in expected) check(values.contentEquals(actual.getValue(name))) { "DLX rollback corrupted $name" }
}
private fun validate(board: IntArray,g: SudokuGeometry) {
    check(board.size==g.cellCount && board.all { it in 1..g.size })
    for (unit in Topology.of(g).units) check(unit.map { board[it] }.toSet().size==g.size)
}
class DlxRollbackAndDifferentialTest {
    @Test
    fun rollbackBudgetsDifferentialGenerationAndMinimality() {
        var cancellationCases=0; var differential=0; var minimalChecks=0
        val g4=SudokuGeometry(4,2,2)
        val engine=DlxEngine(4,2,2,Random(20))
        val before=structuralSnapshot(engine)
        check(engine.solve(IntArray(16),1000)==288); assertStructure(engine,before)
        val full=engine.solvedBoard
        check(engine.solve(full)==1); check(engine.solvedBoard.contentEquals(full)); assertStructure(engine,before)
        val bad=full.copyOf();bad[1]=bad[0]
        try { engine.solve(bad);error("Conflicting givens accepted") } catch (_:IllegalArgumentException) { }
        assertStructure(engine,before)
        // Construct a no-local-conflict UNSAT fixture, independently certify it is really UNSAT.
        val rng=Random(840)
        var noSolution: IntArray?=null
        for (trial in 0 until 10000) {
            val board=IntArray(16)
            repeat(7) {
                val cell=rng.nextInt(16);val value=rng.nextInt(1,5)
                if (Topology.of(g4).peers[cell].none { board[it]==value }) board[cell]=value
            }
            if (IndependentOracle(g4).solve(board).isEmpty()) { noSolution=board;break }
        }
        check(engine.solve(checkNotNull(noSolution))==0)
        assertStructure(engine,before)
        var checkpointCalls=0
        check(engine.solve(IntArray(16),1000,{ checkpointCalls++ })==288)
        for (failAt in 1..checkpointCalls) {
            var calls=0
            try { engine.solve(IntArray(16),1000,{ if (++calls==failAt) throw InjectedCancellation() });error("No cancellation at $failAt") }
            catch (_:InjectedCancellation) { }
            assertStructure(engine,before);check(engine.solutionCount==0 && engine.solvedBoard.all { it==0 })
            check(engine.solve(full)==1); cancellationCases++
        }
        for (budget in listOf(1L,2L,5L,20L,100L)) {
            try { engine.solve(IntArray(16),1000,maxSearchNodes=budget);error("Expected node limit") } catch (_:DlxSearchBudgetExceeded) { }
            assertStructure(engine,before);check(engine.solve(full)==1)
        }
        // Row exclusion is also undone when interrupted before/during search.
        val minimal=ClassicPuzzleGenerator.generate(g4,16,Random(123)).puzzle
        for (cell in minimal.indices) if (minimal[cell]!=0) {
            val value=minimal[cell]; val without=minimal.copyOf(); without[cell]=0
            check(engine.solve(without)==2)
            check(engine.hasAlternativeAfterRemoval(without,cell,value)); assertStructure(engine,before)
            try { engine.hasAlternativeAfterRemoval(without,cell,value,maxSearchNodes=1);error("Missing exclusion node budget") }
            catch (_:DlxSearchBudgetExceeded) { }
            assertStructure(engine,before)
            var callsTotal=0
            engine.hasAlternativeAfterRemoval(without,cell,value,{ callsTotal++ })
            for (failAt in 1..callsTotal) {
                var calls=0
                try { engine.hasAlternativeAfterRemoval(without,cell,value,{ if (++calls==failAt) throw InjectedCancellation() });error("Missing exclusion cancellation") }
                catch (_:InjectedCancellation) { }
                assertStructure(engine,before);check(engine.solve(full)==1);cancellationCases++
            }
        }
        // False alternative result for deleting one clue from a full grid.
        val oneHole=full.copyOf();oneHole[0]=0
        check(!engine.hasAlternativeAfterRemoval(oneHole,0,full[0]));assertStructure(engine,before)
        check(engine.solve(full,saveFirstSolution=false)==1 && engine.solvedBoard.all { it==0 })
        // Differential trials include the orientation swap of rectangular boxes.
        for (g in listOf(g4,SudokuGeometry(6,2,3),SudokuGeometry(6,3,2),SudokuGeometry(9,3,3),SudokuGeometry(12,3,4),SudokuGeometry(12,4,3))) {
            val holes=when(g.size) { 4->10;6->23;9->55;else->100 }
            for (seed in 0 until 16) for (k in listOf(1,4,Int.MAX_VALUE)) {
                val a=ClassicPuzzleGenerator.generateInternal(g,holes,Random(seed),k,10_000_000,{},true)
                val b=ClassicPuzzleGenerator.generateInternal(g,holes,Random(seed),k,10_000_000,{},false)
                check(a.puzzle.contentEquals(b.puzzle)) { "Removal criteria differ: $g/$seed/$k" }
                check(a.solution.contentEquals(b.solution));validate(a.solution,g)
                check(a.uniquenessChecks<=g.cellCount && a.emptyCells<=holes)
                val independent=IndependentOracle(g,10_000_000).solve(a.puzzle,limit=2)
                check(independent.size==1 && independent.single().contentEquals(a.solution))
                val mutable=a.puzzle;mutable.fill(-1);check(a.puzzle.all { it>=0 })
                differential++
            }
        }
        for (seed in 0 until 16) {
            val g=SudokuGeometry(6,2,3)
            val result=ClassicPuzzleGenerator.generate(g,Int.MAX_VALUE,Random(seed))
            val solver=DlxEngine(6,2,3)
            check(solver.solve(result.puzzle)==1)
            for (cell in result.puzzle.indices) if (result.puzzle[cell]!=0) {
                val removed=result.puzzle;removed[cell]=0
                check(solver.solve(removed)==2);minimalChecks++
            }
        }
        for (g in listOf(SudokuGeometry(1,1,1),SudokuGeometry(16,4,4),SudokuGeometry(25,5,5))) {
            // 16/25 are geometry/safe-boundary smoke tests, not deep generation benchmarks.
            val result=ClassicPuzzleGenerator.generate(g,0,Random(8));validate(result.solution,g)
            check(result.puzzle.contentEquals(result.solution))
        }
        for (badTarget in listOf(-1,Int.MIN_VALUE)) {
            try { ClassicPuzzleGenerator.generate(g4,badTarget);error("Negative target accepted") } catch (_:IllegalArgumentException) { }
        }
        for (badK in listOf(0,-1)) {
            try { ClassicPuzzleGenerator.generate(g4,1,topK=badK);error("Invalid topK accepted") } catch (_:IllegalArgumentException) { }
        }
        println("PASS DLX exact enumeration=288; cancellation/exception rollback injections=$cancellationCases; 5 node budgets")
        println("PASS differential generation pairs=$differential; independent uniqueness/solution checks=$differential")
        println("PASS exhaustive 6x6 seeds=16; indispensable clue checks=$minimalChecks; large geometry smoke=16,25")
    }
}
