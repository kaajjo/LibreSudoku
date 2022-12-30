package com.kaajjo.libresudoku.ui.customsudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomSudokuViewModel @Inject constructor(
    private val boardRepository: BoardRepository
) : ViewModel() {
    var inSelectionMode by mutableStateOf(false)

    val selectedItems = mutableStateListOf<Pair<SudokuBoard, SavedGame?>>()

    val boards = boardRepository.getSavedGamesWithBoard(GameDifficulty.Custom)

    var currentFilter by mutableStateOf(SudokuListFilter.All)
    fun clearSelection() = selectedItems.clear()
    fun addToSelection(item: Pair<SudokuBoard, SavedGame?>) {
        if(!selectedItems.contains(item)) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }
    }
    fun addAllToSelection(items: List<Pair<SudokuBoard, SavedGame?>>) {
        items.forEach { item ->
            if(!selectedItems.contains(item)) {
                selectedItems.add(item)
            }
        }
    }
    fun inverseSelection(allItems: List<Pair<SudokuBoard, SavedGame?>>) {
        allItems.forEach { item ->
            addToSelection(item)
        }
    }
    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedItems.forEach { item ->
                boardRepository.delete(item.first)
            }
            clearSelection()
        }
    }

    fun filterBoards(items: List<Pair<SudokuBoard, SavedGame?>>): List<Pair<SudokuBoard, SavedGame?>> {
        return when(currentFilter) {
            SudokuListFilter.All -> items
            SudokuListFilter.Completed -> items.filter {
                it.second?.canContinue?.not() ?: false }
            SudokuListFilter.InProgress -> items.filter {
                it.second?.canContinue ?: false
            }
            SudokuListFilter.NotStarted -> items.filter {
                it.second == null
            }
        }
    }
}

enum class SudokuListFilter(val resName: Int) {
    All(R.string.filter_all),
    Completed(R.string.filter_completed),
    InProgress(R.string.filter_in_progress),
    NotStarted(R.string.filter_not_started)
}