package com.kaajjo.libresudoku.ui.util

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density

/**
 * Column { 1 2 3 } -> Column { 3 2 1 }
 */
@Stable
val ReverseArrangement = object : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val consumedSize = sizes.fold(0) { a, b -> a + b }
        var current = totalSize - consumedSize
        for (i in (sizes.size - 1) downTo 0) {
            outPositions[i] = current
            current += sizes[i]
        }
    }
}