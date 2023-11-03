package com.kaajjo.libresudoku.data.database.repository

import com.kaajjo.libresudoku.data.database.AppDatabase
import com.kaajjo.libresudoku.domain.repository.DatabaseRepository
import kotlinx.coroutines.runBlocking

class DatabaseRepositoryImpl(
    private val appDatabase: AppDatabase
) : DatabaseRepository {
    /**
     * Completely resets database. Clearing all tables and primary key sequence
     */
    override suspend fun resetDb() {
        appDatabase.runInTransaction {
            runBlocking {
                appDatabase.clearAllTables()
                appDatabase.databaseDao().clearPrimaryKeyIndex()
            }
        }
    }
}