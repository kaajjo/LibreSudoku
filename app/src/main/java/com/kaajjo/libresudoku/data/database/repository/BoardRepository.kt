package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.data.database.dao.BoardDao
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

class BoardRepository(
    private val boardDao: BoardDao
) {
    fun getAll(): Flow<List<SudokuBoard>> = boardDao.getAll()
    fun getAllList(): List<SudokuBoard> = boardDao.getAllList()
    suspend fun get(uid: Long): SudokuBoard = boardDao.get(uid)
    suspend fun insert(boardEntity: SudokuBoard): Long = boardDao.insert(boardEntity)
    suspend fun delete(boardEntity: SudokuBoard) = boardDao.delete(boardEntity)
    suspend fun update(boardEntity: SudokuBoard) = boardDao.update(boardEntity)
}