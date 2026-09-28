package com.kaajjo.libresudoku.core.qqwing.models

sealed class SolveResult {
    /**
     * solutionCount is saturated at 2.
     *
     * @param solution First solution as a row-major array of one-based values.
     * @param solutionCount Count capped at two; two means at least two solutions.
     */
    data class Success(val solution: IntArray, val solutionCount: Int): SolveResult()
    object Impossible: SolveResult()
}
