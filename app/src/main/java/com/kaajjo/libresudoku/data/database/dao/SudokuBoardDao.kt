package com.kaajjo.libresudoku.data.database.dao

import androidx.room.*
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardDao {
    @Query("SELECT * FROM board")
    fun getAll(): Flow<List<SudokuBoard>>

    @Query("SELECT * FROM board WHERE difficulty == :gameDifficulty")
    fun getAll(gameDifficulty: GameDifficulty): Flow<List<SudokuBoard>>

    @Query("SELECT * FROM board")
    fun getAllList(): List<SudokuBoard>

    @Query(
        "SELECT * FROM board " +
                "LEFT OUTER JOIN saved_game ON board.uid = saved_game.board_uid " +
                "ORDER BY uid DESC"
    )
    fun getBoardsWithSavedGames(): Flow<Map<SudokuBoard, SavedGame?>>

    @Query(
        "SELECT * FROM board " +
                "LEFT OUTER JOIN saved_game ON board.uid = saved_game.board_uid " +
                "WHERE difficulty == :difficulty " +
                "ORDER BY uid DESC"
    )
    fun getBoardsWithSavedGames(difficulty: GameDifficulty): Flow<Map<SudokuBoard, SavedGame?>>


    @Query("SELECT * FROM board WHERE uid == :uid")
    fun get(uid: Long): SudokuBoard

    @Insert
    suspend fun insert(boardEntity: SudokuBoard): Long

    @Delete
    suspend fun delete(boardEntity: SudokuBoard)

    @Update
    suspend fun update(boardEntity: SudokuBoard)
}