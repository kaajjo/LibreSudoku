package com.kaajjo.libresudoku.ui.gameshistory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import com.kaajjo.libresudoku.ui.create_edit_sudoku.GameStateFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject constructor(
    savedGameRepository: SavedGameRepository,
    appSettingsManager: AppSettingsManager
) : ViewModel(
) {
    val games = savedGameRepository.getWithBoards()

    var sortType by mutableStateOf(SortType.Descending)
    var sortEntry by mutableStateOf(SortEntry.GameID)
    var filterDifficulties by mutableStateOf(emptyList<GameDifficulty>())
    var filterGameTypes by mutableStateOf(emptyList<GameType>())
    var filterByGameState by mutableStateOf(GameStateFilter.All)

    val dateFormat = appSettingsManager.dateFormat

    fun selectFilter(filter: GameDifficulty) {
        filterDifficulties = if (!filterDifficulties.contains(filter)) {
            filterDifficulties + filter
        } else {
            filterDifficulties - filter
        }
    }

    fun selectFilter(filter: GameType) {
        filterGameTypes = if (!filterGameTypes.contains(filter)) {
            filterGameTypes + filter
        } else {
            filterGameTypes - filter
        }
    }

    fun selectFilter(filter: GameStateFilter) {
        this.filterByGameState = filter
    }

    fun switchSortType() {
        sortType = if (sortType == SortType.Ascending) {
            SortType.Descending
        } else {
            SortType.Ascending
        }
    }

    fun selectSortEntry(sortEntry: SortEntry) {
        this.sortEntry = sortEntry
    }

    fun applySortAndFilter(games: List<Pair<SavedGame, SudokuBoard>>): List<Pair<SavedGame, SudokuBoard>> {
        var result = applyFilterDifficulties(games)
        result = applyFilterTypes(result)
        result = applyFilterByGameState(result)
        result = applySort(result)
        return result
    }

    private fun applyFilterDifficulties(games: List<Pair<SavedGame, SudokuBoard>>): List<Pair<SavedGame, SudokuBoard>> {
        return if (filterDifficulties.isNotEmpty()) {
            games.filter {
                filterDifficulties.contains(it.second.difficulty)
            }
        } else {
            games
        }
    }

    private fun applyFilterTypes(games: List<Pair<SavedGame, SudokuBoard>>): List<Pair<SavedGame, SudokuBoard>> {
        return if (filterGameTypes.isNotEmpty()) {
            games.filter {
                filterGameTypes.contains(it.second.type)
            }
        } else {
            games
        }
    }

    private fun applyFilterByGameState(games: List<Pair<SavedGame, SudokuBoard>>): List<Pair<SavedGame, SudokuBoard>> {
        return if (filterByGameState != GameStateFilter.All) {
            games.filter {
                when (filterByGameState) {
                    GameStateFilter.Completed -> !it.first.canContinue
                    GameStateFilter.InProgress -> it.first.canContinue
                    else -> false
                }
            }
        } else {
            games
        }
    }

    private fun applySort(games: List<Pair<SavedGame, SudokuBoard>>): List<Pair<SavedGame, SudokuBoard>> {
        return if (sortType == SortType.Ascending) {
            when (sortEntry) {
                SortEntry.GameID -> games.sortedBy { it.first.uid }
                SortEntry.Timer -> games.sortedBy { it.first.timer }
            }
        } else {
            when (sortEntry) {
                SortEntry.GameID -> games.sortedByDescending { it.first.uid }
                SortEntry.Timer -> games.sortedByDescending { it.first.timer }
            }
        }
    }
}

enum class SortType(val resName: Int) {
    Ascending(R.string.sort_ascending),
    Descending(R.string.sort_descending)
}

enum class SortEntry(val resName: Int) {
    GameID(R.string.sort_by_game_id),
    Timer(R.string.sort_by_timer)
}