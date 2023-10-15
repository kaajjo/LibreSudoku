package com.kaajjo.libresudoku.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_game")
    fun getAll(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_game WHERE board_uid == :uid")
    suspend fun get(uid: Long): SavedGame?

    @Query(
        "SELECT * FROM saved_game " +
                "JOIN board ON saved_game.board_uid == board.uid " +
                "ORDER BY uid DESC"
    )
    fun getSavedWithBoards(): Flow<Map<SavedGame, SudokuBoard>>

    @Query(
        "SELECT * " +
                "FROM saved_game " +
                "ORDER BY board_uid DESC " +
                "LIMIT 1"
    )
    fun getLast(): Flow<SavedGame?>

    @Insert
    suspend fun insert(savedGame: SavedGame): Long

    @Insert
    suspend fun insert(savedGames: List<SavedGame>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(savedGame: SavedGame)

    @Delete
    suspend fun delete(savedGame: SavedGame)
}