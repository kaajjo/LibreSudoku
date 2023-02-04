package com.kaajjo.libresudoku.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.kaajjo.libresudoku.data.database.model.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM Folder")
    fun get(): Flow<List<Folder>>

    @Query("SELECT * FROM Folder WHERE uid = :uid")
    fun get(uid: Long): Flow<Folder>

    @Query("SELECT COUNT(uid) FROM board WHERE folder_id == :uid")
    fun countPuzzlesFolder(uid: Long): Long

    @Insert
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Delete
    suspend fun delete(folder: Folder)
}