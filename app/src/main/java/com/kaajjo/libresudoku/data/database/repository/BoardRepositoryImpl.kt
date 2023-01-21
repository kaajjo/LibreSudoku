package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.data.database.dao.BoardDao
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.domain.repository.BoardRepository
import kotlinx.coroutines.flow.Flow

class BoardRepositoryImpl(
    private val boardDao: BoardDao
) : BoardRepository {
    override fun getAll(): Flow<List<SudokuBoard>> =
        boardDao.getAll()

    override fun getAll(difficulty: GameDifficulty): Flow<List<SudokuBoard>> =
        boardDao.getAll(difficulty)

    override fun getWithSavedGames(): Flow<Map<SudokuBoard, SavedGame?>> =
        boardDao.getBoardsWithSavedGames()

    override fun getWithSavedGames(difficulty: GameDifficulty): Flow<Map<SudokuBoard, SavedGame?>> =
        boardDao.getBoardsWithSavedGames(difficulty)

    override suspend fun get(uid: Long): SudokuBoard = boardDao.get(uid)
    override suspend fun insert(boardEntity: SudokuBoard): Long = boardDao.insert(boardEntity)
    override suspend fun delete(boardEntity: SudokuBoard) = boardDao.delete(boardEntity)
    override suspend fun update(boardEntity: SudokuBoard) = boardDao.update(boardEntity)
}