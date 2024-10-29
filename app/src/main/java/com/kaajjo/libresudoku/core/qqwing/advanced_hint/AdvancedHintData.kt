package com.kaajjo.libresudoku.core.qqwing.advanced_hint

import com.kaajjo.libresudoku.core.Cell

/**
 * Data that [AdvancedHint] returns
 *
 * @property title resource id for title
 * @property textResWithArg resource id and arguments for string resource
 * @property targetCell target cell, the result of hint
 * @property helpCells cells that help understand the hint
 */
data class AdvancedHintData(
    val titleRes: Int,
    val textResWithArg: Pair<Int, List<String>>,
    val targetCell: Cell,
    val helpCells: List<Cell>
)
