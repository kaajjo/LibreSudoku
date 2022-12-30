package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.data.database.dao.BoardDao
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

class BoardRepository(
    private val boardDao: BoardDao
) {
    fun getAll(): Flow<List<SudokuBoard>> = boardDao.getAll()

    fun getAll(gameDifficulty: GameDifficulty): Flow<List<SudokuBoard>> = boardDao.getAll(gameDifficulty)
    fun getAllList(): List<SudokuBoard> = boardDao.getAllList()

    fun getSavedGamesWithBoard(): Flow<Map<SudokuBoard, SavedGame?>> = boardDao.getBoardsWithSavedGames()

    fun getSavedGamesWithBoard(difficulty: GameDifficulty): Flow<Map<SudokuBoard, SavedGame?>> = boardDao.getBoardsWithSavedGames(difficulty)

    suspend fun get(uid: Long): SudokuBoard = boardDao.get(uid)
    suspend fun insert(boardEntity: SudokuBoard): Long = boardDao.insert(boardEntity)
    suspend fun delete(boardEntity: SudokuBoard) = boardDao.delete(boardEntity)
    suspend fun update(boardEntity: SudokuBoard) = boardDao.update(boardEntity)
}