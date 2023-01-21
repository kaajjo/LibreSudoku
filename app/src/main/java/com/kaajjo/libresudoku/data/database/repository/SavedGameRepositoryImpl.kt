package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.data.database.dao.SavedGameDao
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.domain.repository.SavedGameRepository
import kotlinx.coroutines.flow.Flow

class SavedGameRepositoryImpl(
    private val savedGameDao: SavedGameDao
) : SavedGameRepository {
    override fun getAll(): Flow<List<SavedGame>> = savedGameDao.getAll()

    override suspend fun get(uid: Long): SavedGame? = savedGameDao.get(uid)

    override fun getWithBoards(): Flow<Map<SavedGame, SudokuBoard>> = savedGameDao.getSavedWithBoards()

    override fun getLast(): Flow<SavedGame?> = savedGameDao.getLast()

    override suspend fun insert(savedGame: SavedGame): Long = savedGameDao.insert(savedGame)

    override suspend fun update(savedGame: SavedGame) = savedGameDao.update(savedGame)

    override suspend fun delete(savedGame: SavedGame) = savedGameDao.delete(savedGame)
}