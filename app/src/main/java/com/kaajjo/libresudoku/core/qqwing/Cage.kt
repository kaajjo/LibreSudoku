package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.Cell
import kotlinx.serialization.Serializable

@Serializable
data class Cage(
    val id: Int = 0,
    val sum: Int = 0,
    val cells: List<Cell> = emptyList()
) {
    fun size() = cells.size
}