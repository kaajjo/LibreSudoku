package com.kaajjo.libresudoku.ui.game.models

/**
 * One-time effects for the Game screen.
 * Platform actions are performed by the screen rather than the ViewModel.
 */
sealed interface GameUiSideEffect {

    /**
     * Navigates back from the Game screen.
     */
    data object NavigateBack : GameUiSideEffect

    /**
     * Opens the settings screen with the game-specific navigation context.
     */
    data object OpenSettings : GameUiSideEffect

    /**
     * Opens the advanced hint settings screen.
     */
    data object OpenHintSettings : GameUiSideEffect

    /**
     * Copies the exported board to the clipboard and shows confirmation where needed.
     *
     * @property board The serialized Sudoku board, with uppercase digits and dots for empty cells.
     */
    data class CopyBoard(val board: String) : GameUiSideEffect

    /**
     * Requests a short haptic response to an accepted game interaction.
     */
    data object HapticFeedback : GameUiSideEffect

    /**
     * Signals a completed restart so the screen can animate its restart button.
     */
    data object GameRestarted : GameUiSideEffect

    /**
     * Requests a localized notification that saving the game or its completion record failed.
     */
    data object SaveFailed : GameUiSideEffect
}
