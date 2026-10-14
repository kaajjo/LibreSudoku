package com.kaajjo.libresudoku.ui.components.board

import com.kaajjo.libresudoku.core.qqwing.GameType

fun getSectionHeightForSize(size: Int): Int {
    return when (size) {
        6 -> GameType.Default6x6.sectionHeight
        9 -> GameType.Default9x9.sectionHeight
        12 -> GameType.Default12x12.sectionHeight
        else -> GameType.Default9x9.sectionHeight
    }
}

fun getSectionWidthForSize(size: Int): Int {
    return when (size) {
        6 -> GameType.Default6x6.sectionWidth
        9 -> GameType.Default9x9.sectionWidth
        12 -> GameType.Default12x12.sectionWidth
        else -> GameType.Default9x9.sectionWidth
    }
}
