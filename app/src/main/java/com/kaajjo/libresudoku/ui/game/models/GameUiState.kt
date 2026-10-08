package com.kaajjo.libresudoku.ui.game.models

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHintData
import com.kaajjo.libresudoku.data.database.model.Record

/**
 * UI model for the Game screen. Changes are requested through [GameUiEvent].
 *
 * @property isLoading Whether the board, solution and saved progress are being loaded.
 * @property loadError Whether the latest loading attempt failed and can be retried.
 * @property settings Current preferences used to render and play the game.
 * @property gameType The loaded Sudoku variant, or [GameType.Unspecified] before loading.
 * @property gameDifficulty The loaded board difficulty, or [GameDifficulty.Unspecified] before loading.
 * @property gameBoard The player's current board, including locked clues and mistake flags.
 * @property solvedBoard The solution used for hints, mistake checking and the solution preview.
 * @property cages Killer Sudoku cages, or an empty list when the board has no cages.
 * @property notes Candidate notes currently present on the board.
 * @property currCell The selected cell; row and column are -1 when no board cell is selected.
 * @property remainingUsesList Remaining occurrences for each digit, indexed by digit minus one.
 * @property gamePlaying Whether the timer is running and game input is accepted.
 * @property endGame Whether the game has ended through completion, giving up or the mistake limit.
 * @property giveUp Whether the game ended by giving up or reaching the mistake limit.
 * @property timeText Formatted elapsed game time, including penalties from regular hints.
 * @property mistakesCount Persisted number of mistakes counted toward the enabled mistake limit.
 * @property hintsUsed Regular hints used in the current session; advanced hints do not increase it.
 * @property mistakesMade Entries flagged as mistakes in the current session, even when the mistake limit is disabled.
 * @property notesTaken Notes added manually in the current session; computed notes do not increase it.
 * @property digitFirstNumber The selected digit for digit-first input; 0 means none, and -1 clears
 * number selection when erase mode is toggled.
 * @property notesToggled Whether number input adds or removes notes instead of entering a value.
 * @property eraseButtonToggled Whether tapping an editable cell erases its contents.
 * @property showSolution Whether to display [solvedBoard] instead of the player's board.
 * @property renderNotes Whether notes are visible on the player's board.
 * @property restartDialog Whether the restart confirmation dialog is visible.
 * @property giveUpDialog Whether the give-up confirmation dialog is visible.
 * @property showMenu Whether the game actions menu is expanded.
 * @property showNotesMenu Whether the notes actions menu is expanded.
 * @property showUndoRedoMenu Whether the undo/redo menu is expanded.
 * @property advancedHintMode Whether the advanced hint panel replaces the normal game controls.
 * @property advancedHintLoading Whether an advanced hint is currently being calculated.
 * @property advancedHintData The calculated hint, or null before calculation, after cancellation,
 * or when no hint is available; [advancedHintLoading] distinguishes an unfinished calculation.
 * @property allRecords Completion records matching the loaded game type and difficulty.
 * @property size The number of rows and columns in [gameBoard].
 */
data class GameUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val settings: GameSettings = GameSettings(),
    val gameType: GameType = GameType.Unspecified,
    val gameDifficulty: GameDifficulty = GameDifficulty.Unspecified,
    val gameBoard: List<List<Cell>> = List(9) { row -> List(9) { col -> Cell(row, col) } },
    val solvedBoard: List<List<Cell>> = emptyList(),
    val cages: List<Cage> = emptyList(),
    val notes: List<Note> = emptyList(),
    val currCell: Cell = Cell(-1, -1),
    val remainingUsesList: List<Int> = emptyList(),
    val gamePlaying: Boolean = false,
    val endGame: Boolean = false,
    val giveUp: Boolean = false,
    val timeText: String = "00:00",
    val mistakesCount: Int = 0,
    val hintsUsed: Int = 0,
    val mistakesMade: Int = 0,
    val notesTaken: Int = 0,
    val digitFirstNumber: Int = 0,
    val notesToggled: Boolean = false,
    val eraseButtonToggled: Boolean = false,
    val showSolution: Boolean = false,
    val renderNotes: Boolean = true,
    val restartDialog: Boolean = false,
    val giveUpDialog: Boolean = false,
    val showMenu: Boolean = false,
    val showNotesMenu: Boolean = false,
    val showUndoRedoMenu: Boolean = false,
    val advancedHintMode: Boolean = false,
    val advancedHintLoading: Boolean = false,
    val advancedHintData: AdvancedHintData? = null,
    val allRecords: List<Record> = emptyList()
) {
    val size: Int get() = gameBoard.size
}

/**
 * Preference values used by the Game screen and its input rules.
 *
 * @property firstGame Whether to show the introductory game dialog and keep gameplay paused.
 * @property fontSize Board text size preference: 0 for automatic, 1 for small, 2 for medium,
 * and 3 for large.
 * @property keepScreenOn Whether the screen should stay on while the Game screen is displayed.
 * @property remainingUse Whether to show remaining digit counts and hide exhausted keyboard digits.
 * @property timerEnabled Whether to display the timer; elapsed game time is tracked independently.
 * @property identicalHighlight Whether to highlight cells matching the selected value.
 * @property mistakesMethod Mistake checking mode: 0 for off, 1 for rule violations, and 2 for
 * comparison with the solution.
 * @property positionLines Whether to highlight the selected cell's row and column.
 * @property crossHighlight Whether to highlight the board's alternating boxes where supported.
 * @property funKeyboardOverNum Whether to place the function toolbar above the number keyboard.
 * @property mistakesLimit Whether counted mistakes end the game at [PreferencesConstants.MISTAKES_LIMIT].
 * @property autoEraseNotes Whether entering a value removes conflicting notes from related cells.
 * @property resetTimerOnRestart Whether restarting a game resets its elapsed time.
 * @property disableHints Whether regular hints are disabled.
 * @property inputMethod Preferred input order: 0 for cell-first and 1 for digit-first.
 * @property advancedHintEnabled Whether the advanced hint action is available.
 */
data class GameSettings(
    val firstGame: Boolean = false,
    val fontSize: Int = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR,
    val keepScreenOn: Boolean = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON,
    val remainingUse: Boolean = PreferencesConstants.DEFAULT_REMAINING_USES,
    val timerEnabled: Boolean = PreferencesConstants.DEFAULT_SHOW_TIMER,
    val identicalHighlight: Boolean = PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL,
    val mistakesMethod: Int = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES,
    val positionLines: Boolean = PreferencesConstants.DEFAULT_POSITION_LINES,
    val crossHighlight: Boolean = PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT,
    val funKeyboardOverNum: Boolean = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM,
    val mistakesLimit: Boolean = PreferencesConstants.DEFAULT_MISTAKES_LIMIT,
    val autoEraseNotes: Boolean = PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES,
    val resetTimerOnRestart: Boolean = PreferencesConstants.DEFAULT_GAME_RESET_TIMER,
    val disableHints: Boolean = PreferencesConstants.DEFAULT_HINTS_DISABLED,
    val inputMethod: Int = PreferencesConstants.DEFAULT_INPUT_METHOD,
    val advancedHintEnabled: Boolean = PreferencesConstants.DEFAULT_ADVANCED_HINT
)
