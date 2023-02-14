package com.kaajjo.libresudoku.ui.util

class Route {
    companion object {
        const val HOME = "home"
        const val MORE = "more"
        const val ABOUT = "about"
        const val WELCOME_SCREEN = "welcome_screen"
        const val STATISTICS = "statistics"
        const val HISTORY = "history"
        const val LEARN = "learn"
        const val OPEN_SOURCE_LICENSES = "open_source_licenses"
        const val SETTINGS = "settings/?fromGame={fromGame}"
        const val GAME = "game/{uid}/{saved}"
        const val SAVED_GAME = "saved_game/{uid}"
        const val FOLDERS = "folders"
        const val SETTINGS_BOARD_THEME = "settings_board_theme"
    }
}