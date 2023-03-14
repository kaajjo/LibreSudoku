package com.kaajjo.libresudoku.data.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.ZonedDateTime

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
    @ColumnInfo(name = "timer") val timer: Duration,
    @ColumnInfo(name = "completed", defaultValue = "false") val completed: Boolean = false,
    @ColumnInfo(name = "give_up", defaultValue = "false") val giveUp: Boolean = false,
    @ColumnInfo(name = "mistakes", defaultValue = "0") val mistakes: Int = 0,
    @ColumnInfo(name = "can_continue") val canContinue: Boolean = true,
    @ColumnInfo(name = "last_played") val lastPlayed: ZonedDateTime? = null,
    @ColumnInfo(name = "started_at") val startedAt: ZonedDateTime? = null,
    @ColumnInfo(name = "finished_at") val finishedAt: ZonedDateTime? = null,
)