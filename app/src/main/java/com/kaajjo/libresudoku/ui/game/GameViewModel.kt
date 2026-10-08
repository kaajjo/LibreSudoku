package com.kaajjo.libresudoku.ui.game

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.QQWingController
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHint
import com.kaajjo.libresudoku.core.utils.GameState
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.core.utils.UndoRedoManager
import com.kaajjo.libresudoku.core.utils.toFormattedString
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import com.kaajjo.libresudoku.domain.usecase.board.GetBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.board.UpdateBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.record.GetAllRecordsUseCase
import com.kaajjo.libresudoku.navArgs
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import com.kaajjo.libresudoku.ui.game.models.GameSettings
import com.kaajjo.libresudoku.ui.game.models.GameUiEvent
import com.kaajjo.libresudoku.ui.game.models.GameUiSideEffect
import com.kaajjo.libresudoku.ui.game.models.GameUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

@HiltViewModel
class GameViewModel @Inject constructor(
    private val savedGameRepository: SavedGameRepository,
    private val appSettingsManager: AppSettingsManager,
    private val recordRepository: RecordRepository,
    private val updateBoardUseCase: UpdateBoardUseCase,
    private val getBoardUseCase: GetBoardUseCase,
    themeSettingsManager: ThemeSettingsManager,
    savedStateHandle: SavedStateHandle,
    private val getAllRecordsUseCase: GetAllRecordsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState = _uiState.asStateFlow()

    private val effectChannel = Channel<GameUiSideEffect>(Channel.BUFFERED)
    val effect = effectChannel.receiveAsFlow()

    private val state get() = _uiState.value
    private val navArgs: GameScreenNavArgs = savedStateHandle.navArgs()
    private val sudokuUtils = SudokuUtils()
    private val parser = SudokuParser()
    private val settingsReady = CompletableDeferred<Unit>()
    private var boardEntity: SudokuBoard? = null
    private var initialBoard: List<List<Cell>> = emptyList()
    private var undoRedoManager = UndoRedoManager(GameState(emptyList(), emptyList()))
    private var duration = Duration.ZERO
    private var timerJob: Job? = null
    private var hintJob: Job? = null
    private var loadJob: Job? = null
    private var recordsJob: Job? = null
    private var screenResumed = false
    private var playRequested = true
    private var overrideInputMethodDF = false

    // Capture before suspending and write in order, so an autosave cannot overwrite a result
    private data class SaveRequest(
        val board: SudokuBoard,
        val state: GameUiState,
        val duration: Duration,
        val recordCompletion: Boolean,
        val completed: CompletableDeferred<Unit> = CompletableDeferred()
    )

    private val saves = Channel<SaveRequest>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            for (request in saves) {
                try {
                    saveGame(request)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    produceSideEffect(GameUiSideEffect.SaveFailed)
                } finally {
                    request.completed.complete(Unit)
                }
            }
        }
        viewModelScope.launch {
            combine(
                appSettingsManager.firstGame.setting { copy(firstGame = it) },
                appSettingsManager.fontSize.setting { copy(fontSize = it) },
                appSettingsManager.keepScreenOn.setting { copy(keepScreenOn = it) },
                appSettingsManager.remainingUse.setting { copy(remainingUse = it) },
                appSettingsManager.timerEnabled.setting { copy(timerEnabled = it) },
                appSettingsManager.highlightIdentical.setting { copy(identicalHighlight = it) },
                appSettingsManager.highlightMistakes.setting { copy(mistakesMethod = it) },
                appSettingsManager.positionLines.setting { copy(positionLines = it) },
                themeSettingsManager.boardCrossHighlight.setting { copy(crossHighlight = it) },
                appSettingsManager.funKeyboardOverNumbers.setting { copy(funKeyboardOverNum = it) },
                appSettingsManager.mistakesLimit.setting { copy(mistakesLimit = it) },
                appSettingsManager.autoEraseNotes.setting { copy(autoEraseNotes = it) },
                appSettingsManager.resetTimerEnabled.setting { copy(resetTimerOnRestart = it) },
                appSettingsManager.hintsDisabled.setting { copy(disableHints = it) },
                appSettingsManager.inputMethod.setting { copy(inputMethod = it) },
                appSettingsManager.advancedHintEnabled.setting { copy(advancedHintEnabled = it) }
            ) { mutations ->
                mutations.fold(GameSettings()) { settings, mutate -> mutate(settings) }
            }.collect { settings ->
                val previous = state.settings
                _uiState.update { it.copy(settings = settings) }
                settingsReady.complete(Unit)
                if (!state.isLoading && !state.loadError) {
                    if (previous.mistakesMethod != settings.mistakesMethod) checkMistakesAll()
                    if (previous.inputMethod != settings.inputMethod) {
                        overrideInputMethodDF = false
                        _uiState.update { it.copy(digitFirstNumber = 0, currCell = Cell(-1, -1)) }
                    }
                    if (settings.firstGame) pauseTimer() else startTimer()
                }
            }
        }
        loadGame()
    }

    fun sendEvent(event: GameUiEvent) {
        if (!canHandleEvent(event)) return

        val previous = state
        when (event) {
            GameUiEvent.ScreenResumed -> onScreenResumed()
            GameUiEvent.ScreenPaused -> onScreenPaused()
            GameUiEvent.NavigateBack -> leaveScreen(GameUiSideEffect.NavigateBack)
            GameUiEvent.OpenSettings -> leaveScreen(GameUiSideEffect.OpenSettings)
            GameUiEvent.OpenHintSettings -> leaveScreen(GameUiSideEffect.OpenHintSettings)
            GameUiEvent.RetryLoading -> retryLoading()
            GameUiEvent.TogglePause -> togglePause()
            GameUiEvent.FirstGameFinished -> finishFirstGameOnboarding()
            is GameUiEvent.CellTapped -> onCellTapped(event)
            is GameUiEvent.NumberTapped -> processInputKeyboard(event.number, event.longTap)
            is GameUiEvent.ToolbarClicked -> toolbarClick(event.item)
            GameUiEvent.ToggleEraseMode -> toggleEraseMode()
            GameUiEvent.RequestAdvancedHint -> getAdvancedHint()
            GameUiEvent.CancelAdvancedHint -> cancelAdvancedHint()
            GameUiEvent.ApplyAdvancedHint -> applyAdvancedHint()
            GameUiEvent.ToggleSolution -> toggleSolution()
            GameUiEvent.ShowRestartDialog -> showRestartDialog()
            GameUiEvent.DismissRestartDialog -> dismissRestartDialog()
            GameUiEvent.ConfirmRestart -> confirmRestart()
            GameUiEvent.ShowGiveUpDialog -> showGiveUpDialog()
            GameUiEvent.DismissGiveUpDialog -> dismissGiveUpDialog()
            GameUiEvent.ConfirmGiveUp -> confirmGiveUp()
            is GameUiEvent.SetMenuVisible -> setMenuVisible(event.visible)
            is GameUiEvent.SetNotesMenuVisible -> setNotesMenuVisible(event.visible)
            is GameUiEvent.SetUndoRedoMenuVisible -> setUndoRedoMenuVisible(event.visible)
            GameUiEvent.ToggleRenderNotes -> toggleRenderNotes()
            GameUiEvent.ComputeNotes -> computeNotes()
            GameUiEvent.ClearNotes -> clearNotes()
            GameUiEvent.ExportBoard -> exportBoard()
        }

        onGameStateChanged(previous)
    }

    private fun canHandleEvent(event: GameUiEvent): Boolean = when (event) {
        GameUiEvent.ScreenResumed,
        GameUiEvent.ScreenPaused,
        GameUiEvent.NavigateBack,
        GameUiEvent.OpenSettings,
        GameUiEvent.OpenHintSettings,
        GameUiEvent.RetryLoading -> true
        else -> !state.isLoading && !state.loadError
    }

    private fun onScreenResumed() {
        screenResumed = true
        startTimer()
    }

    private fun onScreenPaused() {
        screenResumed = false
        pauseTimer()
        cancelAdvancedHint()
        _uiState.update { it.copy(currCell = Cell(-1, -1)) }
        queueSave()
    }

    private fun retryLoading() {
        if (state.loadError) loadGame()
    }

    private fun togglePause() {
        playRequested = !state.gamePlaying
        if (playRequested) startTimer() else pauseTimer()
        _uiState.update { it.copy(currCell = Cell(-1, -1)) }
    }

    private fun finishFirstGameOnboarding() {
        viewModelScope.launch {
            appSettingsManager.setFirstGame(false)
        }
    }

    private fun toggleEraseMode() {
        if (!state.gamePlaying) return
        toggleEraseButton()
        produceSideEffect(GameUiSideEffect.HapticFeedback)
    }

    private fun toggleSolution() {
        if (!state.endGame) return
        _uiState.update { it.copy(showSolution = !it.showSolution) }
    }

    private fun showRestartDialog() {
        if (state.endGame) return
        pauseTimer()
        cancelAdvancedHint()
        _uiState.update { it.copy(restartDialog = true) }
    }

    private fun dismissRestartDialog() {
        _uiState.update { it.copy(restartDialog = false) }
        startTimer()
    }

    private fun confirmRestart() {
        if (state.restartDialog) resetGame()
    }

    private fun showGiveUpDialog() {
        if (state.endGame) return
        pauseTimer()
        cancelAdvancedHint()
        _uiState.update { it.copy(giveUpDialog = true, showMenu = false) }
    }

    private fun dismissGiveUpDialog() {
        _uiState.update { it.copy(giveUpDialog = false) }
        startTimer()
    }

    private fun confirmGiveUp() {
        if (state.giveUpDialog) finishGame(giveUp = true)
    }

    private fun setMenuVisible(visible: Boolean) {
        _uiState.update { it.copy(showMenu = visible) }
    }

    private fun setNotesMenuVisible(visible: Boolean) {
        if (visible && !state.gamePlaying) return
        _uiState.update { it.copy(showNotesMenu = visible) }
        if (visible) produceSideEffect(GameUiSideEffect.HapticFeedback)
    }

    private fun setUndoRedoMenuVisible(visible: Boolean) {
        _uiState.update { it.copy(showUndoRedoMenu = visible && it.gamePlaying) }
    }

    private fun toggleRenderNotes() {
        _uiState.update { it.copy(renderNotes = !it.renderNotes) }
    }

    private fun computeNotes() {
        if (!state.gamePlaying) return
        _uiState.update { it.copy(notes = sudokuUtils.computeNotes(it.gameBoard, it.gameType)) }
        rememberMove()
    }

    private fun clearNotes() {
        if (!state.gamePlaying) return
        _uiState.update { it.copy(notes = emptyList()) }
        rememberMove()
    }

    private fun exportBoard() {
        val board = parser.boardToString(state.gameBoard, emptySeparator = '.').uppercase()
        produceSideEffect(GameUiSideEffect.CopyBoard(board))
    }

    private fun onGameStateChanged(previous: GameUiState) {
        if (previous.gameBoard != state.gameBoard || previous.notes != state.notes) {
            cancelAdvancedHint()
            when {
                state.endGame -> Unit
                state.settings.mistakesLimit && state.mistakesCount >= PreferencesConstants.MISTAKES_LIMIT ->
                    finishGame(giveUp = true)
                isCompleted() -> finishGame(giveUp = false)
                else -> queueSave()
            }
        }
    }

    private fun loadGame() {
        if (loadJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, loadError = false) }
        loadJob = viewModelScope.launch {
            try {
                settingsReady.await()
                val entity = getBoardUseCase(navArgs.gameUid)
                val saved = savedGameRepository.get(entity.uid).takeIf { navArgs.playedBefore }
                val initial = parser.parseBoard(entity.initialBoard, entity.type).map { row ->
                    row.map { it.copy(locked = it.value != 0) }
                }
                val solved = if (entity.solvedBoard.isNotBlank() && !entity.solvedBoard.contains('0')) {
                    parser.parseBoard(entity.solvedBoard, entity.type)
                } else {
                    val values = withContext(Dispatchers.Default) {
                        QQWingController().solve(entity.initialBoard.map { it.digitToInt(13) }.toIntArray(), entity.type)
                    }
                    List(entity.type.size) { row ->
                        List(entity.type.size) { col -> Cell(row, col, values[row * entity.type.size + col]) }
                    }.also { updateBoardUseCase(entity.copy(solvedBoard = parser.boardToString(it))) }
                }.mapIndexed { row, cells ->
                    cells.mapIndexed { col, cell -> cell.copy(locked = initial[row][col].locked) }
                }
                val board = saved?.let { parser.parseBoard(it.currentBoard, entity.type) } ?: initial
                val restoredBoard = board.mapIndexed { row, cells ->
                    cells.mapIndexed { col, cell -> cell.copy(locked = initial[row][col].locked) }
                }
                boardEntity = entity
                initialBoard = initial
                duration = saved?.timer?.toKotlinDuration() ?: Duration.ZERO
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        gameType = entity.type,
                        gameDifficulty = entity.difficulty,
                        gameBoard = restoredBoard,
                        solvedBoard = solved,
                        cages = entity.killerCages?.let(parser::parseKillerSudokuCages) ?: emptyList(),
                        notes = saved?.let { game -> parser.parseNotes(game.notes) } ?: emptyList(),
                        timeText = duration.toFormattedString(),
                        mistakesCount = saved?.mistakes ?: 0,
                        endGame = saved?.completed == true,
                        giveUp = saved?.giveUp == true,
                        remainingUsesList = countRemainingUses(restoredBoard)
                    )
                }
                checkMistakesAll()
                undoRedoManager = UndoRedoManager(GameState(state.gameBoard, state.notes))
                recordsJob?.cancel()
                recordsJob = viewModelScope.launch {
                    getAllRecordsUseCase(entity.difficulty, entity.type).collect { records ->
                        _uiState.update { it.copy(allRecords = records) }
                    }
                }
                queueSave()
                startTimer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GameViewModel", "Failed to load game ${navArgs.gameUid}", e)
                _uiState.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    private fun onCellTapped(event: GameUiEvent.CellTapped) {
        if (state.endGame || event.row !in state.gameBoard.indices || event.col !in state.gameBoard.indices) return
        if (!state.gamePlaying) {
            if (!event.longTap) {
                playRequested = true
                startTimer()
                if (state.gamePlaying) produceSideEffect(GameUiSideEffect.HapticFeedback)
            }
            return
        }
        val cell = state.gameBoard[event.row][event.col]
        val selected = if (state.currCell.row == cell.row && state.currCell.col == cell.col && state.digitFirstNumber == 0) {
            Cell(-1, -1)
        } else cell.copy()
        _uiState.update { it.copy(currCell = selected) }
        if (selected.row < 0 || selected.locked) return
        if ((state.settings.inputMethod == 1 || overrideInputMethodDF) && state.digitFirstNumber > 0) {
            if (event.longTap) {
                setValueCell(0)
                setNote(state.digitFirstNumber)
                rememberMove()
            } else if (!state.settings.remainingUse || state.remainingUsesList[state.digitFirstNumber - 1] > 0) {
                processNumberInput(state.digitFirstNumber)
                rememberMove()
                if (state.notesToggled) _uiState.update { it.copy(currCell = selected.copy(value = it.digitFirstNumber)) }
            }
        } else if (state.eraseButtonToggled) {
            processNumberInput(0)
            rememberMove()
        }
        if (event.longTap) produceSideEffect(GameUiSideEffect.HapticFeedback)
    }

    private fun processInputKeyboard(number: Int, longTap: Boolean) {
        if (!state.gamePlaying || number !in 1..state.size) return
        if (!longTap) {
            if (state.settings.inputMethod == 0 && selectedCellEditable()) {
                overrideInputMethodDF = false
                _uiState.update { it.copy(digitFirstNumber = 0) }
                processNumberInput(number)
                rememberMove()
            } else if (state.settings.inputMethod == 1) {
                selectDigit(number)
            }
        } else if (state.settings.inputMethod == 0) {
            overrideInputMethodDF = true
            selectDigit(number)
        }
        _uiState.update { it.copy(eraseButtonToggled = false) }
    }

    private fun selectDigit(number: Int) {
        _uiState.update {
            val digit = if (it.digitFirstNumber == number) 0 else number
            it.copy(digitFirstNumber = digit, currCell = Cell(-1, -1, digit))
        }
    }

    private fun selectedCellEditable(): Boolean = state.currCell.let {
        it.row in state.gameBoard.indices && it.col in state.gameBoard.indices && !state.gameBoard[it.row][it.col].locked
    }

    private fun processNumberInput(number: Int) {
        if (!state.gamePlaying || !selectedCellEditable()) return
        if (state.notesToggled && number > 0) {
            setValueCell(0)
            setNote(number)
        } else {
            clearNotesAtCell()
            val cell = state.currCell
            setValueCell(if (state.gameBoard[cell.row][cell.col].value == number) 0 else number)
        }
    }

    private fun clearNotesAtCell() {
        _uiState.update { current ->
            current.copy(notes = current.notes.filterNot { it.row == current.currCell.row && it.col == current.currCell.col })
        }
    }

    private fun setNote(number: Int) {
        val note = Note(state.currCell.row, state.currCell.col, number)
        _uiState.update {
            if (note in it.notes) it.copy(notes = it.notes - note)
            else it.copy(notes = it.notes + note, notesTaken = it.notesTaken + 1)
        }
    }

    private fun setValueCell(value: Int, countMistake: Boolean = true) {
        val current = state
        val selected = current.currCell
        val board = current.gameBoard.copyCells()
        board[selected.row][selected.col].value = value
        val checked = checkedBoard(board)
        val cell = checked[selected.row][selected.col]
        if (!countMistake) cell.error = false
        _uiState.update {
            it.copy(
                gameBoard = checked,
                currCell = cell.copy(),
                remainingUsesList = countRemainingUses(checked),
                mistakesMade = it.mistakesMade + if (countMistake && cell.error) 1 else 0,
                mistakesCount = it.mistakesCount + if (countMistake && cell.error && it.settings.mistakesLimit) 1 else 0,
                notes = if (value != 0 && it.settings.autoEraseNotes) {
                    sudokuUtils.autoEraseNotes(checked, it.notes, cell, it.gameType)
                } else it.notes
            )
        }
    }

    private fun checkedBoard(board: List<List<Cell>>): List<List<Cell>> = board.map { row ->
        row.map { cell ->
            val error = if (cell.value == 0 || cell.locked) false else when (state.settings.mistakesMethod) {
                1 -> !sudokuUtils.isValidCellDynamic(board, cell, state.gameType)
                2 -> state.solvedBoard[cell.row][cell.col].value != cell.value
                else -> false
            }
            cell.copy(error = error)
        }
    }

    private fun checkMistakesAll() {
        val board = checkedBoard(state.gameBoard)
        _uiState.update {
            it.copy(gameBoard = board, currCell = board.getOrNull(it.currCell.row)?.getOrNull(it.currCell.col)?.copy() ?: it.currCell)
        }
    }

    private fun countRemainingUses(board: List<List<Cell>>) =
        (1..board.size).map { board.size - sudokuUtils.countNumberInBoard(board, it) }

    private fun rememberMove() {
        undoRedoManager.addState(GameState(state.gameBoard, state.notes))
    }

    private fun toolbarClick(item: ToolBarItem) {
        if (!state.gamePlaying) return
        when (item) {
            ToolBarItem.Undo -> if (undoRedoManager.canUndo()) restoreMove(undoRedoManager.undo())
            ToolBarItem.Redo -> if (undoRedoManager.canRedo()) undoRedoManager.redo()?.let(::restoreMove)
            ToolBarItem.Hint -> if (!state.settings.disableHints) useHint()
            ToolBarItem.Note -> _uiState.update { it.copy(notesToggled = !it.notesToggled, eraseButtonToggled = false) }
            ToolBarItem.Remove -> {
                if (state.settings.inputMethod == 1 || state.eraseButtonToggled) toggleEraseButton()
                else if (selectedCellEditable()) {
                    clearNotesAtCell()
                    setValueCell(0)
                    rememberMove()
                }
            }
        }
    }

    private fun restoreMove(move: GameState) {
        val board = checkedBoard(move.board)
        _uiState.update {
            it.copy(
                gameBoard = board, notes = move.notes, remainingUsesList = countRemainingUses(board),
                currCell = board.getOrNull(it.currCell.row)?.getOrNull(it.currCell.col)?.copy() ?: it.currCell
            )
        }
    }

    private fun toggleEraseButton() {
        _uiState.update {
            it.copy(notesToggled = false, currCell = Cell(-1, -1), digitFirstNumber = -1, eraseButtonToggled = !it.eraseButtonToggled)
        }
    }

    private fun useHint() {
        if (!selectedCellEditable()) return
        clearNotesAtCell()
        setValueCell(state.solvedBoard[state.currCell.row][state.currCell.col].value, countMistake = false)
        duration += 30.seconds
        _uiState.update { it.copy(timeText = duration.toFormattedString(), hintsUsed = it.hintsUsed + 1) }
        rememberMove()
    }

    private fun resetGame() {
        cancelAdvancedHint()
        if (state.settings.resetTimerOnRestart) duration = Duration.ZERO
        overrideInputMethodDF = false
        val board = initialBoard.copyCells()
        _uiState.update {
            GameUiState(
                isLoading = false, settings = it.settings, gameType = it.gameType, gameDifficulty = it.gameDifficulty,
                gameBoard = board, solvedBoard = it.solvedBoard, cages = it.cages,
                remainingUsesList = countRemainingUses(board), timeText = duration.toFormattedString(),
                allRecords = it.allRecords, renderNotes = it.renderNotes
            )
        }
        undoRedoManager = UndoRedoManager(GameState(state.gameBoard, state.notes))
        playRequested = true
        startTimer()
        queueSave()
        produceSideEffect(GameUiSideEffect.GameRestarted)
    }

    private fun isCompleted(): Boolean = state.solvedBoard.isNotEmpty() && state.gameBoard.indices.all { row ->
        state.gameBoard[row].indices.all { col -> state.gameBoard[row][col].value == state.solvedBoard[row][col].value }
    }

    private fun finishGame(giveUp: Boolean) {
        if (state.endGame) return
        pauseTimer()
        cancelAdvancedHint()
        _uiState.update {
            it.copy(endGame = true, giveUp = giveUp, currCell = Cell(-1, -1), giveUpDialog = false, showMenu = false)
        }
        queueSave(recordCompletion = !giveUp)
    }

    private fun startTimer() {
        if (!screenResumed || !playRequested || state.isLoading || state.loadError || state.endGame ||
            state.settings.firstGame || state.restartDialog || state.giveUpDialog || timerJob?.isActive == true) return
        _uiState.update { it.copy(gamePlaying = true) }
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(50)
                val previousSeconds = duration.inWholeSeconds
                duration += 50.milliseconds
                if (previousSeconds != duration.inWholeSeconds) {
                    _uiState.update { it.copy(timeText = duration.toFormattedString()) }
                    queueSave()
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(gamePlaying = false) }
    }

    private fun queueSave(recordCompletion: Boolean = false): CompletableDeferred<Unit>? {
        val entity = boardEntity ?: return null
        if (state.isLoading || state.loadError) return null
        val request = SaveRequest(entity, state, duration, recordCompletion)
        return if (saves.trySend(request).isSuccess) request.completed else null
    }

    private suspend fun saveGame(request: SaveRequest) {
        val snapshot = request.state
        val existing = savedGameRepository.get(request.board.uid)
        val now = ZonedDateTime.now()
        val saved = SavedGame(
            uid = request.board.uid,
            currentBoard = parser.boardToString(snapshot.gameBoard),
            notes = parser.notesToString(snapshot.notes),
            timer = request.duration.toJavaDuration(),
            mistakes = snapshot.mistakesCount,
            completed = snapshot.endGame,
            giveUp = snapshot.giveUp,
            canContinue = !snapshot.endGame,
            lastPlayed = now,
            startedAt = existing?.startedAt ?: now,
            finishedAt = if (snapshot.endGame) existing?.finishedAt ?: now else null
        )
        if (existing == null) savedGameRepository.insert(saved) else savedGameRepository.update(saved)
        if (request.recordCompletion) {
            recordRepository.insert(
                Record(
                    board_uid = request.board.uid, type = request.board.type, difficulty = request.board.difficulty,
                    date = now, time = request.duration.toJavaDuration()
                )
            )
        }
    }

    private fun getAdvancedHint() {
        if (!state.gamePlaying || !state.settings.advancedHintEnabled) return
        cancelAdvancedHint()
        val snapshot = state
        _uiState.update { it.copy(advancedHintMode = true, advancedHintLoading = true, currCell = Cell(-1, -1)) }
        hintJob = viewModelScope.launch {
            val settings = appSettingsManager.advancedHintSettings.first()
            val hint = withContext(Dispatchers.Default) {
                AdvancedHint(
                    type = snapshot.gameType, board = snapshot.gameBoard.copyCells(),
                    solvedBoard = snapshot.solvedBoard.copyCells(), notes = snapshot.notes, settings = settings
                ).getEasiestHint()
            }
            _uiState.update { it.copy(advancedHintData = hint, advancedHintLoading = false) }
        }
    }

    private fun cancelAdvancedHint() {
        hintJob?.cancel()
        hintJob = null
        _uiState.update { it.copy(advancedHintMode = false, advancedHintLoading = false, advancedHintData = null) }
    }

    private fun applyAdvancedHint() {
        if (!state.gamePlaying) return
        val target = state.advancedHintData?.targetCell ?: return
        if (target.row !in state.gameBoard.indices || target.col !in state.gameBoard.indices) return
        _uiState.update { it.copy(currCell = it.gameBoard[target.row][target.col].copy()) }
        if (selectedCellEditable()) {
            clearNotesAtCell()
            setValueCell(if (state.currCell.value == target.value) 0 else target.value)
            rememberMove()
        }
        cancelAdvancedHint()
    }

    private fun leaveScreen(effect: GameUiSideEffect) {
        screenResumed = false
        pauseTimer()
        cancelAdvancedHint()
        _uiState.update { it.copy(showMenu = false) }
        val saved = queueSave()
        viewModelScope.launch {
            saved?.await()
            effectChannel.send(effect)
        }
    }

    private fun produceSideEffect(effect: GameUiSideEffect) {
        viewModelScope.launch { effectChannel.send(effect) }
    }
}

private fun List<List<Cell>>.copyCells() = map { row -> row.map { it.copy() } }

private fun <T> Flow<T>.setting(mutation: GameSettings.(T) -> GameSettings): Flow<(GameSettings) -> GameSettings> =
    map { value -> { settings -> settings.mutation(value) } }
