package com.kaajjo.libresudoku.core.generator.dlx

import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry
import com.kaajjo.libresudoku.core.generator.rating.Topology
import kotlin.random.Random

/**
 * Holds a unique puzzle and its solution with defensive array snapshots.
 *
 * @param puzzle Row-major givens, with zero for empty cells.
 * @param solution Complete row-major solution matching the givens.
 * @param uniquenessChecks Number of completed clue-removal checks.
 * @param searchNodes Total DLX nodes visited during the initial solve and removal checks.
 */
class GeneratedPuzzle internal constructor(puzzle: IntArray, solution: IntArray, val uniquenessChecks: Int, val searchNodes: Long) {
    private val puzzleData=puzzle.copyOf(); private val solutionData=solution.copyOf()
    val puzzle: IntArray get()=puzzleData.copyOf()
    val solution: IntArray get()=solutionData.copyOf()
    val emptyCells: Int=puzzle.count { it==0 }
}

/**
 * Stateless classic generator. Unique single-clue removal; target is best-effort, not difficulty.
 * Random must be owned by this request. Checkpoint/budget exceptions abort rather than mark a clue
 * unremovable. The final full solution is kept separately and never passed to the logical rater.
 */
object ClassicPuzzleGenerator {
    /**
     * Builds a full solution and removes clues while preserving uniqueness.
     *
     * @param geometry Grid and box dimensions.
     * @param targetEmptyCells Nonnegative desired number of holes, capped at the cell count; may be unreachable.
     * @param random Random source owned by this request.
     * @param topK Positive number of weighted removal candidates considered per step.
     * @param maxSearchNodesPerCheck Positive node budget for each DLX search.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @return Independent puzzle and solution snapshots plus search statistics.
     */
    fun generate(
        geometry: SudokuGeometry,
        targetEmptyCells: Int,
        random: Random=Random.Default,
        topK: Int=4,
        maxSearchNodesPerCheck: Long=2_000_000L,
        checkpoint: () -> Unit={}
    ): GeneratedPuzzle = generateInternal(geometry,targetEmptyCells,random,topK,maxSearchNodesPerCheck,checkpoint,true)

    /**
     * Runs clue removal with a selectable uniqueness-check strategy.
     *
     * @param geometry Grid and box dimensions.
     * @param targetEmptyCells Desired hole count; removal stops earlier if no unique removal remains.
     * @param random Random source owned by this request.
     * @param topK Positive candidate pool size for each removal step.
     * @param maxSearchNodesPerCheck Positive node budget for each DLX search.
     * @param checkpoint Cooperative cancellation or deadline callback; exceptions propagate to the caller.
     * @param alternativeCheck Use single-clue alternative search when true, general count-to-two when false.
     */
    internal fun generateInternal(
        geometry: SudokuGeometry, targetEmptyCells: Int, random: Random, topK: Int,
        maxSearchNodesPerCheck: Long, checkpoint: () -> Unit, alternativeCheck: Boolean
    ): GeneratedPuzzle {
        require(targetEmptyCells>=0 && topK>0 && maxSearchNodesPerCheck>0)
        checkpoint()
        val topology=Topology.of(geometry); val cells=geometry.cellCount
        val target=minOf(targetEmptyCells,cells); val k=minOf(topK,cells)
        val engine=DlxEngine(geometry.size,geometry.boxHeight,geometry.boxWidth,random,checkpoint)
        check(engine.solve(IntArray(cells),1,checkpoint,maxSearchNodesPerCheck)==1)
        val solution=engine.solvedBoard; val board=solution.copyOf()
        var nodes=engine.searchNodes; var checks=0
        if (target==0) return GeneratedPuzzle(board,solution,checks,nodes)
        val canRemove=BooleanArray(cells) { true }
        val filledPeers=IntArray(cells) { topology.peers[it].size }
        val indices=IntArray(k); val weights=IntArray(k); val order=IntArray(cells) { it }
        var holes=0; var preferSparse=false
        while (holes<target) {
            checkpoint(); indices.fill(-1);weights.fill(-1);order.shuffle(random)
            var candidates=0
            for (cell in order) {
                if (board[cell]==0 || !canRemove[cell]) continue
                val weight=if (preferSparse) topology.peers[cell].size-filledPeers[cell]
                    else filledPeers[cell]*10+random.nextInt(10)
                candidates++
                if (weight<=weights[k-1]) continue
                var at=k-1
                while (at>0 && weights[at-1]<weight) at--
                for (i in k-1 downTo at+1) { weights[i]=weights[i-1]; indices[i]=indices[i-1] }
                weights[at]=weight;indices[at]=cell
            }
            if (candidates==0) break
            val pool=minOf(k,candidates)
            for (i in pool-1 downTo 1) {
                val j=random.nextInt(i+1); val tmp=indices[i];indices[i]=indices[j];indices[j]=tmp
            }
            var removed=false
            for (i in 0 until pool) {
                checkpoint()
                val cell=indices[i]; check(cell>=0)
                val value=board[cell]; board[cell]=0
                var accept=false
                try {
                    accept=if (alternativeCheck) !engine.hasAlternativeAfterRemoval(board,cell,value,checkpoint,maxSearchNodesPerCheck)
                    else when (val count=engine.solve(board,2,checkpoint,maxSearchNodesPerCheck,false)) {
                        1 -> true; 2 -> false; else -> error("Lost the known solution; count=$count")
                    }
                    checks++;nodes+=engine.searchNodes
                } finally {
                    if (!accept) board[cell]=value
                }
                if (accept) {
                    holes++; removed=true; preferSparse=false
                    for (peer in topology.peers[cell]) filledPeers[peer]--
                    break
                } else canRemove[cell]=false
            }
            if (!removed) preferSparse=true
        }
        checkpoint()
        return GeneratedPuzzle(board,solution,checks,nodes)
    }
}
