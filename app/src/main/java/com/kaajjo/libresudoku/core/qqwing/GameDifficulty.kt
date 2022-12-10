package com.kaajjo.libresudoku.core.qqwing

import android.os.Parcelable
import com.kaajjo.libresudoku.R
import kotlinx.parcelize.Parcelize

@Parcelize
enum class GameDifficulty(val resName: Int) : Parcelable {
    Unspecified(R.string.difficulty_unspecified),
    Simple(R.string.difficulty_simple),
    Easy(R.string.difficulty_easy),
    Moderate(R.string.difficulty_moderate),
    Hard(R.string.difficulty_hard),
    Challenge(R.string.difficulty_challenge),
    Custom(R.string.difficulty_custom)
}