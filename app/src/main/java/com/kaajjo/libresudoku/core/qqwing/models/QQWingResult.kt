package com.kaajjo.libresudoku.core.qqwing.models

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty

data class QQWingResult(
    val puzzle: IntArray,
    val solution: IntArray,
    val difficulty: GameDifficulty,
    val solutionCount: Int
)
