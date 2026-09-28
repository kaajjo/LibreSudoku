package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry

/** Test-only copy-on-branch MRV backtracker. Does not call DLX or any logical technique detector. */
class IndependentOracle(val geometry: SudokuGeometry, val nodeLimit: Long = 2_000_000L) {
    private val n=geometry.size; private val cells=n*n; private val full=(1 shl n)-1
    private val units=ArrayList<IntArray>()
    private val peers=Array(cells) { IntArray(0) }
    var nodes=0L; private set
    init {
        for (r in 0 until n) units+=IntArray(n) { r*n+it }
        for (c in 0 until n) units+=IntArray(n) { it*n+c }
        for (br in 0 until n step geometry.boxHeight) for (bc in 0 until n step geometry.boxWidth) {
            val u=ArrayList<Int>()
            for (r in br until br+geometry.boxHeight) for (c in bc until bc+geometry.boxWidth) u+=r*n+c
            units+=u.toIntArray()
        }
        for (c in 0 until cells) peers[c]=units.filter { c in it }.flatMap { it.toList() }.distinct().filter { it!=c }.toIntArray()
    }
    fun solve(board: IntArray=IntArray(cells), masks: IntArray?=null, limit: Int=1): List<IntArray> {
        require(board.size==cells && board.all { it in 0..n } && limit>0)
        val domain=masks?.copyOf() ?: IntArray(cells) { full }
        for (c in board.indices) if (board[c]!=0) domain[c]=1 shl (board[c]-1)
        nodes=0
        val results=ArrayList<IntArray>()
        fun dfs(dom: IntArray) {
            nodes++; check(nodes<=nodeLimit) { "Oracle node budget exceeded ($geometry)" }
            // Fixed point of independent singleton and missing-digit support propagation.
            var change=true
            while (change) {
                change=false
                for (c in 0 until cells) {
                    val mask=dom[c]
                    if (mask==0) return
                    if (Integer.bitCount(mask)==1) for (p in peers[c]) if (dom[p] and mask!=0) {
                        dom[p]=dom[p] and mask.inv(); change=true
                        if (dom[p]==0) return
                    }
                }
                for (unit in units) for (v in 0 until n) {
                    val b=1 shl v; var count=0; var last=-1
                    for (c in unit) if (dom[c] and b!=0) { count++; last=c }
                    if (count==0) return
                    if (count==1 && dom[last]!=b) { dom[last]=b; change=true }
                }
            }
            var chosen=-1; var best=n+1
            for (c in 0 until cells) {
                val k=Integer.bitCount(dom[c]); if (k>1 && k<best) { best=k; chosen=c }
            }
            if (chosen==-1) { results+=IntArray(cells) { Integer.numberOfTrailingZeros(dom[it])+1 }; return }
            var options=dom[chosen]
            while (options!=0 && results.size<limit) {
                val b=options and -options; options=options xor b
                val next=dom.copyOf(); next[chosen]=b; dfs(next)
            }
        }
        dfs(domain)
        return results
    }
}
