package com.kaajjo.libresudoku.data.database.converters

import androidx.room.TypeConverter
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty

/**
 * Converts Game Difficulty
 */
class GameDifficultyConverter {
    @TypeConverter
    fun fromDifficulty(gameDifficulty: GameDifficulty): Int {
        return when (gameDifficulty) {
            GameDifficulty.Unspecified -> 0
            GameDifficulty.Simple -> 1
            GameDifficulty.Easy -> 2
            GameDifficulty.Moderate -> 3
            GameDifficulty.Hard -> 4
            GameDifficulty.Challenge -> 5
            GameDifficulty.Custom -> 6
        }
    }

    @TypeConverter
    fun toDifficulty(value: Int): GameDifficulty {
        return when (value) {
            0 -> GameDifficulty.Unspecified
            1 -> GameDifficulty.Simple
            2 -> GameDifficulty.Easy
            3 -> GameDifficulty.Moderate
            4 -> GameDifficulty.Hard
            5 -> GameDifficulty.Challenge
            6 -> GameDifficulty.Custom
            else -> GameDifficulty.Unspecified
        }
    }
}