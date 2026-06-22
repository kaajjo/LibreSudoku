package com.kaajjo.libresudoku.core.qqwing.models

sealed class SolveResult {
    data class Success(val solution: IntArray, val solutionCount: Int) : SolveResult()
    object Impossible : SolveResult()
}
