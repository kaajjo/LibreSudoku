package com.kaajjo.libresudoku.ui.settings.advanced_hint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHintSettings
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsAdvancedHintViewModel @Inject constructor(
    private val settingsManager: AppSettingsManager
) : ViewModel() {
    val advancedHintEnabled = settingsManager.advancedHintEnabled
    val advancedHintSettings = settingsManager.advancedHintSettings

    fun setAdvancedHintEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.setAdvancedHint(enabled)
        }
    }

    fun updateAdvancedHintSettings(settings: AdvancedHintSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateAdvancedHintSettings(settings)
        }
    }
}
