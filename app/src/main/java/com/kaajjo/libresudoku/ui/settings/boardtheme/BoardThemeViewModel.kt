package com.kaajjo.libresudoku.ui.settings.boardtheme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SettingsBoardThemeViewModel @Inject constructor(
    private val themeSettingsManager: ThemeSettingsManager
) : ViewModel() {
    val monetSudokuBoard = themeSettingsManager.monetSudokuBoard

    fun updateMonetSudokuBoardSetting(enabled: Boolean) {
        viewModelScope.launch {
            themeSettingsManager.setMonetSudokuBoard(enabled)
        }
    }
}