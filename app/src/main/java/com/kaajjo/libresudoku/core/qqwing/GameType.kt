package com.kaajjo.libresudoku.core.qqwing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class GameType(val size: Int, val sectionHeight: Int, val sectionWidth: Int) : Parcelable {
    Unspecified(1, 1, 1),
    Default9x9(9, 3, 3),
    Default12x12(12, 3, 4),
    Default6x6(6,2,3),
}