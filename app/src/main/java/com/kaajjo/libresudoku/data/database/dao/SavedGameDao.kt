package com.kaajjo.libresudoku.data.database.dao

import androidx.room.*
import com.kaajjo.libresudoku.data.database.model.SavedGame
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedGameDao {
    @Query("SELECT * FROM saved_game")
    fun getAll(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_game WHERE board_uid == :uid")
    suspend fun get(uid: Long): SavedGame?

    @Query("SELECT * " +
            "FROM saved_game " +
            "ORDER BY board_uid DESC " +
            "LIMIT 1")
    fun getLast(): Flow<SavedGame?>

    @Insert
    suspend fun insert(savedGame: SavedGame): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(savedGame: SavedGame)

    @Delete
    suspend fun delete(savedGame: SavedGame)
}