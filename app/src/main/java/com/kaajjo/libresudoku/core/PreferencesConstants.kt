package com.kaajjo.libresudoku.core

// Default values for preferences
class PreferencesConstants {
    companion object {
        const val MISTAKES_LIMIT = 3

        // Game settings
        const val DEFAULT_MISTAKES_LIMIT = false
        const val DEFAULT_HINTS_DISABLED = false
        const val DEFAULT_SHOW_TIMER = true
        const val DEFAULT_GAME_RESET_TIMER = true
        const val DEFAULT_HIGHLIGHT_MISTAKES = 1
        const val DEFAULT_HIGHLIGHT_IDENTICAL = true
        const val DEFAULT_REMAINING_USES = true
        const val DEFAULT_POSITION_LINES = true
        const val DEFAULT_AUTO_ERASE_NOTES = true
        const val DEFAULT_FONT_SIZE_FACTOR = 1
        const val DEFAULT_KEEP_SCREEN_ON = true
        const val DEFAULT_INPUT_METHOD = 1
        const val DEFAULT_FUN_KEYBOARD_OVER_NUM = false
        const val DEFAULT_SAVE_LAST_SELECTED_DIFF_TYPE = true

        // Theme settings
        const val GREEN_THEME_KEY = "green"
        const val PEACH_THEME_KEY = "pink"
        const val YELLOW_THEME_KEY = "yellow"
        const val LAVENDER_THEME_KEY = "lavender"
        const val BLACK_AND_WHITE_THEME_KEY = "black_and_white"
        const val BLUE_THEME_KEY = "blue"

        const val DEFAULT_DYNAMIC_COLORS = true
        const val DEFAULT_DARK_THEME = 0
        const val DEFAULT_AMOLED_BLACK = false
        const val DEFAULT_SELECTED_THEME = GREEN_THEME_KEY
        const val DEFAULT_MONET_SUDOKU_BOARD = true
        const val DEFAULT_BOARD_CROSS_HIGHLIGHT = false
    }
}