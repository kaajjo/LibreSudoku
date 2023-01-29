package com.kaajjo.libresudoku.domain.repository

import com.kaajjo.libresudoku.data.database.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAll(): Flow<List<Folder>>
    fun get(uid: Long): Flow<Folder>
    fun countPuzzlesFolder(uid: Long): Long
    suspend fun insert(folder: Folder): Long
    suspend fun update(folder: Folder)
    suspend fun delete(folder: Folder)
}