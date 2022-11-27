package com.kaajjo.libresudoku.ui.gameshistory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuUtils
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

    fun getBoardByUid(uid: Long): StateFlow<SudokuBoard?> {
        val result = MutableStateFlow<SudokuBoard?>(null)
        viewModelScope.launch(Dispatchers.IO) {
            val board = boardRepository.get(uid)
            result.emit(board)
        }
        return result
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