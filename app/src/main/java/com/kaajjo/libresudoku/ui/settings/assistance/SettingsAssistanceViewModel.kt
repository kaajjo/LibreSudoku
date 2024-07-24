package com.kaajjo.libresudoku.ui.settings.assistance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsAssistanceViewModel @Inject constructor(
    private val settings: AppSettingsManager,
) : ViewModel() {
    val remainingUse = settings.remainingUse
    fun updateRemainingUse(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setRemainingUse(enabled)
        }
    }

    val highlightIdentical = settings.highlightIdentical
    fun updateHighlightIdentical(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settings.setSameValuesHighlight(enabled)
        }

    val autoEraseNotes = settings.autoEraseNotes
    fun updateAutoEraseNotes(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setAutoEraseNotes(enabled)
        }
    }

    val highlightMistakes = settings.highlightMistakes
    fun updateMistakesHighlight(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settings.setHighlightMistakes(index)
        }
    }
}