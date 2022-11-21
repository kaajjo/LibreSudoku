package com.kaajjo.libresudoku.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cell(
    val row: Int,
    val col: Int,
    var value: Int = 0,
    var error: Boolean = false,
    var locked: Boolean = false,
) : Parcelable