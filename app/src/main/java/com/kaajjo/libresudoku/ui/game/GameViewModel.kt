package com.kaajjo.libresudoku.ui.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.GameState
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.core.utils.UndoManager
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.database.repository.RecordRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.util.*
import javax.inject.Inject
import kotlin.concurrent.fixedRateTimer
import kotlin.time.*

@HiltViewModel
class GameViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    private val savedGameRepository: SavedGameRepository,
    private val appSettingsManager: AppSettingsManager,
    private val recordRepository: RecordRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    init {
        val sudokuParser = SudokuParser()
        val continueSaved = savedStateHandle.get<Boolean>("saved")

        viewModelScope.launch(Dispatchers.IO) {
            boardEntity = boardRepository.get(savedStateHandle["uid"] ?: 1L)
            val savedGame = savedGameRepository.get(boardEntity.uid)

            withContext(Dispatchers.Default) {
                initialBoard = sudokuParser.parseBoard(
                    boardEntity.initialBoard,
                    boardEntity.type
                ).toList()
                initialBoard.forEach { cells ->
                    cells.forEach { cell ->
                        cell.locked = cell.value != 0
                    }
                }
                solvedBoard = sudokuParser.parseBoard(
                    boardEntity.solvedBoard,
                    boardEntity.type
                )
                for (i in solvedBoard.indices) {
                    for (j in initialBoard.indices) {
                        solvedBoard[i][j].locked = initialBoard[i][j].locked
                    }
                }
            }

            withContext(Dispatchers.Main) {
                if (savedGame != null && continueSaved!!) {
                    restoreSavedGame(savedGame)
                } else {
                    gameBoard = initialBoard
                }
                size = gameBoard.size
                undoManager = UndoManager(GameState(gameBoard, notes))
                remainingUsesList = countRemainingUses(gameBoard)
            }
            saveGame()
        }
    }

    var giveUp by mutableStateOf(false)

    val fontSize = appSettingsManager.fontSize
    val keepScreenOn = appSettingsManager.keepScreenOn

    var remainingUsesList = emptyList<Int>()
    val firstGame = appSettingsManager.firstGame
    lateinit var boardEntity: SudokuBoard
    var size by mutableStateOf(9)

    // dialogs, menus
    var restartDialog by mutableStateOf(false)
    var showMenu by mutableStateOf(false)
    var showNotesMenu by mutableStateOf(false)

    // count remaining uses
    var remainingUse = appSettingsManager.remainingUse

    // timer
    var timerEnabled = appSettingsManager.timerEnabled

    // identical numbers highlight
    val identicalHighlight = appSettingsManager.highlightIdentical

    // mistakes checking method
    var mistakesMethod = appSettingsManager.highlightMistakes.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES
    )

    var positionLines = appSettingsManager.positionLines

    var mistakesLimit = appSettingsManager.mistakesLimit.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PreferencesConstants.DEFAULT_MISTAKES_LIMIT
    )

    private var autoEraseNotes = appSettingsManager.autoEraseNotes.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES
    )

    var resetTimerOnRestart = appSettingsManager.resetTimerEnabled

    var disableHints = appSettingsManager.hintsDisabled

    var endGame by mutableStateOf(false)
    var giveUpDialog by mutableStateOf(false)

    // mistakes
    var mistakesCount by mutableStateOf(0)
    var mistakesLimitDialog by mutableStateOf(false)

    // notes
    var notesToggled by mutableStateOf(false)
    var notes by mutableStateOf(emptyList<Note>())

    private lateinit var initialBoard: List<List<Cell>>
    var gameBoard by mutableStateOf(List(9) { row -> List(9) { col -> Cell(row, col, 0) } })
    var solvedBoard = List(9) { row -> List(9) { col -> Cell(row, col, 0) } }

    var currCell by mutableStateOf(Cell(-1, -1, 0))
    private var undoManager = UndoManager(GameState(gameBoard, notes))
    private var sudokuUtils = SudokuUtils()
    var gameCompleted by mutableStateOf(false)

    // Selected number for digit first method
    var digitFirstNumber by mutableStateOf(0)
    private val inputMethod = appSettingsManager.inputMethod
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PreferencesConstants.DEFAULT_INPUT_METHOD
        )

    // temporarily use digit first method when true
    private var overrideInputMethodDF by mutableStateOf(false)

    // show/hide solution (when give up)
    var showSolution by mutableStateOf(false)

    // when true, tapping on any cell will clear it
    var eraseButtonToggled by mutableStateOf(false)

    private fun clearNotesAtCell(
        notes: List<Note>,
        row: Int = currCell.row,
        col: Int = currCell.col
    ): List<Note> {
        return notes.minus(
            notes.filter { note ->
                note.row == row
                        && note.col == col
            }.toSet()
        )
    }

    private fun emptyNotes(): List<Note> = emptyList()

    fun clearNotes() {
        notes = emptyNotes()
        undoManager.addState(
            GameState(gameBoard, notes)
        )
    }

    private fun addNote(note: Int, row: Int, col: Int): List<Note> {
        return notes.plus(Note(row, col, note))
    }

    private fun removeNote(note: Int, row: Int, col: Int): List<Note> =
        notes.minus(Note(row, col, note))

    private fun getBoardNoRef(): List<List<Cell>> =
        gameBoard.map { items -> items.map { item -> item.copy() } }

    private fun setValueCell(
        value: Int,
        row: Int = currCell.row,
        col: Int = currCell.col
    ): List<List<Cell>> {
        var new = getBoardNoRef()

        new[row][col].value = value
        remainingUsesList = countRemainingUses(new)

        if (currCell.row == row && currCell.col == col) {
            currCell = currCell.copy(value = new[row][col].value)
        }
        if (value == 0) {
            new[row][col].error = false
            currCell.error = false
            return new
        }
        // checking for mistakes
        if (mistakesMethod.value == 1) {
            // rule violations
            new[row][col].error =
                !sudokuUtils.isValidCellDynamic(new, new[row][col], boardEntity.type)
            new.forEach { cells ->
                cells.forEach { cell ->
                    if (cell.value != 0 && cell.error) {
                        cell.error = !sudokuUtils.isValidCellDynamic(new, cell, boardEntity.type)
                    }
                }
            }
        } else if (mistakesMethod.value == 2) {
            // check with final solution
            new = isValidCell(new, new[row][col])
        }

        currCell.error = currCell.value == 0
        // updating mistakes limit
        if (mistakesLimit.value && new[row][col].error) {
            mistakesCount++
            if (mistakesCount >= 3) {
                mistakesLimitDialog = true
                pauseTimer()
                giveUp()
                endGame = true
            }
        }

        gameCompleted = isCompleted(new)

        if (autoEraseNotes.value) {
            notes = autoEraseNotes(new, currCell)
        }

        return new
    }

    private fun countRemainingUses(board: List<List<Cell>>): MutableList<Int> {
        val uses = mutableListOf<Int>()
        for (i in 0..size) {
            uses.add(size - sudokuUtils.countNumberInBoard(board, i + 1))
        }
        return uses
    }

    fun processInput(cell: Cell, remainingUse: Boolean, longTap: Boolean = false): Boolean {
        if (gamePlaying) {
            currCell =
                if (currCell.row == cell.row && currCell.col == cell.col && digitFirstNumber == 0) Cell(
                    -1,
                    -1
                ) else cell

            if (currCell.row >= 0 && currCell.col >= 0) {
                if ((inputMethod.value == 1 || overrideInputMethodDF) && digitFirstNumber > 0) {
                    if (!longTap) {
                        if ((remainingUsesList.size >= digitFirstNumber && remainingUsesList[digitFirstNumber - 1] > 0) || !remainingUse) {
                            processNumberInput(digitFirstNumber)
                            undoManager.addState(GameState(gameBoard, notes))
                            if (notesToggled) currCell =
                                Cell(currCell.row, currCell.col, digitFirstNumber)
                        }
                    } else if(!currCell.locked) {
                        gameBoard = setValueCell(0)
                        setNote(digitFirstNumber)
                        undoManager.addState(GameState(gameBoard, notes))
                    }
                } else if (eraseButtonToggled) {
                    val oldCell = currCell
                    processNumberInput(0)
                    if (oldCell.value != 0 && !oldCell.locked) {
                        undoManager.addState(GameState(gameBoard, notes))
                    }
                }
                remainingUsesList = countRemainingUses(gameBoard)
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }

    fun processInputKeyboard(number: Int, longTap: Boolean = false) {
        if (gamePlaying) {
            if (!longTap) {
                if (inputMethod.value == 0) {
                    overrideInputMethodDF = false
                    digitFirstNumber = 0
                    processNumberInput(number)
                    undoManager.addState(GameState(gameBoard, notes))
                } else if (inputMethod.value == 1) {
                    digitFirstNumber = if (digitFirstNumber == number) 0 else number
                    currCell = Cell(-1, -1, digitFirstNumber)
                }
            } else {
                if (inputMethod.value == 0) {
                    overrideInputMethodDF = true
                    digitFirstNumber = if (digitFirstNumber == number) 0 else number
                    currCell = Cell(-1, -1, digitFirstNumber)
                }
            }
            eraseButtonToggled = false
        }
    }


    fun processNumberInput(number: Int) {
        if (currCell.row >= 0 && currCell.col >= 0 && gamePlaying && !currCell.locked) {
            if (!notesToggled) {
                // Clear all note to set a number
                notes = clearNotesAtCell(notes, currCell.row, currCell.col)

                gameBoard = setValueCell(
                    if (gameBoard[currCell.row][currCell.col].value == number) 0 else number
                )
            } else {
                gameBoard = setValueCell(0)
                setNote(number)
                remainingUsesList = countRemainingUses(gameBoard)
            }
        }
    }

    fun setNote(number: Int) {
        val note = Note(currCell.row, currCell.col, number)
        notes = if (notes.contains(note)) {
            removeNote(note.value, note.row, note.col)
        } else {
            addNote(note.value, note.row, note.col)
        }
    }

    var timeText by mutableStateOf("00:00")
    private var duration = Duration.ZERO
    private lateinit var timer: Timer
    var gamePlaying by mutableStateOf(false)

    fun startTimer() {
        if (!gamePlaying) {
            gamePlaying = true
            val updateRate = 50L

            timer = fixedRateTimer(initialDelay = updateRate, period = updateRate) {
                val prevTime = duration

                duration = duration.plus((updateRate * 1e6).toDuration(DurationUnit.NANOSECONDS))
                // update text every second
                if (prevTime.toInt(DurationUnit.SECONDS) != duration.toInt(DurationUnit.SECONDS)) {
                    timeText = durationToString(duration)
                    // save game
                    viewModelScope.launch(Dispatchers.IO) {
                        saveGame()
                    }
                }
            }
        }
    }

    private fun durationToString(duration: Duration): String {
        return duration.toComponents { minutes, seconds, _ ->
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun pauseTimer() {
        gamePlaying = false
        timer.cancel()
    }

    fun toolbarClick(item: ToolBarItem) {
        if (gamePlaying) {
            when (item) {
                ToolBarItem.Undo -> {
                    if (undoManager.count() > 0) {
                        undoManager.getPrevState().also {
                            gameBoard = it.board
                            notes = it.notes
                        }
                        undoManager.addState(GameState(getBoardNoRef(), notes))
                    }
                    remainingUsesList = countRemainingUses(gameBoard)
                }
                ToolBarItem.Hint -> {
                    useHint()
                }

                ToolBarItem.Note -> {
                    notesToggled = !notesToggled
                    eraseButtonToggled = false
                }
                ToolBarItem.Remove -> {
                    if (inputMethod.value == 1 || eraseButtonToggled) {
                        toggleEraseButton()
                        return
                    }
                    if (currCell.row >= 0 && currCell.col >= 0 && !currCell.locked) {
                        val prevValue = gameBoard[currCell.row][currCell.col].value
                        val notesInCell =
                            notes.count { note -> note.row == currCell.row && note.col == currCell.col }
                        notes = clearNotesAtCell(notes)
                        gameBoard = setValueCell(0)
                        if (prevValue != 0 || notesInCell != 0) {
                            undoManager.addState(GameState(gameBoard, notes))
                        }
                    }
                }
            }
        }
    }

    private fun useHint() {
        if (currCell.row >= 0 && currCell.col >= 0 && !currCell.locked) {
            notes = clearNotesAtCell(notes, currCell.row, currCell.col)
            gameBoard = setValueCell(solvedBoard[currCell.row][currCell.col].value)

            val new = getBoardNoRef()
            new[currCell.row][currCell.col].error = false
            gameBoard = new

            duration = duration.plus(30.toDuration(DurationUnit.SECONDS))
            timeText = durationToString(duration)
            undoManager.addState(GameState(gameBoard, notes))
        }
    }

    fun resetGame(resetTimer: Boolean) {
        // stop and reset game
        notes = emptyNotes()
        currCell = Cell(-1, -1, 0)
        if (resetTimer) {
            duration = Duration.ZERO
            timeText = durationToString(duration)
        }
        digitFirstNumber = 0
        notesToggled = false
        undoManager.clear()

        // init a new game with initial board
        gameBoard = initialBoard.map { items -> items.map { item -> item.copy() } }

        remainingUsesList = countRemainingUses(gameBoard)
    }

    private fun isValidCell(
        board: List<List<Cell>> = getBoardNoRef(),
        cell: Cell
    ): List<List<Cell>> {
        solvedBoard.let {
            board[cell.row][cell.col].error =
                it[cell.row][cell.col].value != board[cell.row][cell.col].value
        }
        return board
    }

    private fun isCompleted(board: List<List<Cell>> = getBoardNoRef()): Boolean {
        solvedBoard.let {
            for (i in it.indices) {
                for (j in it.indices) {
                    if (it[i][j].value != board[i][j].value) {
                        return false
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val savedGame = savedGameRepository.get(boardEntity.uid)
            if (savedGame != null) {
                savedGameRepository.update(
                    savedGame.copy(
                        completed = true,
                        giveUp = false,
                        canContinue = false
                    )
                )
            }
        }
        return true
    }

    fun computeNotes() {
        notes = sudokuUtils.computeNotes(gameBoard, boardEntity.type)
        undoManager.addState(GameState(gameBoard, notes))
    }

    private fun autoEraseNotes(board: List<List<Cell>> = getBoardNoRef(), cell: Cell): List<Note> {
        if (currCell.row < 0 || currCell.col < 0) {
            return notes
        }
        return sudokuUtils.autoEraseNotes(board, notes, cell, boardEntity.type)
    }

    private suspend fun saveGame() {
        val savedGame = savedGameRepository.get(boardEntity.uid)
        val sudokuParser = SudokuParser()
        if (savedGame != null) {
            savedGameRepository.update(
                savedGame.copy(
                    timer = java.time.Duration.ofSeconds(duration.inWholeSeconds),
                    currentBoard = sudokuParser.boardToString(gameBoard),
                    notes = sudokuParser.notesToString(notes),
                    mistakes = mistakesCount
                )
            )
        } else {
            savedGameRepository.insert(
                SavedGame(
                    uid = boardEntity.uid,
                    currentBoard = sudokuParser.boardToString(gameBoard),
                    notes = sudokuParser.notesToString(notes),
                    timer = java.time.Duration.ofSeconds(duration.inWholeSeconds),
                    mistakes = mistakesCount
                )
            )
        }
    }

    private fun restoreSavedGame(savedGame: SavedGame?) {
        if (savedGame != null) {
            // restore timer and text
            duration = savedGame.timer.toKotlinDuration()
            timeText = durationToString(duration)

            mistakesCount = savedGame.mistakes
            val sudokuParser = SudokuParser()
            gameBoard = sudokuParser.parseBoard(
                savedGame.currentBoard,
                boardEntity.type
            )
            notes = sudokuParser.parseNotes(savedGame.notes)

            for (i in gameBoard.indices) {
                for (j in gameBoard.indices) {
                    gameBoard[i][j].locked = initialBoard[i][j].locked

                    if (gameBoard[i][j].value != 0 && !gameBoard[i][j].locked) {
                        if (mistakesMethod.value == 1) {
                            gameBoard[i][j].error =
                                !sudokuUtils.isValidCellDynamic(
                                    board = gameBoard,
                                    cell = gameBoard[i][j],
                                    type = boardEntity.type
                                )
                        } else {
                            gameBoard[i][j].error =
                                isValidCell(gameBoard, gameBoard[i][j])[i][j].error
                        }
                    }
                }
            }
        }
    }

    fun giveUp() {
        giveUp = true
        endGame = true
        currCell = Cell(-1, -1, 0)
        viewModelScope.launch(Dispatchers.IO) {
            val savedGame = savedGameRepository.get(boardEntity.uid)
            if (savedGame != null) {
                val sudokuParser = SudokuParser()
                savedGameRepository.update(
                    savedGame.copy(
                        timer = java.time.Duration.ofSeconds(duration.inWholeSeconds),
                        currentBoard = sudokuParser.boardToString(gameBoard),
                        completed = true,
                        giveUp = true,
                        mistakes = mistakesCount,
                        canContinue = false
                    )
                )
            }
        }
    }

    fun onGameComplete() {
        viewModelScope.launch(Dispatchers.IO) {
            saveGame()
            recordRepository.insert(
                Record(
                    board_uid = boardEntity.uid,
                    type = boardEntity.type,
                    difficulty = boardEntity.difficulty,
                    date = ZonedDateTime.now(),
                    time = duration.toJavaDuration()
                )
            )
        }
        pauseTimer()
        currCell = Cell(-1, -1, 0)
    }

    fun getFontSize(type: GameType = boardEntity.type, factor: Int): TextUnit {
        return sudokuUtils.getFontSize(type, factor)
    }

    fun setFirstGameFalse() {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setFirstGame(false)
        }
    }

    fun toggleEraseButton() {
        notesToggled = false
        currCell = Cell(-1, -1, 0)
        digitFirstNumber = -1
        eraseButtonToggled = !eraseButtonToggled
    }
}
