package com.kaajjo.libresudoku.ui.customsudoku

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomSudokuViewModel @Inject constructor(
    private val boardRepository: BoardRepository
) : ViewModel() {
    var inSelectionMode by mutableStateOf(false)

    val selectedItems = mutableStateListOf<Pair<SudokuBoard, SavedGame?>>()

    val boards = boardRepository.getSavedGamesWithBoard(GameDifficulty.Custom)

    var selectedGameStateFilter by mutableStateOf(GameStateFilter.All)
    var selectedGameTypeFilters by mutableStateOf(emptyList<GameType>())

    @OptIn(ExperimentalMaterialApi::class)
    var drawerState: ModalBottomSheetState = ModalBottomSheetState(
        ModalBottomSheetValue.Hidden, isSkipHalfExpanded = true
    )

    fun clearSelection() = selectedItems.clear()
    fun addToSelection(item: Pair<SudokuBoard, SavedGame?>) {
        if (!selectedItems.contains(item)) {
            selectedItems.add(item)
        } else {
            selectedItems.remove(item)
        }
    }

    fun addAllToSelection(items: List<Pair<SudokuBoard, SavedGame?>>) {
        items.forEach { item ->
            if (!selectedItems.contains(item)) {
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
        var result = items
        result = applyGameStateFilter(result, selectedGameStateFilter)
        result = applyGameTypeFilter(result, selectedGameTypeFilters)
        return result
    }

    private fun applyGameStateFilter(
        items: List<Pair<SudokuBoard, SavedGame?>>,
        filter: GameStateFilter
    ): List<Pair<SudokuBoard, SavedGame?>> {
        return when (filter) {
            GameStateFilter.All -> items
            GameStateFilter.Completed -> items.filter {
                it.second?.canContinue?.not() ?: false
            }

            GameStateFilter.InProgress -> items.filter {
                it.second?.canContinue ?: false
            }

            GameStateFilter.NotStarted -> items.filter {
                it.second == null
            }
        }
    }

    private fun applyGameTypeFilter(
        items: List<Pair<SudokuBoard, SavedGame?>>,
        filters: List<GameType>
    ): List<Pair<SudokuBoard, SavedGame?>> {
        return if (filters.isNotEmpty()) {
            items.filter {
                filters.contains(it.first.type)
            }
        } else {
            items
        }
    }

    fun selectGameStateFilter(filter: GameStateFilter) {
        selectedGameStateFilter = filter
    }

    fun selectGameTypeFilter(gameType: GameType) {
        selectedGameTypeFilters = if (!selectedGameTypeFilters.contains(gameType)) {
            selectedGameTypeFilters + gameType
        } else {
            selectedGameTypeFilters - gameType
        }
    }
}

enum class GameStateFilter(val resName: Int) {
    All(R.string.filter_all),
    Completed(R.string.filter_completed),
    InProgress(R.string.filter_in_progress),
    NotStarted(R.string.filter_not_started)
}