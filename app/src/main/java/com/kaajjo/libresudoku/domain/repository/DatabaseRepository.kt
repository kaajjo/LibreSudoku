package com.kaajjo.libresudoku.domain.repository

interface DatabaseRepository {
    suspend fun resetDb()
}