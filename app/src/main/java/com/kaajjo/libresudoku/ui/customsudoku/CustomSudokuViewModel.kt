package com.kaajjo.libresudoku.ui.customsudoku

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


    fun getBoards(): StateFlow<List<SudokuBoard>> {
        val result = MutableStateFlow<List<SudokuBoard>>(emptyList())
        viewModelScope.launch(Dispatchers.IO) {
            result.emit(boardRepository.getAllList())
        }
        return result
    }
}