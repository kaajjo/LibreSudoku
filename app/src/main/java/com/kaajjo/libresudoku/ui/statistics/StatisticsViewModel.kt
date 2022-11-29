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
import com.kaajjo.libresudoku.core.utils.SudokuUtils
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

    //fun setStreakTipCard(enabled: Boolean) {
    //    viewModelScope.launch(Dispatchers.IO) {
    //        tipCardsDataStore.setStreakCard(enabled)
    //    }
    //}
    //
    //fun getCurrentStreak(games: List<SavedGame>): Int {
    //    var currentStreak = 0
    //    games.forEach { game ->
    //        if(game.completed && !game.canContinue) {
    //            currentStreak = if(!game.giveUp) currentStreak + 1 else 0
    //        }
    //    }
    //    return currentStreak
    //}
    //
    //fun getMaxStreak(games: List<SavedGame>): Int {
    //    var maxStreak = 0
    //    var currentStreak = 0
    //    games.forEach { game ->
    //        if(game.completed && !game.canContinue) {
    //            currentStreak = if(!game.giveUp) currentStreak + 1 else 0
    //            if(currentStreak > maxStreak){
    //                maxStreak = currentStreak
    //            }
    //        }
    //    }
    //    return maxStreak
    //}

    fun getCurrentTypeString(context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getGameTypeString(selectedType, context)
    }
    fun getCurrentDifficultyString(context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getDifficultyString(selectedDifficulty, context)
    }

    fun getDifficultyString(difficulty: GameDifficulty, context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getDifficultyString(difficulty, context)
    }

    fun getGameTypeString(gameType: GameType, context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getGameTypeString(gameType, context)
    }
}