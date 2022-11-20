package com.kaajjo.libresudoku.core.qqwing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class GameType(val size: Int, val sectionHeight: Int, val sectionWidth: Int, val resName: String) : Parcelable {
    Unspecified(1, 1, 1, "Неизвестно"),
    Default9x9(9, 3, 3, "9x9"),
    Default12x12(12, 3, 4, "12x12"),
    Default6x6(6,2,3, "6x6"),
}