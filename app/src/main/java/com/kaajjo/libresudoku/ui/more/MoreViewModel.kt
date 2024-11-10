package com.kaajjo.libresudoku.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.core.update.Release
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {
    val updateChannel = appSettingsManager.autoUpdateChannel
    val updateDismissedName = appSettingsManager.updateDismissedName

    fun dismissUpdate(release: Release) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setUpdateDismissedName(release.name.toString())
        }
    }

}