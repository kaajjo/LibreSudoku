package com.kaajjo.libresudoku.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kaajjo.libresudoku.data.backup.serializer.ZonedDateTimeLongSerializer
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

@Serializable
@Entity
data class Folder(
    @PrimaryKey(autoGenerate = true) val uid: Long,
    @ColumnInfo(name = "name") val name: String,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "date_created") val createdAt: ZonedDateTime
)
