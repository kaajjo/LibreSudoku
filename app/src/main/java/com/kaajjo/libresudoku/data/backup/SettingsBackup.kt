package com.kaajjo.libresudoku.data.backup

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

@Serializable
data class SettingsBackup(
    val inputMethod: Int = PreferencesConstants.DEFAULT_INPUT_METHOD,
    val mistakesLimit: Boolean = PreferencesConstants.DEFAULT_MISTAKES_LIMIT,
    val hintDisabled: Boolean = PreferencesConstants.DEFAULT_HINTS_DISABLED,
    val timer: Boolean = PreferencesConstants.DEFAULT_SHOW_TIMER,
    val resetTimer: Boolean = PreferencesConstants.DEFAULT_GAME_RESET_TIMER,
    val highlightMistakes: Int = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES,
    val highlightIdentical: Boolean = PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL,
    val remainingUses: Boolean = PreferencesConstants.DEFAULT_REMAINING_USES,
    val positionLines: Boolean = PreferencesConstants.DEFAULT_POSITION_LINES,
    val autoEraseNotes: Boolean = PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES,
    val fontSize: Int = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR,
    val keepScreenOn: Boolean = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON,
    val funKeyboardOverNum: Boolean = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM,
    val dateFormat: String = "",
    val saveSelectedGameDifficulty: Boolean = PreferencesConstants.DEFAULT_SAVE_LAST_SELECTED_DIFF_TYPE,
    val autoBackupInterval: Long = PreferencesConstants.DEFAULT_AUTOBACKUP_INTERVAL,
    val maxAutoBackups: Int = PreferencesConstants.DEFAULT_AUTO_BACKUPS_NUMBER,
    val dynamicColors: Boolean = PreferencesConstants.DEFAULT_DYNAMIC_COLORS,
    val darkTheme: Int = PreferencesConstants.DEFAULT_DARK_THEME,
    val monetSudokuBoard: Boolean = PreferencesConstants.DEFAULT_MONET_SUDOKU_BOARD,
    val boardCrossHighlight: Boolean = PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT,
    val currentThemeSeedColor: Int = PreferencesConstants.DEFAULT_THEME_SEED_COLOR,
    val paletteStyle: Int = PreferencesConstants.DEFAULT_PALETTE_STYLE,
    val isUserDefinedSeedColor: Boolean = false,
    val advancedHint: Boolean = PreferencesConstants.DEFAULT_ADVANCED_HINT
) {
    suspend fun setSettings(settings: AppSettingsManager, themeSettings: ThemeSettingsManager) {
        settings.setInputMethod(inputMethod)
        settings.setMistakesLimit(mistakesLimit)
        settings.setHintsDisabled(hintDisabled)
        settings.setTimer(timer)
        settings.setResetTimer(resetTimer)
        settings.setHighlightMistakes(highlightMistakes)
        settings.setSameValuesHighlight(highlightIdentical)
        settings.setRemainingUse(remainingUses)
        settings.setPositionLines(positionLines)
        settings.setAutoEraseNotes(autoEraseNotes)
        settings.setFontSize(fontSize)
        settings.setKeepScreenOn(keepScreenOn)
        settings.setFunKeyboardOverNum(funKeyboardOverNum)
        settings.setDateFormat(dateFormat)
        settings.setSaveSelectedGameDifficultyType(saveSelectedGameDifficulty)
        settings.setAutoBackupInterval(autoBackupInterval)
        settings.setAutoBackupsNumber(maxAutoBackups)
        settings.setAdvancedHint(advancedHint)

        themeSettings.setDynamicColors(dynamicColors)
        themeSettings.setDarkTheme(darkTheme)
        themeSettings.setMonetSudokuBoard(monetSudokuBoard)
        themeSettings.setBoardCrossHighlight(boardCrossHighlight)
        themeSettings.setCurrentThemeColor(Color(currentThemeSeedColor))
        themeSettings.setPaletteStyle(ThemeSettingsManager.getPaletteStyle(paletteStyle))
        themeSettings.setIsUserDefinedSeedColor(isUserDefinedSeedColor)
    }

    companion object {
        fun getSettings(
            settings: AppSettingsManager,
            themeSettings: ThemeSettingsManager
        ): SettingsBackup {
            return SettingsBackup(
                inputMethod = runBlocking { settings.inputMethod.first() },
                mistakesLimit = runBlocking { settings.mistakesLimit.first() },
                hintDisabled = runBlocking { settings.hintsDisabled.first() },
                timer = runBlocking { settings.timerEnabled.first() },
                resetTimer = runBlocking { settings.resetTimerEnabled.first() },
                highlightMistakes = runBlocking { settings.highlightMistakes.first() },
                highlightIdentical = runBlocking { settings.highlightIdentical.first() },
                remainingUses = runBlocking { settings.remainingUse.first() },
                positionLines = runBlocking { settings.positionLines.first() },
                autoEraseNotes = runBlocking { settings.autoEraseNotes.first() },
                fontSize = runBlocking { settings.fontSize.first() },
                keepScreenOn = runBlocking { settings.keepScreenOn.first() },
                funKeyboardOverNum = runBlocking { settings.funKeyboardOverNumbers.first() },
                dateFormat = runBlocking { settings.dateFormat.first() },
                saveSelectedGameDifficulty = runBlocking { settings.saveSelectedGameDifficultyType.first() },
                autoBackupInterval = runBlocking { settings.autoBackupInterval.first() },
                maxAutoBackups = runBlocking { settings.autoBackupsNumber.first() },
                dynamicColors = runBlocking { themeSettings.dynamicColors.first() },
                darkTheme = runBlocking { themeSettings.darkTheme.first() },
                monetSudokuBoard = runBlocking { themeSettings.monetSudokuBoard.first() },
                boardCrossHighlight = runBlocking { themeSettings.boardCrossHighlight.first() },
                currentThemeSeedColor = runBlocking { themeSettings.themeColorSeed.first().toArgb() },
                paletteStyle = runBlocking { ThemeSettingsManager.getPaletteIndex(themeSettings.themePaletteStyle.first()) },
                isUserDefinedSeedColor = runBlocking { themeSettings.isUserDefinedSeedColor.first() },
                advancedHint = runBlocking { settings.advancedHintEnabled.first() }
            )
        }
    }
}