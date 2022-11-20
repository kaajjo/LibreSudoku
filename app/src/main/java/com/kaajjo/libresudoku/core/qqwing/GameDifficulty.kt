package com.kaajjo.libresudoku.core.qqwing

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class GameDifficulty(val resName: String) : Parcelable {
    Unspecified("Неизвестно"),
    Simple("Легчайше"),
    Easy("Легко"),
    Moderate("Средне"),
    Hard("Сложно"),
    Challenge("Экстремально"),
    Custom("Кастомная")
}