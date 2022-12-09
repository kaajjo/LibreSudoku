package com.kaajjo.libresudoku.data.database.dao

import androidx.room.*
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM board")
    fun getAll(): Flow<List<SudokuBoard>>

    @Query("SELECT * FROM board")
    fun getAllList(): List<SudokuBoard>

    @Query("SELECT * FROM board WHERE uid == :uid")
    fun get(uid: Long): SudokuBoard

    @Insert
    suspend fun insert(boardEntity: SudokuBoard) : Long

    @Delete
    suspend fun delete(boardEntity: SudokuBoard)

    @Update
    suspend fun update(boardEntity: SudokuBoard)
}