package com.kaajjo.libresudoku.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import com.kaajjo.libresudoku.data.backup.serializer.DurationLongSerializer
import com.kaajjo.libresudoku.data.backup.serializer.ZonedDateTimeLongSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.ZonedDateTime

@Serializable
@Entity(
    tableName = "saved_game",
    foreignKeys = [ForeignKey(
        onDelete = CASCADE,
        entity = SudokuBoard::class,
        parentColumns = arrayOf("uid"),
        childColumns = arrayOf("board_uid")
    )]
)
data class SavedGame(
    @PrimaryKey
    @ColumnInfo(name = "board_uid") val uid: Long,
    @ColumnInfo(name = "current_board") val currentBoard: String,
    @ColumnInfo(name = "notes") val notes: String,
    @Serializable(with = DurationLongSerializer::class)
    @ColumnInfo(name = "timer") val timer: Duration,
    @ColumnInfo(name = "completed", defaultValue = "false") val completed: Boolean = false,
    @ColumnInfo(name = "give_up", defaultValue = "false") val giveUp: Boolean = false,
    @ColumnInfo(name = "mistakes", defaultValue = "0") val mistakes: Int = 0,
    @ColumnInfo(name = "can_continue") val canContinue: Boolean = true,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "last_played") val lastPlayed: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "started_at") val startedAt: ZonedDateTime? = null,
    @Serializable(with = ZonedDateTimeLongSerializer::class)
    @ColumnInfo(name = "finished_at") val finishedAt: ZonedDateTime? = null,
)