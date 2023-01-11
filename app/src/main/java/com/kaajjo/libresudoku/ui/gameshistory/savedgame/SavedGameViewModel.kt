package com.kaajjo.libresudoku.ui.gameshistory.savedgame

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class SavedGameViewModel
@Inject constructor(
    private val boardRepository: BoardRepository,
    private val savedGameRepository: SavedGameRepository,
    appSettingsManager: AppSettingsManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val boardUid = savedStateHandle.get<Long>("uid")

    val fontSize = appSettingsManager.fontSize

    var savedGame by mutableStateOf<SavedGame?>(null)
    var boardEntity by mutableStateOf<SudokuBoard?>(null)

    var parsedInitialBoard by mutableStateOf(emptyList<List<Cell>>())
    var parsedCurrentBoard by mutableStateOf(emptyList<List<Cell>>())
    var notes by mutableStateOf(emptyList<Note>())

    var exportDialog by mutableStateOf(false)

    fun updateGame() {
        viewModelScope.launch(Dispatchers.IO) {
            boardEntity = boardRepository.get(boardUid ?: 0)
            savedGame = savedGameRepository.get(boardUid ?: 0)

            boardEntity?.let { boardEntity ->
                savedGame?.let { savedGame ->
                    withContext(Dispatchers.Default) {
                        val sudokuParser = SudokuParser()
                        parsedInitialBoard =
                            sudokuParser.parseBoard(boardEntity.initialBoard, boardEntity.type)
                        parsedCurrentBoard =
                            sudokuParser.parseBoard(savedGame.currentBoard, boardEntity.type)
                        notes = sudokuParser.parseNotes(savedGame.notes)
                    }
                }
            }
        }
    }

    var causeMistakesLimit by mutableStateOf(false)
    var correctSolution by mutableStateOf(false)
    fun isSolved(): Boolean {
        val sudokuParser = SudokuParser()

        savedGame?.let {
            if (it.mistakes >= 3) {
                causeMistakesLimit = true
                return true
            }
        }

        if (boardEntity == null || savedGame == null || parsedInitialBoard.isEmpty()) return false
        boardEntity?.let { boardEntity ->
            val solvedBoard = sudokuParser.parseBoard(boardEntity.solvedBoard, boardEntity.type)
            if (solvedBoard.size != parsedCurrentBoard.size) return false
            for (i in 0 until boardEntity.type.size) {
                for (j in 0 until boardEntity.type.size) {
                    if (solvedBoard[i][j].value != parsedCurrentBoard[i][j].value) {
                        return false
                    }
                }
            }
        }
        correctSolution = true
        return true
    }

    fun getProgressFilled(): Pair<Int, Int> {
        var size = 0
        val count = boardEntity?.let { boardEntity ->
            boardEntity.type.let { type ->
                size = (type.sectionWidth * type.sectionHeight)
                    .toDouble()
                    .pow(2.0)
                    .toInt()

                size - parsedCurrentBoard.let { board ->
                    board.sumOf { cells -> cells.count { cell -> cell.value == 0 } }
                }
            }
        } ?: 0
        return Pair(size, count)
    }

    fun copyBoardToClipboard(context: Context): Boolean {
        boardEntity?.let {
            val clipBoardManager =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipBoardManager.setPrimaryClip(
                ClipData.newPlainText("Sudoku", it.initialBoard.replace('0', '.'))
            )
            return true
        }
        return false
    }

    fun getFontSize(factor: Int): TextUnit {
        boardEntity?.let {
            val sudokuUtils = SudokuUtils()
            return sudokuUtils.getFontSize(it.type, factor)
        }
        return 24.sp
    }
}