package com.kaajjo.libresudoku.ui.explore_folder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.qqwing.QQWingController
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.domain.usecase.UpdateManyBoardsUseCase
import com.kaajjo.libresudoku.domain.usecase.board.DeleteBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.board.DeleteBoardsUseCase
import com.kaajjo.libresudoku.domain.usecase.board.GetBoardsInFolderWithSavedUseCase
import com.kaajjo.libresudoku.domain.usecase.board.UpdateBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.GetFolderUseCase
import com.kaajjo.libresudoku.domain.usecase.folder.GetFoldersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreFolderViewModel @Inject constructor(
    getFolderUseCase: GetFolderUseCase,
    getBoardsInFolderWithSavedUseCase: GetBoardsInFolderWithSavedUseCase,
    private val updateBoardUseCase: UpdateBoardUseCase,
    private val updateManyBoardsUseCase: UpdateManyBoardsUseCase,
    private val deleteBoardUseCase: DeleteBoardUseCase,
    private val deleteBoardsUseCase: DeleteBoardsUseCase,
    getFoldersUseCase: GetFoldersUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val folderUid = savedStateHandle.get<Long>("uid") ?: 0

    val folder = getFolderUseCase(folderUid)
    val games = getBoardsInFolderWithSavedUseCase(folderUid)

    var gameUidToPlay: Long? by mutableStateOf(null)
    var isPlayedBefore by mutableStateOf(false)
    var readyToPlay by mutableStateOf(false)

    var inSelectionMode by mutableStateOf(false)
    var selectedBoardsList by mutableStateOf(emptyList<SudokuBoard>())

    val folders = getFoldersUseCase()

    fun prepareSudokuToPlay(board: SudokuBoard) {
        gameUidToPlay = board.uid
        if (board.solvedBoard == "") {
            viewModelScope.launch {
                val qqWingController = QQWingController()
                val sudokuParser = SudokuParser()
                val boardToSolve = board.initialBoard.map { it.digitToInt(13) }.toIntArray()

                val solved = qqWingController.solve(boardToSolve, board.type)

                if (qqWingController.solutionCount == 1) {
                    updateBoardUseCase(
                        board.copy(solvedBoard = sudokuParser.boardToString(solved))
                    )
                    readyToPlay = true
                }
            }
        } else {
            isPlayedBefore = true
            readyToPlay = true
        }
    }

    fun addToSelection(board: SudokuBoard) {
        var currentSelected = selectedBoardsList
        currentSelected = if (!currentSelected.contains(board)) {
            currentSelected + board
        } else {
            currentSelected - board
        }
        selectedBoardsList = currentSelected
    }

    fun addAllToSelection(boards: List<SudokuBoard>) {
        selectedBoardsList = boards
    }

    fun deleteSelected() {
        viewModelScope.launch(Dispatchers.IO) {
            deleteBoardsUseCase(selectedBoardsList)
            selectedBoardsList = emptyList()
            inSelectionMode = false
        }
    }

    fun deleteGame(board: SudokuBoard) {
        viewModelScope.launch {
            deleteBoardUseCase(board)
        }
    }

    fun moveBoards(folderUid: Long) {
        viewModelScope.launch {
            updateManyBoardsUseCase(
                selectedBoardsList.map { it.copy(folderId = folderUid) }
            )
            selectedBoardsList = emptyList()
        }
    }
}