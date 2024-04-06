package com.kaajjo.libresudoku.domain.repository

import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import kotlinx.coroutines.flow.Flow

interface SavedGameRepository {
    fun getAll(): Flow<List<SavedGame>>
    suspend fun get(uid: Long): SavedGame?
    fun getWithBoards(): Flow<Map<SavedGame, SudokuBoard>>
    fun getLast(): Flow<SavedGame?>
    fun getLastPlayable(limit: Int): Flow<Map<SavedGame, SudokuBoard>>
    suspend fun insert(savedGame: SavedGame): Long
    suspend fun insert(savedGames: List<SavedGame>)
    suspend fun update(savedGame: SavedGame)
    suspend fun delete(savedGame: SavedGame)
}