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
import com.kaajjo.libresudoku.ui.game.components.ToolBardItem
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
                initialBoard.forEach { cells -> cells.forEach { cell -> cell.locked = cell.value != 0 } }
                solvedBoard = sudokuParser.parseBoard(
                    boardEntity.solvedBoard,
                    boardEntity.type
                )
            }

            withContext(Dispatchers.Main) {
                if(savedGame != null && continueSaved!!) {
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
    var restartDialog by  mutableStateOf(false)
    var showMenu by mutableStateOf(false)
    var showNotesMenu by mutableStateOf(false)

    // считать количество оставшихся использований
    var remainingUse = appSettingsManager.remainingUse

    // таймер
    var timerEnabled = appSettingsManager.timerEnabled

    // подсветка одинаковых чисел
    val identicalHighlight = appSettingsManager.highlightIdentical

    // подсветка ошибок
    var errorHighlight = appSettingsManager.highlightMistakes
    // метод проверки чисел.
    // True - конфликтующие числа. False - серяем с конечным решением
    private var errorMethodConflict = errorHighlight.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

    // подсветка строки и столбца текущей клетки
    var positionLines = appSettingsManager.positionLines

    // считать и ограничить количество ошибок
    var mistakesLimit = appSettingsManager.mistakesLimit
    var mistakesLimitValue = mistakesLimit.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // автоматически стирать заметки
    private val autoEraseNotes = appSettingsManager.autoEraseNotes
    private var autoEraseNotesValue = autoEraseNotes.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    // сбрасывать таймер при рестарте
    var resetTimerOnRestart = appSettingsManager.resetTimerEnabled

    // вкл/выкл кнопка подсказок
    var disableHints = appSettingsManager.hintsDisabled

    var endGame by mutableStateOf(false)
    var giveUpDialog by mutableStateOf(false)

    // ошибки
    var mistakesCount by mutableStateOf(0)
    var mistakesLimitDialog by mutableStateOf(false)
    // заметки
    var notesToggled by mutableStateOf(false)
    var notes by mutableStateOf(emptyList<Note>())

    // игровое поле
    private lateinit var initialBoard: List<List<Cell>>
    var gameBoard by mutableStateOf(List(9) { row -> List(9) { col -> Cell(row, col, 0) } } )
    var solvedBoard = List(9) { row -> List(9) { col -> Cell(row, col, 0) } }

    var currCell by mutableStateOf(Cell(-1, -1, 0))
    private var undoManager = UndoManager(GameState(gameBoard, notes))
    private var sudokuUtils = SudokuUtils()
    var gameCompleted by mutableStateOf(false)

    // выбранное число для ввода "сперва число"
    var fdSelectedNumber by mutableStateOf(0)
    val inputMethod = appSettingsManager.inputMethod
    // при лонгтапе метод ввода переключается на сперва число
    var overrideInputMethodFD by mutableStateOf(false)

    // очищает заметки в заданной клетке
    private fun clearNotesAtCell(notes: List<Note>, row: Int = currCell.row, col: Int = currCell.col) : List<Note> {
        return notes.minus(
            notes.filter { note ->
                note.row == row
                        && note.col == col
            }.toSet()
        )
    }

    // очищает все заметки на поле
    private fun emptyNotes() : List<Note> = emptyList()

    fun clearNotes() {
        notes = emptyNotes()
        undoManager.addState(
            GameState(gameBoard, notes)
        )
    }
    // добавляет заметку в клетку
    private fun addNote(note: Int, row: Int, col: Int) : List<Note> {
        return notes.plus(Note(row, col, note))
    }

    // удаляет заметку из клетки
    private fun removeNote(note: Int, row: Int, col: Int) : List<Note> = notes.minus(Note(row, col, note))

    // возвращает игровое поле с отличающимися ссылками (рекомпозиция завязана на ссылках)
    private fun getBoardNoRef():List<List<Cell>> = gameBoard.map { items -> items.map { item -> item.copy() } }

    // устаналивает значение в клетке (в выбранную клетку, если не указано другое)
    private fun setValueCell(value: Int, row: Int = currCell.row, col: Int = currCell.col) : List<List<Cell>> {
        // такое копирование и присовоение чтобы избавиться от ссылок на объекты чтобы рекомпозиция произошла
        var new = getBoardNoRef()

        // сохраняем стейт
        //if(!(value == 0 && new[row][col].value == 0)) {
        //    undoManager.addState(GameState(new, notes))
        //}

        // ставим число в клетку
        new[row][col].value = value
        // пересчитываем использования
        remainingUsesList = countRemainingUses(new)

        // если это выбранная клетка, то обновляем ее состояние
        if(currCell.row == row && currCell.col == col) {
            currCell = currCell.copy(value = new[row][col].value)
        }
        if(value == 0) {
            new[row][col].error = false
            currCell.error = false
            return new
        }
        // проверка числа
        if(errorMethodConflict.value == 1) {
            // проверка основываясь на текущем поле (конфликтующие числа)
            // проверяем текущую клетку
            new[row][col].error = !sudokuUtils.isValidCellDynamic(new, new[row][col], boardEntity.type)
            // проверяем все неверные клетки
            new.forEach { cells ->
                cells.forEach { cell ->
                    if(cell.value != 0 && cell.error) {
                        cell.error = !sudokuUtils.isValidCellDynamic(new, cell, boardEntity.type)
                    }
                }
            }
        } else if(errorMethodConflict.value == 2) {
            // проверка основываясь на конечном решении
            new = isValidCell(new, new[row][col])
        }
        currCell.error = currCell.value == 0
        // считаем ошибки, если лимит то пиздец
        if(mistakesLimitValue.value && new[row][col].error) {
            mistakesCount++
            if(mistakesCount >= 3) {
                // выводим диалог о 3х ошибка, останавливаем таймер, останаливаем игру
                mistakesLimitDialog = true
                pauseTimer()
                giveUp()
                endGame = true
            }
        }

        // проверяем решена ли судоку
        gameCompleted = isCompleted(new)

        // атоудаление заметок
        if(autoEraseNotesValue.value) {
            notes = autoEraseNotes(new, currCell)
        }

        return new
    }

    // Считает количество оставшихся использований для каждого числа
    // Используется для вывода оставшихся испольозваний под цифрами клавиатуры
    private fun countRemainingUses(board: List<List<Cell>>): MutableList<Int> {
        val uses = mutableListOf<Int>()
        for(i in 0..size) {
            uses.add(size - sudokuUtils.countNumberInBoard(board, i + 1))
        }
        return uses
    }

    fun processInput(inputMethod: Int, cell: Cell, remainingUse: Boolean, longTap: Boolean = false): Boolean {
        if(gamePlaying) {
            currCell =
                if(currCell.row == cell.row && currCell.col == cell.col && fdSelectedNumber == 0) Cell(-1, -1) else cell

            if(currCell.row >= 0 && currCell.col >= 0) {
                if(inputMethod == 1 || overrideInputMethodFD) {
                    if(!longTap) {
                        if(fdSelectedNumber > 0 &&
                            (remainingUsesList.size >= fdSelectedNumber &&
                                    remainingUsesList[fdSelectedNumber - 1] > 0) ||
                            !remainingUse
                        ) {
                            processNumberInput(fdSelectedNumber)
                            undoManager.addState(GameState(gameBoard, notes))
                            if(notesToggled) currCell = Cell(currCell.row, currCell.col, fdSelectedNumber)
                        }
                    } else {
                        setNote(fdSelectedNumber)
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

    fun processInputKeyboard(number: Int, inputMethod: Int, longTap: Boolean = false) {
        if(gamePlaying) {
            if(!longTap) {
                if(inputMethod == 0) {
                    overrideInputMethodFD = false
                    fdSelectedNumber = 0
                    processNumberInput(number)
                    undoManager.addState(GameState(gameBoard, notes))
                } else if(inputMethod == 1){
                    fdSelectedNumber = if(fdSelectedNumber == number) 0 else number
                    currCell = Cell(-1,-1, fdSelectedNumber)
                }
            } else {
                if(inputMethod == 0) {
                    overrideInputMethodFD = true
                    fdSelectedNumber = if(fdSelectedNumber == number) 0 else number
                    currCell = Cell(-1,-1, fdSelectedNumber)
                }
            }
        }
    }


    // Обработка числа с клавиатуры
    fun processNumberInput(number: Int) {
        if(currCell.row >= 0 && currCell.col >= 0 && gamePlaying && !currCell.locked) {
            if(!notesToggled) {
                // Удаляем все затметки с клетки, чтобы поставить в ней число
                notes = clearNotesAtCell(notes, currCell.row, currCell.col)

                gameBoard = setValueCell(
                    value = if(gameBoard[currCell.row][currCell.col].value == number) 0 else number
                )
            } else {
                gameBoard = setValueCell(0)
                setNote(number)
                remainingUsesList = countRemainingUses(gameBoard)
            }
        }
    }

    fun setNote(number: Int) {
        // Создаме заметку. Если такая уже есть, то удаляем
        val note = Note(currCell.row, currCell.col, number)
        notes = if(notes.contains(note)) {
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
        if(!gamePlaying) {
            gamePlaying = true
            // Периодичность обновления таймера в миллисекундах
            val updateRate = 50L

            timer = fixedRateTimer(initialDelay = updateRate, period = updateRate) {
                val prevTime = duration

                duration = duration.plus((updateRate * 1e6).toDuration(DurationUnit.NANOSECONDS))
                // обновляем ui только при измении секунд
                if(prevTime.toInt(DurationUnit.SECONDS) != duration.toInt(DurationUnit.SECONDS)) {
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

    // обработка команд с клавиатуры с инструментами
    fun toolBoardClick(item: ToolBardItem) {
        if(gamePlaying) {
            when(item) {
                ToolBardItem.Undo -> {
                    if(undoManager.count() > 0) {
                        // ставим предыдущий стейт
                        undoManager.getPrevState().also {
                            gameBoard = it.board
                            notes = it.notes
                        }
                        // добавляем его наверх
                        undoManager.addState(GameState(getBoardNoRef(), notes))
                    }
                    remainingUsesList = countRemainingUses(gameBoard)
                }
                ToolBardItem.Hint -> {
                    useHint()
                }

                ToolBardItem.Note -> { notesToggled = !notesToggled }
                ToolBardItem.Remove -> {
                    if(currCell.row >= 0 && currCell.col >= 0 && !currCell.locked) {
                        val prevValue = gameBoard[currCell.row][currCell.col].value
                        val notesInCell =  notes.count { note -> note.row == currCell.row && note.col == currCell.col }
                        notes = clearNotesAtCell(notes)
                        gameBoard = setValueCell(0)
                        if(prevValue != 0 || notesInCell != 0) {
                            undoManager.addState(GameState(gameBoard, notes))
                        }
                    }
                }
            }
        }
    }

    private fun useHint() {
        if(currCell.row >= 0 && currCell.col >= 0 && !currCell.locked) {
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
        currCell = Cell(-1,-1,0)
        if(resetTimer) {
            duration = Duration.ZERO
            timeText = durationToString(duration)
        }
        fdSelectedNumber = 0
        notesToggled = false
        undoManager.clear()

        // init a new game with initial board
        gameBoard = initialBoard.map { items -> items.map { item -> item.copy() } }

        remainingUsesList = countRemainingUses(gameBoard)
    }

    private fun isValidCell(board: List<List<Cell>> = getBoardNoRef(), cell: Cell) : List<List<Cell>> {
        solvedBoard.let {
            board[cell.row][cell.col].error = it[cell.row][cell.col].value != board[cell.row][cell.col].value
        }
        return board
    }

    private fun isCompleted(board: List<List<Cell>> = getBoardNoRef()) : Boolean {
        solvedBoard.let {
            for(i in it.indices) {
                for(j in it.indices) {
                    if(it[i][j].value != board[i][j].value) {
                        return false
                    }
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            val savedGame = savedGameRepository.get(boardEntity.uid)
            if(savedGame != null) {
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

    // заполняет все пустые клетки числами, которые могут стоять в клетке
    fun computeNotes() {
        notes = sudokuUtils.computeNotes(gameBoard, boardEntity.type)
        undoManager.addState(GameState(gameBoard, notes))
    }

    // возвращает список всех заметок без заметок, которые имеют значение,
    // конфликтующее с заданной клеткой
    private fun autoEraseNotes(board: List<List<Cell>> = getBoardNoRef(), cell: Cell) : List<Note> {
        if(currCell.row <0 || currCell.col < 0) {
            return notes
        }
        return sudokuUtils.autoEraseNotes(board, notes, cell, boardEntity.type)
    }

    private suspend fun saveGame() {
        val savedGame = savedGameRepository.get(boardEntity.uid)
        val sudokuParser = SudokuParser()
        if(savedGame != null) {
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
        if(savedGame != null) {
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

            for(i in gameBoard.indices) {
                for(j in gameBoard.indices) {
                    gameBoard[i][j].locked = initialBoard[i][j].locked

                    if(gameBoard[i][j].value != 0 && !gameBoard[i][j].locked) {
                        if(errorMethodConflict.value == 1) {
                            gameBoard[i][j].error =
                                !sudokuUtils.isValidCellDynamic(
                                    board = gameBoard,
                                    cell = gameBoard[i][j],
                                    type = boardEntity.type
                                )
                        } else {
                            gameBoard[i][j].error = isValidCell(gameBoard,  gameBoard[i][j])[i][j].error
                        }
                    }
                }
            }
        }
    }

    fun giveUp() {
        giveUp = true
        endGame = true
        currCell = Cell(-1,-1,0)
        viewModelScope.launch(Dispatchers.IO) {
            val savedGame = savedGameRepository.get(boardEntity.uid)
            if(savedGame != null) {
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
}
