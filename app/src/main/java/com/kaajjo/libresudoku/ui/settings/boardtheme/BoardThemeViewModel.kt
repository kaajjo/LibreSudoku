package com.kaajjo.libresudoku.ui.settings.boardtheme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsBoardThemeViewModel @Inject constructor(
    private val themeSettingsManager: ThemeSettingsManager,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {
    val monetSudokuBoard = themeSettingsManager.monetSudokuBoard
    val positionLines = appSettingsManager.positionLines
    val highlightMistakes = appSettingsManager.highlightMistakes
    var crossHighlight = themeSettingsManager.boardCrossHighlight

    fun updateMonetSudokuBoardSetting(enabled: Boolean) {
        viewModelScope.launch {
            themeSettingsManager.setMonetSudokuBoard(enabled)
        }
    }

    fun updatePositionLinesSetting(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsManager.setPositionLines(enabled)
        }
    }

    fun updateBoardCrossHighlight(enabled: Boolean) {
        viewModelScope.launch {
            themeSettingsManager.setBoardCrossHighlight(enabled)
        }
    }
}