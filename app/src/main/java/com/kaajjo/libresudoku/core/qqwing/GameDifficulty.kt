package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.R

enum class GameDifficulty(val resName: Int) {
    Unspecified(R.string.difficulty_unspecified),
    Simple(R.string.difficulty_simple),
    Easy(R.string.difficulty_easy),
    Moderate(R.string.difficulty_moderate),
    Hard(R.string.difficulty_hard),
    Challenge(R.string.difficulty_challenge),
    Custom(R.string.difficulty_custom)
}