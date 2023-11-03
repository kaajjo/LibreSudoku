package com.kaajjo.libresudoku.data.backup

import com.kaajjo.libresudoku.data.backup.serializer.ZonedDateTimeLongSerializer
import com.kaajjo.libresudoku.data.database.model.Folder
import com.kaajjo.libresudoku.data.database.model.Record
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

const val BACKUP_SCHEME_VERSION = 1

@Serializable
data class BackupData(
    val appVersionName: String,
    val appVersionCode: Int,
    val backupSchemeVersion: Int = BACKUP_SCHEME_VERSION,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    val createdAt: ZonedDateTime,
    val boards: List<SudokuBoard>,
    val records: List<Record> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val savedGames: List<SavedGame>,
    val settings: SettingsBackup? = null
) {
    companion object {
        /**
         * Regex for auto backups
         */
        val regexAuto = """LibreSudoku-AutoBackup-\d+-\d+-\d+--\d+-\d+-\d+.json""".toRegex()

        /**
         * Filename for manual backups
         */
        val nameManual: String
            get() = "LibreSudoku-Backup-${
                ZonedDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss"))

            }"

        /**
         * Filename for auto backups
         */
        val nameAuto: String
            get() = "LibreSudoku-AutoBackup-${
            ZonedDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss"))
        }"
    }
}