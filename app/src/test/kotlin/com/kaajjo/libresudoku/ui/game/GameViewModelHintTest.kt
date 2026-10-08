package com.kaajjo.libresudoku.ui.game

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
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
import com.kaajjo.libresudoku.ui.game.models.GameUiEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for issues #162, #176, #183: using a hint crashed the game with
 * IndexOutOfBoundsException (index -1) when the hinted value hit the mistakes limit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GameViewModelHintTest {

    // Keep initialization and events on the same dispatcher as the ViewModel's state updates.
    private val mainDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        val context = ApplicationProvider.getApplicationContext<Application>()

        val appSettingsManager = AppSettingsManager(context)
        runBlocking {
            appSettingsManager.setFirstGame(false)
            appSettingsManager.setInputMethod(0)
            appSettingsManager.setRemainingUse(false)
            appSettingsManager.setMistakesLimit(true)
            // check for rules violations
            appSettingsManager.setHighlightMistakes(1)
        }

        val boardRepository = FakeBoardRepository(
            SudokuBoard(
                uid = BOARD_UID,
                initialBoard = "00" + SOLVED_BOARD.drop(2),
                solvedBoard = SOLVED_BOARD,
                difficulty = GameDifficulty.Easy,
                type = GameType.Default9x9
            )
        )
        val recordRepository = FakeRecordRepository()
        viewModel = GameViewModel(
            savedGameRepository = FakeSavedGameRepository(),
            appSettingsManager = appSettingsManager,
            recordRepository = recordRepository,
            updateBoardUseCase = UpdateBoardUseCase(boardRepository),
            getBoardUseCase = GetBoardUseCase(boardRepository),
            themeSettingsManager = ThemeSettingsManager(context),
            savedStateHandle = SavedStateHandle(
                mapOf("gameUid" to BOARD_UID, "playedBefore" to false)
            ),
            getAllRecordsUseCase = GetAllRecordsUseCase(recordRepository)
        )

        waitUntil {
            val state = viewModel.uiState.value
            !state.isLoading && !state.loadError && state.remainingUsesList.isNotEmpty() &&
                    state.settings.mistakesMethod == 1 && state.settings.mistakesLimit
        }
        viewModel.sendEvent(GameUiEvent.ScreenResumed)
        mainDispatcher.scheduler.runCurrent()
    }

    @After
    fun tearDown() {
        if (::viewModel.isInitialized) {
            viewModel.viewModelScope.cancel()
            mainDispatcher.scheduler.runCurrent()
        }
        Dispatchers.resetMain()
    }

    @Test
    fun hintConflictingWithWrongUserInput_onLastAllowedMistake_doesNotCrashOrEndGame() {
        // Two wrong entries reach the last allowed mistake; 5 belongs to (0, 0).
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 1))
        viewModel.sendEvent(GameUiEvent.NumberTapped(4))
        viewModel.sendEvent(GameUiEvent.NumberTapped(5))
        assertEquals(PreferencesConstants.MISTAKES_LIMIT - 1, viewModel.uiState.value.mistakesCount)

        // the correct value 5 at (0, 0) now violates the row rule because of the user's digit
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 0))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Hint))

        val state = viewModel.uiState.value
        val hintedCell = state.gameBoard[0][0]
        assertEquals(5, hintedCell.value)
        assertFalse(hintedCell.error)
        assertEquals(PreferencesConstants.MISTAKES_LIMIT - 1, state.mistakesCount)
        assertFalse(state.endGame)
        assertFalse(state.giveUp)
        assertEquals(1, state.hintsUsed)
    }

    @Test
    fun hintOnEmptyCell_setsSolutionValueWithoutMistake() {
        viewModel.sendEvent(GameUiEvent.CellTapped(0, 1))
        viewModel.sendEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Hint))

        val state = viewModel.uiState.value
        assertEquals(3, state.gameBoard[0][1].value)
        assertFalse(state.gameBoard[0][1].error)
        assertEquals(0, state.mistakesCount)
        assertEquals(1, state.hintsUsed)
    }

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            mainDispatcher.scheduler.runCurrent()
            if (condition()) return
            check(System.currentTimeMillis() < deadline) { "Timed out waiting for GameViewModel" }
            Thread.sleep(10)
        }
    }

    private class FakeBoardRepository(private var board: SudokuBoard) : BoardRepository {
        override fun getAll(): Flow<List<SudokuBoard>> = flowOf(listOf(board))
        override fun getAll(difficulty: GameDifficulty): Flow<List<SudokuBoard>> = getAll()
        override fun getAllInFolder(folderUid: Long): Flow<List<SudokuBoard>> = flowOf(emptyList())
        override fun getAllInFolderList(folderUid: Long): List<SudokuBoard> = emptyList()
        override fun getWithSavedGames(): Flow<Map<SudokuBoard, SavedGame?>> = flowOf(emptyMap())
        override fun getWithSavedGames(difficulty: GameDifficulty): Flow<Map<SudokuBoard, SavedGame?>> =
            flowOf(emptyMap())
        override fun getInFolderWithSaved(folderUid: Long): Flow<Map<SudokuBoard, SavedGame?>> =
            flowOf(emptyMap())
        override fun getBoardsInFolderFlow(uid: Long): Flow<List<SudokuBoard>> = flowOf(emptyList())
        override fun getBoardsInFolder(uid: Long): List<SudokuBoard> = emptyList()
        override suspend fun get(uid: Long): SudokuBoard = board
        override suspend fun insert(boards: List<SudokuBoard>): List<Long> = boards.map { it.uid }
        override suspend fun insert(board: SudokuBoard): Long = board.uid
        override suspend fun delete(board: SudokuBoard) = Unit
        override suspend fun delete(boards: List<SudokuBoard>) = Unit
        override suspend fun update(board: SudokuBoard) {
            this.board = board
        }
        override suspend fun update(boards: List<SudokuBoard>) = Unit
    }

    private class FakeSavedGameRepository : SavedGameRepository {
        @Volatile
        private var savedGame: SavedGame? = null

        override fun getAll(): Flow<List<SavedGame>> = flowOf(listOfNotNull(savedGame))
        override suspend fun get(uid: Long): SavedGame? = savedGame
        override fun getWithBoards(): Flow<Map<SavedGame, SudokuBoard>> = flowOf(emptyMap())
        override fun getLast(): Flow<SavedGame?> = flowOf(savedGame)
        override fun getLastPlayable(limit: Int): Flow<Map<SavedGame, SudokuBoard>> = flowOf(emptyMap())
        override suspend fun insert(savedGame: SavedGame): Long {
            this.savedGame = savedGame
            return savedGame.uid
        }
        override suspend fun insert(savedGames: List<SavedGame>) = Unit
        override suspend fun update(savedGame: SavedGame) {
            this.savedGame = savedGame
        }
        override suspend fun delete(savedGame: SavedGame) {
            this.savedGame = null
        }
    }

    private class FakeRecordRepository : RecordRepository {
        override suspend fun get(uid: Long): Record = error("Not used")
        override fun getAll(): Flow<List<Record>> = flowOf(emptyList())
        override fun getAllSortByTime(): Flow<List<Record>> = flowOf(emptyList())
        override fun getAll(difficulty: GameDifficulty, type: GameType): Flow<List<Record>> =
            flowOf(emptyList())
        override suspend fun insert(record: Record) = Unit
        override suspend fun insert(records: List<Record>) = Unit
        override suspend fun delete(record: Record) = Unit
    }

    private companion object {
        const val BOARD_UID = 1L
        const val SOLVED_BOARD =
            "534678912" +
            "672195348" +
            "198342567" +
            "859761423" +
            "426853791" +
            "713924856" +
            "961537284" +
            "287419635" +
            "345286179"
    }
}
