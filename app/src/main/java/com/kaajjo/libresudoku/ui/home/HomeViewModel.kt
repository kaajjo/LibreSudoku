package com.kaajjo.libresudoku.ui.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.components.generation.GenerationSession
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class HomeViewModel
@Inject constructor(
    private val appSettingsManager: AppSettingsManager,
    private val boardRepository: BoardRepository,
    private val savedGameRepository: SavedGameRepository
) : ViewModel() {

    val lastSavedGame = savedGameRepository.getLast()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val lastGamesLimit = 5
    val lastGames = savedGameRepository.getLastPlayable(limit = lastGamesLimit)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    var insertedBoardUid = -1L

    private val difficulties = listOf(
        GameDifficulty.Easy,
        GameDifficulty.Moderate,
        GameDifficulty.Hard,
        GameDifficulty.Challenge,
    )

    private val types = listOf(
        GameType.Default9x9,
        GameType.Default6x6,
        GameType.Default12x12,
        GameType.Killer9x9,
        GameType.Killer12x12,
        GameType.Killer6x6
    )

    val lastSelectedGameDifficultyType = appSettingsManager.lastSelectedGameDifficultyType
    val saveSelectedGameDifficultyType = appSettingsManager.saveSelectedGameDifficultyType

    var selectedDifficulty by mutableStateOf(difficulties.first())
    var selectedType by mutableStateOf(types.first())

    val generation = GenerationSession(viewModelScope, onError = { exception ->
        Log.e(TAG, "Unable to generate or save a Sudoku puzzle", exception)
    })

    fun startGame() {
        val type = selectedType
        val difficulty = selectedDifficulty
        val previousGame = lastSavedGame.value
        var storedUid: Long? = null
        generation.start(type, difficulty) { result ->
            check(result.solutionCount == 1) { "Generated puzzle is not unique" }
            // A save retry must not insert a second board if updating the previous game failed.
            if (storedUid == null) {
                val board = withContext(Dispatchers.Default) {
                    val parser = SudokuParser()
                    SudokuBoard(
                        uid = 0,
                        initialBoard = parser.boardToString(result.puzzle),
                        solvedBoard = parser.boardToString(result.solution),
                        difficulty = result.difficulty,
                        type = type,
                        killerCages = result.killerCages?.let(parser::killerSudokuCagesToString),
                        ratingMetadata = result.ratingMetadata
                    )
                }
                if (appSettingsManager.saveSelectedGameDifficultyType.first()) {
                    appSettingsManager.setLastSelectedGameDifficultyType(
                        difficulty = result.difficulty,
                        type = type
                    )
                }
                storedUid = withContext(Dispatchers.IO) { boardRepository.insert(board) }
            }
            if (previousGame != null && !previousGame.completed) {
                withContext(Dispatchers.IO) {
                    savedGameRepository.update(previousGame.copy(completed = true, canContinue = true))
                }
            }
            insertedBoardUid = checkNotNull(storedUid)
            selectedDifficulty = result.difficulty
        }
    }

    fun changeDifficulty(diff: Int) {
        val indexToSet = difficulties.indexOf(selectedDifficulty) + diff
        if (indexToSet >= 0 && indexToSet < difficulties.count()) {
            selectedDifficulty = difficulties[indexToSet]
        }
    }

    fun changeType(diff: Int) {
        val indexToSet = types.indexOf(selectedType) + diff
        if (indexToSet >= 0 && indexToSet < types.count()) {
            selectedType = types[indexToSet]
        }
    }

    private companion object {
        const val TAG = "HomeViewModel"
    }
}
