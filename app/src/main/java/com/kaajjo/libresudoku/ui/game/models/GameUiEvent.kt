package com.kaajjo.libresudoku.ui.game.models

import com.kaajjo.libresudoku.ui.game.components.ToolBarItem

/**
 * Events for [com.kaajjo.libresudoku.ui.game.GameScreen]
 */
sealed interface GameUiEvent {

    /**
     * Marks the screen as resumed and attempts to resume a game that was not manually paused
     */
    data object ScreenResumed : GameUiEvent

    /**
     * Pauses the timer, cancels advanced hints, clears the selection and queues a save
     */
    data object ScreenPaused : GameUiEvent

    /**
     * Toggles the user's play/pause choice and clears the selected cell
     */
    data object TogglePause : GameUiEvent

    /**
     * Marks the introductory game dialog as completed in the app preferences
     */
    data object FirstGameFinished : GameUiEvent

    /**
     * Selects a board cell or applies the active input mode to it
     * A short tap on a paused board requests resumption without entering a value
     *
     * @property row Zero-based row of the tapped cell
     * @property col Zero-based column of the tapped cell
     * @property longTap Whether the gesture was a long press. In digit-first mode it toggles a note
     */
    data class CellTapped(val row: Int, val col: Int, val longTap: Boolean = false) : GameUiEvent

    /**
     * Enters a number or selects it for digit-first input according to the current input mode
     *
     * @property number The keyboard digit, from 1 through the board size
     * @property longTap Whether the gesture was a long press. In cell-first mode it temporarily
     * selects digit-first input.
     */
    data class NumberTapped(val number: Int, val longTap: Boolean = false) : GameUiEvent

    /**
     * Performs a toolbar action while the game is running.
     *
     * @property item The undo, redo, regular hint, note mode or remove action to perform
     */
    data class ToolbarClicked(val item: ToolBarItem) : GameUiEvent

    /**
     * Toggles erase-on-tap mode and clears note mode and the current selection
     */
    data object ToggleEraseMode : GameUiEvent

    /**
     * Opens the advanced hint panel and starts calculating a hint for the current board
     */
    data object RequestAdvancedHint : GameUiEvent

    /**
     * Cancels any pending hint calculation and closes the advanced hint panel
     */
    data object CancelAdvancedHint : GameUiEvent

    /**
     * Applies the current advanced hint and closes its panel
     * A wrong-value hint clears the indicated value instead of entering a new one
     */
    data object ApplyAdvancedHint : GameUiEvent

    /**
     * Switches between the player's board and the solution after the game has ended
     */
    data object ToggleSolution : GameUiEvent

    /**
     * Pauses the game, cancels advanced hints and opens the restart confirmation dialog
     */
    data object ShowRestartDialog : GameUiEvent

    /**
     * Closes the restart dialog and resumes the game when the user's play/pause choice allows it
     */
    data object DismissRestartDialog : GameUiEvent

    /**
     * Restores the initial board, resets move history and session statistics, and resumes play
     * The elapsed time is reset only when the restart preference requests it
     */
    data object ConfirmRestart : GameUiEvent

    /**
     * Pauses the game, cancels advanced hints and opens the give-up confirmation dialog
     */
    data object ShowGiveUpDialog : GameUiEvent

    /**
     * Closes the give-up dialog and resumes the game when the user's play/pause choice allows it
     */
    data object DismissGiveUpDialog : GameUiEvent

    /**
     * Ends the game as given up and queues its final state for saving
     */
    data object ConfirmGiveUp : GameUiEvent

    /**
     * Changes the visibility of the game actions menu
     *
     * @property visible Whether the menu should be expanded.
     */
    data class SetMenuVisible(val visible: Boolean) : GameUiEvent

    /**
     * Changes the visibility of the notes menu; opening it requires a running game
     *
     * @property visible Whether the menu should be expanded
     */
    data class SetNotesMenuVisible(val visible: Boolean) : GameUiEvent

    /**
     * Changes the visibility of the undo/redo menu; opening it requires a running game
     *
     * @property visible Whether the menu should be expanded
     */
    data class SetUndoRedoMenuVisible(val visible: Boolean) : GameUiEvent

    /**
     * Shows or hides notes without changing the notes stored on the board.
     */
    data object ToggleRenderNotes : GameUiEvent

    /**
     * Replaces existing notes with the candidates computed for the current board
     */
    data object ComputeNotes : GameUiEvent

    /**
     * Removes all notes from the board and records the change in move history
     */
    data object ClearNotes : GameUiEvent

    /**
     * Pauses the game and requests navigation back after the pending save attempt finishes
     */
    data object NavigateBack : GameUiEvent

    /**
     * Pauses the game and requests the game settings screen after the pending save attempt finishes
     */
    data object OpenSettings : GameUiEvent

    /**
     * Pauses the game and requests advanced hint settings after the pending save attempt finishes
     */
    data object OpenHintSettings : GameUiEvent

    /**
     * Requests copying the player's current board as a Sudoku string
     */
    data object ExportBoard : GameUiEvent

    /**
     * Retries loading the game after a previous loading failure
     */
    data object RetryLoading : GameUiEvent
}
