package com.kaajjo.libresudoku.ui.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.BuildConfig
import com.kaajjo.libresudoku.data.backup.BackupData
import com.kaajjo.libresudoku.data.backup.SettingsBackup
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.DatabaseRepository
import com.kaajjo.libresudoku.domain.repository.FolderRepository
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.time.ZonedDateTime
import javax.inject.Inject


@HiltViewModel
class BackupScreenViewModel @Inject constructor(
    private val appSettingsManager: AppSettingsManager,
    private val themeSettingsManager: ThemeSettingsManager,
    private val boardRepository: BoardRepository,
    private val folderRepository: FolderRepository,
    private val recordRepository: RecordRepository,
    private val savedGameRepository: SavedGameRepository,
    private val databaseRepository: DatabaseRepository
) : ViewModel() {
    val backupUri = appSettingsManager.backupUri.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        initialValue = ""
    )

    var backupData by mutableStateOf<BackupData?>(null)
    private var backupJson: String? = null
    var restoreError by mutableStateOf(false)
    var restoreExceptionString by mutableStateOf("")

    val autoBackupsNumber = appSettingsManager.autoBackupsNumber
    val autoBackupInterval = appSettingsManager.autoBackupInterval
    val lastBackupDate = appSettingsManager.lastBackupDate
    val dateFormat = appSettingsManager.dateFormat

    fun createBackup(
        backupSettings: Boolean,
        onCreated: (Boolean) -> Unit
    ) {
        try {
            val boards = runBlocking { boardRepository.getAll().first() }
            val folders = runBlocking { folderRepository.getAll().first() }
            val records = runBlocking { recordRepository.getAll().first() }
            val savedGames = runBlocking { savedGameRepository.getAll().first() }

            backupData = BackupData(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                createdAt = ZonedDateTime.now(),
                boards = boards,
                folders = folders,
                records = records,
                savedGames = savedGames,
                settings = if (backupSettings) SettingsBackup.getSettings(
                    appSettingsManager,
                    themeSettingsManager
                ) else null
            )

            val json = Json {
                encodeDefaults = true
                ignoreUnknownKeys = true
            }
            backupJson = json.encodeToString(backupData)
            onCreated(true)
        } catch (e: Exception) {
            onCreated(false)
        }
    }

    fun setBackupDirectory(uri: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setBackupUri(uri)
        }
    }

    fun prepareBackupToRestore(
        backupString: String,
        onComplete: () -> Unit
    ) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            backupData = json.decodeFromString<BackupData?>(backupString)
            onComplete()
        } catch (e: Exception) {
            restoreError = true
            restoreExceptionString = e.message.toString()
        }
    }

    fun saveBackupTo(
        outputStream: OutputStream?,
        onComplete: (Throwable?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            backupJson?.let { backup ->
                try {
                    outputStream?.use {
                        it.write(backup.toByteArray())
                        it.close()
                    }
                    onComplete(null)
                    viewModelScope.launch(Dispatchers.IO) {
                        appSettingsManager.setLastBackupDate(ZonedDateTime.now())
                    }
                } catch (e: Exception) {
                    onComplete(e)
                }
            }
        }
    }

    fun restoreBackup(
        onComplete: () -> Unit
    ) {
        backupData?.let { backup ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    // deleting all data from database
                    runBlocking { databaseRepository.resetDb() }

                    if (backup.boards.isNotEmpty()) {
                        folderRepository.insert(backup.folders)
                        boardRepository.insert(backup.boards)
                        savedGameRepository.insert(backup.savedGames)
                        recordRepository.insert(backup.records)
                    }

                    backup.settings?.setSettings(appSettingsManager, themeSettingsManager)
                    onComplete()
                } catch (e: Exception) {
                    restoreError = true
                    restoreExceptionString = e.message.toString()
                }
            }
        }
    }

    fun setAutoBackupsNumber(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setAutoBackupsNumber(value)
        }
    }

    fun setAutoBackupInterval(hours: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            appSettingsManager.setAutoBackupInterval(hours)
        }
    }
}