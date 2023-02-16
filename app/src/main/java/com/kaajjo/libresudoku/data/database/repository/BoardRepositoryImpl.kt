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

    override fun getAllInFolder(folderUid: Long): Flow<List<SudokuBoard>> =
        boardDao.getAllInFolder(folderUid)

    override fun getAllInFolderList(folderUid: Long): List<SudokuBoard> =
        boardDao.getAllInFolderList(folderUid)

    override fun getWithSavedGames(): Flow<Map<SudokuBoard, SavedGame?>> =
        boardDao.getBoardsWithSavedGames()

    override fun getWithSavedGames(difficulty: GameDifficulty): Flow<Map<SudokuBoard, SavedGame?>> =
        boardDao.getBoardsWithSavedGames(difficulty)

    override fun getInFolderWithSaved(folderUid: Long): Flow<Map<SudokuBoard, SavedGame?>> =
        boardDao.getInFolderWithSaved(folderUid)

    override fun getBoardsInFolder(uid: Long): List<SudokuBoard> = boardDao.getBoardsInFolder(uid)
    override fun getBoardsInFolderFlow(uid: Long): Flow<List<SudokuBoard>> =
        boardDao.getBoardsInFolderFlow(uid)

    override suspend fun get(uid: Long): SudokuBoard = boardDao.get(uid)
    override suspend fun insert(boards: List<SudokuBoard>) = boardDao.insert(boards)
    override suspend fun insert(board: SudokuBoard): Long = boardDao.insert(board)
    override suspend fun delete(board: SudokuBoard) = boardDao.delete(board)
    override suspend fun delete(boards: List<SudokuBoard>) = boardDao.delete(boards)
    override suspend fun update(board: SudokuBoard) = boardDao.update(board)
    override suspend fun update(boards: List<SudokuBoard>) = boardDao.update(boards)
}