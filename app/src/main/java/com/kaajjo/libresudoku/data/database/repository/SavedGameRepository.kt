package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.data.database.dao.SavedGameDao
import com.kaajjo.libresudoku.data.database.model.SavedGame
import kotlinx.coroutines.flow.Flow

class SavedGameRepository(
    private val savedGameDao: SavedGameDao
) {
    fun getAll(): Flow<List<SavedGame>> = savedGameDao.getAll()

    suspend fun get(uid: Long): SavedGame? = savedGameDao.get(uid)

    fun getLast(): Flow<SavedGame?> = savedGameDao.getLast()

    suspend fun insert(savedGame: SavedGame): Long = savedGameDao.insert(savedGame)

    suspend fun update(savedGame: SavedGame) = savedGameDao.update(savedGame)

    suspend fun delete(savedGame: SavedGame) = savedGameDao.delete(savedGame)
}