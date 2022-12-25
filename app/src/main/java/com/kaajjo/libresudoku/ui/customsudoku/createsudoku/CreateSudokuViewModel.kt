package com.kaajjo.libresudoku.ui.customsudoku.createsudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWingController
import com.kaajjo.libresudoku.core.utils.GameState
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.core.utils.UndoManager
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateSudokuViewModel @Inject constructor(
    appSettingsManager: AppSettingsManager,
    private val boardRepository: BoardRepository
) : ViewModel() {
    val highlightIdentical = appSettingsManager.highlightIdentical
    val inputMethod = appSettingsManager.inputMethod
    val fontSize = appSettingsManager.fontSize

    var multipleSolutionsDialog by mutableStateOf(false)
    var noSolutionsDialog by mutableStateOf(false)

    var gameType by mutableStateOf(GameType.Default9x9)
    var gameBoard by mutableStateOf(List(gameType.size) { row -> List(gameType.size) { col -> Cell(row, col, 0)} })
    var currCell by mutableStateOf(Cell(-1, -1, 0))

    var importStringValue by mutableStateOf("")
    var importTextFieldError by mutableStateOf(false)

    private val sudokuUtils = SudokuUtils()
    private val undoManager = UndoManager(GameState(gameBoard, emptyList()))

    private var overrideInputMethodDF = false
    var digitFirstNumber = -1

    private fun getBoardNoRef():List<List<Cell>> = gameBoard.map { items -> items.map { item -> item.copy() } }

    fun getFontSize(type: GameType = gameType, factor: Int): TextUnit {
        return sudokuUtils.getFontSize(type, factor)
    }

    fun processInput(inputMethod: Int, cell: Cell): Boolean {
        currCell =
            if(currCell.row == cell.row && currCell.col == cell.col && digitFirstNumber == 0) Cell(-1, -1) else cell

        return if(currCell.row >= 0 && currCell.col >= 0) {
            if((inputMethod == 1 || overrideInputMethodDF) && digitFirstNumber > 0) {
                processNumberInput(digitFirstNumber)
                undoManager.addState(GameState(getBoardNoRef(), emptyList()))
            }
            true
        } else {
            false
        }
    }

    fun processInputKeyboard(number: Int, inputMethod: Int, longTap: Boolean = false) {
        if(!longTap) {
            if(inputMethod == 0) {
                overrideInputMethodDF = false
                digitFirstNumber = 0
                processNumberInput(number)
                undoManager.addState(GameState(getBoardNoRef(), emptyList()))
            } else if(inputMethod == 1){
                digitFirstNumber = if(digitFirstNumber == number) 0 else number
                currCell = Cell(-1,-1, digitFirstNumber)
            }
        } else {
            if(inputMethod == 0) {
                overrideInputMethodDF = true
                digitFirstNumber = if(digitFirstNumber == number) 0 else number
                currCell = Cell(-1,-1, digitFirstNumber)
            }
        }
    }


    private fun processNumberInput(number: Int) {
        if(currCell.row >= 0 && currCell.col >= 0) {
            gameBoard = setValueCell(
                if(gameBoard[currCell.row][currCell.col].value == number) 0 else number
            )
        }
    }

    private fun setValueCell(value: Int, row: Int = currCell.row, col: Int = currCell.col) : List<List<Cell>> {
        val new = getBoardNoRef()
        new[row][col].value = value

        if(currCell.row == row && currCell.col == col) {
            currCell = currCell.copy(value = new[row][col].value)
        }

        if(value == 0) {
            new[row][col].error = false
            currCell.error = false
            return new
        }

        new[row][col].error = !sudokuUtils.isValidCellDynamic(new, new[row][col], gameType)
        new.forEach { cells ->
            cells.forEach { cell ->
                if(cell.value != 0 && cell.error) {
                    cell.error = !sudokuUtils.isValidCellDynamic(new, cell, gameType)
                }
            }
        }

        return new
    }

    fun toolbarClick(item: ToolBarItem) {
        when(item) {
            ToolBarItem.Undo -> {
                if(undoManager.count() > 0) {
                    val prevBoard = undoManager.getPrevState().board
                    gameBoard =
                        if(prevBoard.size == gameBoard.size) {
                            prevBoard
                        } else {
                            List(gameType.size) { row -> List(gameType.size) { col -> Cell(row, col, 0)} }
                        }

                    undoManager.addState(GameState(getBoardNoRef(), emptyList()))
                }
            }

            ToolBarItem.Remove -> {
                if(currCell.row >= 0 && currCell.col >= 0 && !currCell.locked) {
                    val prevValue = gameBoard[currCell.row][currCell.col].value
                    gameBoard = setValueCell(0)
                    if(prevValue != 0) {
                        undoManager.addState(GameState(getBoardNoRef(), emptyList()))
                    }
                }
            }

            else -> { }
        }
    }

    fun changeGameType(gameType: GameType) {
        if(this.gameType != gameType) {
            this.gameType = gameType
            currCell = Cell(-1, -1, 0)
            digitFirstNumber = -1
            gameBoard = List(gameType.size) { row -> List(gameType.size) { col -> Cell(row, col, 0)} }
        }
    }

    fun setFromString(puzzle: String): Boolean {
        val sudokuParser = SudokuParser()
        if(puzzle.length !in setOf(36, 81)) {
            return false
        }
        if(puzzle.all { char ->
                if(char != '0' && char != '.') {
                    char.isDigit()
                } else {
                    true
                }
            }
        ) {
            gameType = when(puzzle.length) {
                36 -> GameType.Default6x6
                81 -> GameType.Default9x9
                else -> GameType.Default9x9
            }
            gameBoard = sudokuParser.parseBoard(
                board = puzzle,
                gameType = gameType
            )
            return true
        } else {
            return false
        }
    }

    fun saveGame(): Boolean {
        val result = checkGame()
        when(result.second) {
            0 -> noSolutionsDialog = true
            1 -> saveToDb(result.first)
            else -> multipleSolutionsDialog = true
        }
        return result.second == 1
    }

    private fun saveToDb(board: IntArray) {
        viewModelScope.launch(Dispatchers.IO) {
            boardRepository.insert(
                SudokuBoard(
                    uid = 0,
                    initialBoard = gameBoard.flatten()
                        .joinToString("") { cell -> cell.value.toString() },
                    solvedBoard = board.joinToString(""),
                    difficulty = GameDifficulty.Custom,
                    type = gameType
                )
            )
        }
    }
    private fun checkGame(): Pair<IntArray, Int> {
        val qqWingController = QQWingController()
        val solution = qqWingController.solve(gameBoard.flatten().map { cell -> cell.value }.toIntArray(), gameType)
        return Pair(solution, qqWingController.solutionCount)
    }
}
