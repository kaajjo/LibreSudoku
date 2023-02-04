package com.kaajjo.libresudoku.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true) val uid: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "date_created") val createdAt: ZonedDateTime
)
