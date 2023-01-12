package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TipCardsDataStore @Inject constructor(@ApplicationContext context: Context) {
    private val Context.createDataStore: DataStore<Preferences> by preferencesDataStore(name = "tip_card")
    private val dataStore = context.createDataStore

    private val recordCardKey = booleanPreferencesKey("record")
    private val streakCardKey = booleanPreferencesKey("streak")

    suspend fun setRecordCard(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[recordCardKey] = enabled
        }
    }

    val recordCard = dataStore.data.map { preferences ->
        preferences[recordCardKey] ?: true
    }

    suspend fun setStreakCard(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[streakCardKey] = enabled
        }
    }

    val streakCard = dataStore.data.map { preferences ->
        preferences[streakCardKey] ?: true
    }
}