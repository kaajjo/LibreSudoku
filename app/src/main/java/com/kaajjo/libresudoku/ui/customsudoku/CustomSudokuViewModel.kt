package com.kaajjo.libresudoku.ui.customsudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
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

    val selectedItems = mutableStateListOf<SudokuBoard>()

    val allBoards = boardRepository.getAll()
    fun getBoards(): StateFlow<List<SudokuBoard>> {
        val result = MutableStateFlow<List<SudokuBoard>>(emptyList())
        viewModelScope.launch(Dispatchers.IO) {
            result.emit(boardRepository.getAllList())
        }
        return result
    }

    fun clearSelection() = selectedItems.clear()
    fun addToSelection(board: SudokuBoard) {
        if(!selectedItems.contains(board)) {
            selectedItems.add(board)
        } else {
            selectedItems.remove(board)
        }
    }
    fun addAllToSelection(boards: List<SudokuBoard>) {
        boards.forEach { item ->
            if(!selectedItems.contains(item)) {
                selectedItems.add(item)
            }
        }
    }
    fun inverseSelection(fullBoard: List<SudokuBoard>) {
        fullBoard.forEach { item ->
            addToSelection(item)
        }
    }
    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            selectedItems.forEach { item ->
                boardRepository.delete(item)
            }
            clearSelection()
        }
    }
}