package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.dao.RecordDao
import com.kaajjo.libresudoku.data.database.model.Record
import kotlinx.coroutines.flow.Flow

class RecordRepository(
    private val recordDao: RecordDao
) {
    suspend fun get(uid: Long): Record = recordDao.get(uid)
    fun getAll(): Flow<List<Record>> = recordDao.getAll()
    fun getAllSortByTime(): Flow<List<Record>> = recordDao.getAllSortByTime()
    fun getAll(difficulty: GameDifficulty, type: GameType) = recordDao.getAll(difficulty, type)
    suspend fun insert(record: Record) = recordDao.insert(record)
    suspend fun delete(record: Record) = recordDao.delete(record)
}