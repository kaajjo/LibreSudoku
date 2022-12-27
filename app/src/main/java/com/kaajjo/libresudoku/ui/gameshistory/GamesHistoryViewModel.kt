package com.kaajjo.libresudoku.ui.gameshistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class HistoryViewModel
@Inject constructor(
    savedGameRepository: SavedGameRepository,
    private val boardRepository: BoardRepository
): ViewModel(
) {
    val savedGames = savedGameRepository.getAll()
    fun getBoards(): StateFlow<List<SudokuBoard>> {
        // The lazycolumn not lagging now when scrolling fast,
        // but I don't think this is the right way to do that
        // but better this than nothing
        val result = MutableStateFlow<List<SudokuBoard>>(emptyList())
        viewModelScope.launch(Dispatchers.IO) {
            result.emit(boardRepository.getAllList())
        }
        return result
    }
}