package com.kaajjo.libresudoku.data.backup

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kaajjo.libresudoku.BuildConfig
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import com.kaajjo.libresudoku.domain.repository.FolderRepository
import com.kaajjo.libresudoku.domain.repository.RecordRepository
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val appSettingsManager: AppSettingsManager,
    private val boardRepository: BoardRepository,
    private val folderRepository: FolderRepository,
    private val recordRepository: RecordRepository,
    private val savedGameRepository: SavedGameRepository,
    private val themeSettingsManager: ThemeSettingsManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        var backupSuccessfull = false

        try {
            val backupUri = runBlocking {appSettingsManager.backupUri.first() }

            val boards = runBlocking { boardRepository.getAll().first() }

            if (backupUri.isEmpty()) {
                Log.i(
                    WORK_NAME_AUTO_BACKUP,
                    "Automatic backup skipped: URI is empty"
                )
                return Result.failure()
            } else if (boards.isEmpty()) {
                Log.i(
                    WORK_NAME_AUTO_BACKUP,
                    "Automatic backup skipped: Nothing to backup"
                )
                return Result.failure()
            }

            if(!context.contentResolver.persistedUriPermissions.any { it.uri == backupUri.toUri()}) {
                Log.i(
                    WORK_NAME_AUTO_BACKUP,
                    "Automatic backup skipped: not persisted URI"
                )
                return Result.failure()
            }

            val folders = runBlocking { folderRepository.getAll().first() }
            val records = runBlocking { recordRepository.getAll().first() }
            val savedGames = runBlocking { savedGameRepository.getAll().first() }

            val documentFile = DocumentFile.fromTreeUri(context, backupUri.toUri())
            if (documentFile != null) {
                val backupData = BackupData(
                    appVersionName = BuildConfig.VERSION_NAME,
                    appVersionCode = BuildConfig.VERSION_CODE,
                    createdAt = ZonedDateTime.now(),
                    boards = boards,
                    folders = folders,
                    records = records,
                    savedGames = savedGames,
                    settings = SettingsBackup.getSettings(appSettingsManager, themeSettingsManager)
                )

                val json = Json {
                    encodeDefaults = true
                }
                val backupJson = json.encodeToString(backupData)

                val file = documentFile.createFile(
                    "application/json",
                    BackupData.nameAuto
                )

                if (file != null) {
                    context.contentResolver.openOutputStream(file.uri).use { outputStream ->
                        outputStream?.write(backupJson.toByteArray())
                        outputStream?.close()
                    }
                    backupSuccessfull = true

                    appSettingsManager.setLastBackupDate(ZonedDateTime.now())
                }
                val autoBackupsNumber = runBlocking { appSettingsManager.autoBackupsNumber.first() }

                documentFile.listFiles()
                    .filter { BackupData.regexAuto.matches(it.name ?: "") }
                    .sortedByDescending { it.name }
                    .drop(autoBackupsNumber)
                    .forEach { it.delete() }
            }
        } catch (e: Exception) {
            Log.e(WORK_NAME_AUTO_BACKUP, e.message.toString())
            return Result.failure()
        }
        Log.i(
            WORK_NAME_AUTO_BACKUP,
            "Automatic backup created. T=${
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            }"
        )
        return if (backupSuccessfull) Result.success() else Result.failure()
    }

    companion object {
        fun setupWorker(context: Context, intervalHours: Long) {
            if (intervalHours < 1) {
                cancelWorker(context)
                return
            }

            val periodicWorkRequest = PeriodicWorkRequest.Builder(
                BackupWorker::class.java,
                Duration.ofHours(intervalHours)
            ).build()

            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME_AUTO_BACKUP,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                periodicWorkRequest
            )
        }

        fun cancelWorker(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(WORK_NAME_AUTO_BACKUP)
        }
    }
}

private const val WORK_NAME_AUTO_BACKUP = "AutomaticBackupWorker"