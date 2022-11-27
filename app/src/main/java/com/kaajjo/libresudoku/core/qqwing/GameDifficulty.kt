package com.kaajjo.libresudoku.core.qqwing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class GameDifficulty : Parcelable {
    Unspecified,
    Simple,
    Easy,
    Moderate,
    Hard,
    Challenge,
    Custom
}