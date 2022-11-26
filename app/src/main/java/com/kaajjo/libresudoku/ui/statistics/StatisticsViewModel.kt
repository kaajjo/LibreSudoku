package com.kaajjo.libresudoku.ui.statistics

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.repository.RecordRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import com.kaajjo.libresudoku.data.datastore.TipCardsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel
@Inject constructor(
    private val recordRepository: RecordRepository,
    private val savedGameRepository: SavedGameRepository,
    private val tipCardsDataStore: TipCardsDataStore
) : ViewModel() {
    var showDeleteDialog by mutableStateOf(false)
    var selectedDifficulty by mutableStateOf(GameDifficulty.Unspecified)
    var selectedType by mutableStateOf(GameType.Unspecified)

    val recordTipCard = tipCardsDataStore.recordCard
    val streakTipCard = tipCardsDataStore.streakCard

    var recordList: Flow<List<Record>> = recordRepository.getAllSortByTime()
    val savedGamesList: Flow<List<SavedGame>> = savedGameRepository.getAll()

    fun deleteRecord(recordEntity: Record) {
        viewModelScope.launch {
            recordRepository.delete(recordEntity)
        }
    }

    fun setDifficulty(difficulty: GameDifficulty) {
        selectedDifficulty = difficulty

        if (difficulty != GameDifficulty.Unspecified && selectedType == GameType.Unspecified) {
            selectedType = GameType.Default9x9
        } else if (difficulty == GameDifficulty.Unspecified) {
            selectedType = GameType.Unspecified
        }

        loadRecords(selectedType == GameType.Unspecified && selectedDifficulty == GameDifficulty.Unspecified)
    }

    fun setType(type: GameType) {
        selectedType = type

        if(selectedType == GameType.Unspecified) {
            selectedDifficulty = GameDifficulty.Unspecified
        } else if(selectedType != GameType.Unspecified && selectedDifficulty == GameDifficulty.Unspecified) {
            selectedDifficulty = GameDifficulty.Easy
        }

        loadRecords(selectedType == GameType.Unspecified && selectedDifficulty == GameDifficulty.Unspecified)
    }

    private fun loadRecords(all: Boolean) {
        recordList = if(all) {
            recordRepository.getAllSortByTime()
        } else {
            recordRepository.getAll(selectedDifficulty, selectedType)
        }
    }

    fun setRecordTipCard(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tipCardsDataStore.setRecordCard(enabled)
        }
    }

    fun setStreakTipCard(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            tipCardsDataStore.setStreakCard(enabled)
        }
    }

    fun getCurrentStreak(games: List<SavedGame>): Int {
        var currentStreak = 0
        games.forEach { game ->
            if(game.completed) {
                currentStreak = if(!game.giveUp) currentStreak + 1 else 0
            }
        }
        return currentStreak
    }

    fun getMaxStreak(games: List<SavedGame>): Int {
        var maxStreak = 0
        var currentStreak = 0
        games.forEach { game ->
            if(game.completed) {
                currentStreak = if(!game.giveUp && !game.canContinue) currentStreak + 1 else 0
                if(currentStreak > maxStreak){
                    maxStreak = currentStreak
                }
            }
        }
        return maxStreak
    }

    fun getDifficultyString(difficulty: GameDifficulty, context: Context): String {
        return context.getString(
            when(difficulty) {
                GameDifficulty.Unspecified -> R.string.difficulty_unspecified
                GameDifficulty.Simple -> R.string.difficulty_simple
                GameDifficulty.Easy -> R.string.difficulty_easy
                GameDifficulty.Moderate -> R.string.difficulty_moderate
                GameDifficulty.Hard -> R.string.difficulty_hard
                GameDifficulty.Challenge -> R.string.difficulty_challenge
                GameDifficulty.Custom -> R.string.difficulty_custom
            }
        )
    }

    fun getGameTypeString(type: GameType, context: Context): String {
        return context.getString(
            when(type) {
                GameType.Unspecified -> R.string.type_unspecified
                GameType.Default9x9 -> R.string.type_default_9x9
                GameType.Default12x12 -> R.string.type_default_12x12
                GameType.Default6x6 -> R.string.type_default_6x6
            }
        )
    }

    fun getCurrentTypeString(context: Context): String {
        return getGameTypeString(selectedType, context)
    }
    fun getCurrentDifficultyString(context: Context): String {
        return getDifficultyString(selectedDifficulty, context)
    }
}