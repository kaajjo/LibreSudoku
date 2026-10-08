package com.kaajjo.libresudoku.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHintSettings
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import com.kaajjo.libresudoku.domain.usecase.board.GetBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.board.UpdateBoardUseCase
import com.kaajjo.libresudoku.domain.usecase.record.GetAllRecordsUseCase
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import com.kaajjo.libresudoku.ui.game.models.GameSettings
import com.kaajjo.libresudoku.ui.game.models.GameUiEvent
import com.kaajjo.libresudoku.ui.game.models.GameUiSideEffect
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val boardRepository = mockk<BoardRepository>(relaxed = true)
    private val savedGameRepository = mockk<SavedGameRepository>(relaxed = true)
    private val recordRepository = mockk<RecordRepository>(relaxed = true)
    private val appSettings = mockk<AppSettingsManager>()
    private val themeSettings = mockk<ThemeSettingsManager>()
    private val viewModels = mutableListOf<GameViewModel>()
    private val savedGames = mutableListOf<SavedGame>()
    private var persistedGame: SavedGame? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        stubSettings(GameSettings(inputMethod = 0, remainingUse = false))
        coEvery { boardRepository.get(BOARD_ID) } returns board()
        every { recordRepository.getAll(any(), any()) } returns flowOf(emptyList())
        coEvery { savedGameRepository.get(BOARD_ID) } answers { persistedGame }
        coEvery { savedGameRepository.insert(any<SavedGame>()) } answers {
            firstArg<SavedGame>().also {
                persistedGame = it
                savedGames += it
            }.uid
        }
        coEvery { savedGameRepository.update(any()) } answers {
            persistedGame = firstArg()
            savedGames += firstArg<SavedGame>()
        }
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `input while loading does not change the game`() = runGameTest {
        val pendingBoard = CompletableDeferred<SudokuBoard>()
        coEvery { boardRepository.get(BOARD_ID) } coAnswers { pendingBoard.await() }
        val viewModel = createViewModel()
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()
        val loading = viewModel.uiState.value
        assertTrue(loading.isLoading)

        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Note))
        viewModel.sendEvent(GameUiEvent.ComputeNotes)
        runCurrent()

        assertEquals(loading.gameBoard, viewModel.uiState.value.gameBoard)
        assertEquals(loading.currCell, viewModel.uiState.value.currCell)
        assertEquals(loading.notes, viewModel.uiState.value.notes)
        assertEquals(loading.notesToggled, viewModel.uiState.value.notesToggled)
        pendingBoard.complete(board())
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(0, viewModel.uiState.value.gameBoard[0][0].value)
    }

    @Test
    fun `input while paused does not change the game`() = runGameTest {
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.TogglePause)
        runCurrent()
        val paused = viewModel.uiState.value
        assertFalse(paused.gamePlaying)

        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Note))
        viewModel.sendEvent(GameUiEvent.ComputeNotes)
        runCurrent()

        assertEquals(paused.gameBoard, viewModel.uiState.value.gameBoard)
        assertEquals(paused.currCell, viewModel.uiState.value.currCell)
        assertEquals(paused.notes, viewModel.uiState.value.notes)
        assertEquals(paused.notesToggled, viewModel.uiState.value.notesToggled)

        // Tapping the paused board resumes play but does not select or edit a cell.
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 1))
        runCurrent()
        assertTrue(viewModel.uiState.value.gamePlaying)
        assertEquals(paused.gameBoard, viewModel.uiState.value.gameBoard)
        assertEquals(paused.currCell, viewModel.uiState.value.currCell)
    }

    @Test
    fun `edits undo and redo never mutate previously emitted snapshots`() = runGameTest {
        val viewModel = loadedGame()
        val initial = viewModel.uiState.value
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        runCurrent()
        val edited = viewModel.uiState.value
        assertEquals(5, edited.gameBoard[0][0].value)

        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Undo))
        runCurrent()
        val undone = viewModel.uiState.value
        assertEquals(0, undone.gameBoard[0][0].value)
        assertEquals(5, edited.gameBoard[0][0].value)

        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Redo))
        runCurrent()
        assertEquals(5, viewModel.uiState.value.gameBoard[0][0].value)
        assertEquals(0, initial.gameBoard[0][0].value)
        assertEquals(0, undone.gameBoard[0][0].value)
        assertEquals(5, edited.currCell.value)
    }

    @Test
    fun `restart clears mistakes and prevents undo from restoring the previous game`() = runGameTest {
        stubSettings(
            GameSettings(inputMethod = 0, remainingUse = false, mistakesLimit = true, mistakesMethod = 2)
        )
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(3))
        runCurrent()
        assertEquals(1, viewModel.uiState.value.mistakesCount)
        assertTrue(viewModel.uiState.value.gameBoard[0][0].error)

        viewModel.sendEvent(GameUiEvent.ShowRestartDialog)
        viewModel.sendEvent(GameUiEvent.ConfirmRestart)
        runCurrent()
        val restarted = viewModel.uiState.value
        assertEquals(0, restarted.mistakesCount)
        assertEquals(0, restarted.mistakesMade)
        assertEquals(0, restarted.gameBoard[0][0].value)
        assertFalse(restarted.gameBoard[0][0].error)
        assertFalse(restarted.restartDialog)
        assertFalse(restarted.endGame)

        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Undo))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Redo))
        runCurrent()
        assertEquals(restarted.gameBoard, viewModel.uiState.value.gameBoard)
        assertEquals(0, viewModel.uiState.value.mistakesCount)
    }

    @Test
    fun `last digit persists the completed board and record exactly once`() = runGameTest {
        coEvery { boardRepository.get(BOARD_ID) } returns board(emptyCells = 1)
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        runCurrent()

        assertTrue(viewModel.uiState.value.endGame)
        assertFalse(viewModel.uiState.value.gamePlaying)
        assertFalse(viewModel.uiState.value.giveUp)
        assertEquals(SOLUTION, persistedGame?.currentBoard)
        assertEquals(true, persistedGame?.completed)
        assertEquals(false, persistedGame?.canContinue)
        assertEquals(false, persistedGame?.giveUp)
        assertEquals(1, savedGames.count { it.completed })
        coVerify(exactly = 1) { recordRepository.insert(any<Record>()) }

        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Undo))
        runCurrent()
        assertEquals(SOLUTION, persistedGame?.currentBoard)
        assertEquals(1, savedGames.count { it.completed })
        coVerify(exactly = 1) { recordRepository.insert(any<Record>()) }
    }

    @Test
    fun `background time is excluded and repeated resume does not duplicate the timer`() = runGameTest {
        val viewModel = loadedGame()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("00:02", viewModel.uiState.value.timeText)

        viewModel.sendEvent(GameUiEvent.ScreenPaused)
        runCurrent()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals("00:02", viewModel.uiState.value.timeText)
        assertFalse(viewModel.uiState.value.gamePlaying)

        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("00:03", viewModel.uiState.value.timeText)
        assertTrue(viewModel.uiState.value.gamePlaying)
    }

    @Test
    fun `manual pause survives background and foreground transitions`() = runGameTest {
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.TogglePause)
        viewModel.sendEvent(GameUiEvent.ScreenPaused)
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()
        assertFalse(viewModel.uiState.value.gamePlaying)
        assertEquals("00:00", viewModel.uiState.value.timeText)

        viewModel.sendEvent(GameUiEvent.TogglePause)
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(viewModel.uiState.value.gamePlaying)
        assertEquals("00:01", viewModel.uiState.value.timeText)
    }

    @Test
    fun `onboarding blocks play until the saved first game preference changes`() = runGameTest {
        val firstGame = MutableStateFlow(true)
        every { appSettings.firstGame } returns firstGame
        coEvery { appSettings.setFirstGame(false) } returns Unit
        val viewModel = createViewModel()
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.settings.firstGame)
        assertFalse(viewModel.uiState.value.gamePlaying)

        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, viewModel.uiState.value.gameBoard[0][0].value)
        assertEquals("00:00", viewModel.uiState.value.timeText)

        viewModel.sendEvent(GameUiEvent.FirstGameFinished)
        runCurrent()
        coVerify(exactly = 1) { appSettings.setFirstGame(false) }
        assertFalse(viewModel.uiState.value.gamePlaying)

        firstGame.value = false
        runCurrent()
        assertFalse(viewModel.uiState.value.settings.firstGame)
        assertTrue(viewModel.uiState.value.gamePlaying)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("00:01", viewModel.uiState.value.timeText)
    }

    @Test
    fun `changing mistake highlighting keeps older board and selected cell snapshots intact`() = runGameTest {
        val mistakeHighlight = MutableStateFlow(0)
        every { appSettings.highlightMistakes } returns mistakeHighlight
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(3))
        runCurrent()
        val unhighlighted = viewModel.uiState.value
        assertFalse(unhighlighted.gameBoard[0][0].error)
        assertFalse(unhighlighted.currCell.error)

        mistakeHighlight.value = 2
        runCurrent()
        val highlighted = viewModel.uiState.value
        assertTrue(highlighted.gameBoard[0][0].error)
        assertTrue(highlighted.currCell.error)
        assertEquals(0, highlighted.mistakesMade)
        assertFalse(unhighlighted.gameBoard[0][0].error)
        assertFalse(unhighlighted.currCell.error)

        mistakeHighlight.value = 0
        runCurrent()
        assertFalse(viewModel.uiState.value.gameBoard[0][0].error)
        assertFalse(viewModel.uiState.value.currCell.error)
        assertTrue(highlighted.gameBoard[0][0].error)
        assertTrue(highlighted.currCell.error)
    }

    @Test
    fun `mistake limit persists the losing move without a completion record`() = runGameTest {
        stubSettings(
            GameSettings(inputMethod = 0, remainingUse = false, mistakesLimit = true, mistakesMethod = 2)
        )
        val viewModel = loadedGame()
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        listOf(3, 4, 6).forEach { number -> viewModel.sendEvent(GameUiEvent.NumberTapped(number)) }
        runCurrent()

        assertEquals(3, viewModel.uiState.value.mistakesCount)
        assertEquals(6, viewModel.uiState.value.gameBoard[0][0].value)
        assertTrue(viewModel.uiState.value.endGame)
        assertTrue(viewModel.uiState.value.giveUp)
        assertFalse(viewModel.uiState.value.gamePlaying)
        assertEquals("60" + SOLUTION.drop(2), persistedGame?.currentBoard)
        assertEquals(3, persistedGame?.mistakes)
        assertEquals(true, persistedGame?.completed)
        assertEquals(true, persistedGame?.giveUp)
        assertEquals(false, persistedGame?.canContinue)
        assertEquals(1, savedGames.count { it.completed })
        coVerify(exactly = 0) { recordRepository.insert(any<Record>()) }
    }

    @Test
    fun `saved 6x6 game restores notes time and given cell locks`() = runGameTest {
        assertSavedGameRestored(GameType.Default6x6)
    }

    @Test
    fun `saved 12x12 game restores notes time and given cell locks`() = runGameTest {
        assertSavedGameRestored(GameType.Default12x12)
    }

    @Test
    fun `wrong value advanced hint clears the cell in cell first mode`() = runGameTest {
        assertWrongValueHintClearsCell(inputMethod = 0)
    }

    @Test
    fun `wrong value advanced hint clears the cell in digit first mode`() = runGameTest {
        assertWrongValueHintClearsCell(inputMethod = 1)
    }

    @Test
    fun `navigation waits for the latest move to be persisted`() = runGameTest {
        val viewModel = loadedGame()
        val effects = mutableListOf<GameUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        val releaseSave = CompletableDeferred<Unit>()
        coEvery { savedGameRepository.update(any()) } coAnswers {
            releaseSave.await()
            persistedGame = firstArg()
            savedGames += firstArg<SavedGame>()
        }
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        viewModel.sendEvent(GameUiEvent.NavigateBack)
        runCurrent()
        assertFalse(GameUiSideEffect.NavigateBack in effects)
        assertEquals("00" + SOLUTION.drop(2), persistedGame?.currentBoard)

        releaseSave.complete(Unit)
        runCurrent()
        assertEquals("50" + SOLUTION.drop(2), persistedGame?.currentBoard)
        assertEquals(1, effects.count { it == GameUiSideEffect.NavigateBack })
    }

    @Test
    fun `one failed save reports an effect and does not stop later saves`() = runGameTest {
        val viewModel = loadedGame()
        val effects = mutableListOf<GameUiSideEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        var failNextSave = true
        coEvery { savedGameRepository.update(any()) } answers {
            if (failNextSave) {
                failNextSave = false
                throw IllegalStateException("Storage unavailable")
            }
            persistedGame = firstArg()
            savedGames += firstArg<SavedGame>()
        }
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.NumberTapped(3))
        runCurrent()
        assertEquals(listOf(GameUiSideEffect.SaveFailed), effects)

        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        runCurrent()
        assertEquals("50" + SOLUTION.drop(2), persistedGame?.currentBoard)
        assertEquals(1, effects.count { it == GameUiSideEffect.SaveFailed })
    }

    private fun TestScope.assertSavedGameRestored(type: GameType) {
        val solution = buildString {
            repeat(type.size) { row ->
                repeat(type.size) { col ->
                    val value = (row * type.sectionWidth + row / type.sectionHeight + col) % type.size + 1
                    append(value.digitToChar(13))
                }
            }
        }
        val initial = "00" + solution.substring(2, solution.lastIndex) + "0"
        val current = "1" + initial.drop(1)
        val last = (type.size - 1).toString(13)
        val notes = "0,1,2;$last,$last,${type.size.toString(13)};"
        coEvery { boardRepository.get(BOARD_ID) } returns board().copy(
            type = type,
            initialBoard = initial,
            solvedBoard = solution
        )
        persistedGame = SavedGame(
            uid = BOARD_ID,
            currentBoard = current,
            notes = notes,
            timer = java.time.Duration.ofSeconds(75),
            mistakes = 1
        )
        val viewModel = createViewModel(playedBefore = true)
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()

        val restored = viewModel.uiState.value
        assertFalse(restored.isLoading)
        assertFalse(restored.loadError)
        assertEquals(type, restored.gameType)
        assertEquals(type.size, restored.size)
        assertEquals(type.size, restored.remainingUsesList.size)
        assertEquals(1, restored.gameBoard[0][0].value)
        assertFalse(restored.gameBoard[0][0].locked)
        assertEquals(0, restored.gameBoard[0][1].value)
        assertFalse(restored.gameBoard[0][1].locked)
        assertTrue(restored.gameBoard[0][2].locked)
        assertEquals(
            listOf(Note(0, 1, 2), Note(type.size - 1, type.size - 1, type.size)),
            restored.notes
        )
        assertEquals("01:15", restored.timeText)
        assertEquals(1, restored.mistakesCount)
        // Board serialization uses lowercase digits for 10..12, including uppercase input.
        assertEquals(current.lowercase(), persistedGame?.currentBoard)
        assertEquals(notes, persistedGame?.notes)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals("01:16", viewModel.uiState.value.timeText)
    }

    private suspend fun TestScope.assertWrongValueHintClearsCell(inputMethod: Int) {
        stubSettings(GameSettings(inputMethod = inputMethod, remainingUse = false, advancedHintEnabled = true))
        val viewModel = loadedGame()
        if (inputMethod == 0) {
            viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
            viewModel.sendEvent(GameUiEvent.NumberTapped(3))
        } else {
            viewModel.sendEvent(GameUiEvent.NumberTapped(3))
            viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        }
        runCurrent()
        val beforeHint = viewModel.uiState.value
        assertEquals(3, beforeHint.gameBoard[0][0].value)

        viewModel.sendEvent(GameUiEvent.RequestAdvancedHint)
        runCurrent()
        // Hint computation uses Default; this timeout measures real time, not the test clock.
        val hintState = withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                viewModel.uiState.first { !it.advancedHintLoading && it.advancedHintData != null }
            }
        }
        assertEquals(3, hintState.advancedHintData?.targetCell?.value)

        viewModel.sendEvent(GameUiEvent.ApplyAdvancedHint)
        runCurrent()
        assertEquals(0, viewModel.uiState.value.gameBoard[0][0].value)
        assertFalse(viewModel.uiState.value.advancedHintMode)
        assertEquals(3, beforeHint.gameBoard[0][0].value)
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Undo))
        runCurrent()
        assertEquals(3, viewModel.uiState.value.gameBoard[0][0].value)
    }

    private fun runGameTest(test: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try {
            test()
        } finally {
            // viewModelScope is independent of TestScope; stop its timer before runTest drains tasks.
            viewModels.forEach { it.viewModelScope.cancel() }
        }
    }

    private fun TestScope.loadedGame(): GameViewModel = createViewModel().also {
        it.sendEvent(GameUiEvent.ScreenResumed)
        runCurrent()
        assertFalse(it.uiState.value.isLoading)
        assertFalse(it.uiState.value.loadError)
        assertTrue(it.uiState.value.gamePlaying)
    }

    private fun createViewModel(playedBefore: Boolean = false) = GameViewModel(
        savedGameRepository = savedGameRepository,
        appSettingsManager = appSettings,
        recordRepository = recordRepository,
        updateBoardUseCase = UpdateBoardUseCase(boardRepository),
        getBoardUseCase = GetBoardUseCase(boardRepository),
        themeSettingsManager = themeSettings,
        savedStateHandle = SavedStateHandle(mapOf("gameUid" to BOARD_ID, "playedBefore" to playedBefore)),
        getAllRecordsUseCase = GetAllRecordsUseCase(recordRepository)
    ).also(viewModels::add)

    private fun stubSettings(settings: GameSettings) {
        every { appSettings.firstGame } returns flowOf(settings.firstGame)
        every { appSettings.fontSize } returns flowOf(settings.fontSize)
        every { appSettings.keepScreenOn } returns flowOf(settings.keepScreenOn)
        every { appSettings.remainingUse } returns flowOf(settings.remainingUse)
        every { appSettings.timerEnabled } returns flowOf(settings.timerEnabled)
        every { appSettings.highlightIdentical } returns flowOf(settings.identicalHighlight)
        every { appSettings.highlightMistakes } returns flowOf(settings.mistakesMethod)
        every { appSettings.positionLines } returns flowOf(settings.positionLines)
        every { themeSettings.boardCrossHighlight } returns flowOf(settings.crossHighlight)
        every { appSettings.funKeyboardOverNumbers } returns flowOf(settings.funKeyboardOverNum)
        every { appSettings.mistakesLimit } returns flowOf(settings.mistakesLimit)
        every { appSettings.autoEraseNotes } returns flowOf(settings.autoEraseNotes)
        every { appSettings.resetTimerEnabled } returns flowOf(settings.resetTimerOnRestart)
        every { appSettings.hintsDisabled } returns flowOf(settings.disableHints)
        every { appSettings.inputMethod } returns flowOf(settings.inputMethod)
        every { appSettings.advancedHintEnabled } returns flowOf(settings.advancedHintEnabled)
        every { appSettings.advancedHintSettings } returns flowOf(AdvancedHintSettings())
    }

    private fun board(emptyCells: Int = 2) = SudokuBoard(
        uid = BOARD_ID,
        initialBoard = "0".repeat(emptyCells) + SOLUTION.drop(emptyCells),
        solvedBoard = SOLUTION,
        difficulty = GameDifficulty.Easy,
        type = GameType.Default9x9
    )

    private companion object {
        const val BOARD_ID = 42L
        const val SOLUTION =
            "534678912672195348198342567859761423426853791713924856961537284287419635345286179"
    }
}
