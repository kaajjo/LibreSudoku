package com.kaajjo.libresudoku.ui.settings.autoupdate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoUpdateViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {
    val updateChannel = appSettingsManager.autoUpdateChannel

    fun updateAutoUpdateChannel(channel: UpdateChannel) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setAutoUpdateChannel(channel)
        }
    }
}