package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@Singleton
class AppSettingsManager(context: Context) {
    private val Context.createDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore = context.createDataStore

    // first app launch
    private val firstLaunchKey = booleanPreferencesKey("first_launch")
    // input method (0 -> cell first, 1 -> digit first)
    private val inputMethodKey = intPreferencesKey("input_method")
    // mistakes limit
    private val mistakesLimitKey = booleanPreferencesKey("mistakes_limit")
    // disable hint button
    private val hintsDisabledKey = booleanPreferencesKey("hints_disabled")
    // show timer
    private val timerKey = booleanPreferencesKey("timer")
    // game reset resets timer
    private val resetTimerKey = booleanPreferencesKey("timer_reset")
    // highlight mistakes
    private val highlightMistakesKey = intPreferencesKey("mistakes_highlight")
    // highlight same numbers
    private val highlightIdenticalKey = booleanPreferencesKey("same_values_highlight")
    // count and show remaining uses for numbers
    private val remainingUseKey = booleanPreferencesKey("remaining_use")
    // highlight current position with horizontal and vertical lines
    private val positionLinesKey = booleanPreferencesKey("position_lines")
    // auto erase notes
    private val autoEraseNotesKey = booleanPreferencesKey("notes_auto_erase")
    // font size (0 - small, 1 - medium (default), 2 - big)
    private val fontSizeKey = intPreferencesKey("font_size")
    // keep screen on
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")
    // first game
    private val firstGameKey = booleanPreferencesKey("first_game")

    suspend fun setFirstLaunch(value: Boolean) {
        dataStore.edit { settings ->
            settings[firstLaunchKey] = value
        }
    }
    val firstLaunch = dataStore.data.map { preferences ->
        preferences[firstLaunchKey] ?: true
    }

    suspend fun setMistakesLimit(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[mistakesLimitKey] = enabled
        }
    }
    val mistakesLimit = dataStore.data.map { preferences ->
        preferences[mistakesLimitKey] ?: false
    }

    suspend fun setHintsDisabled(disabled: Boolean) {
        dataStore.edit { settings ->
            settings[hintsDisabledKey] = disabled
        }
    }
    val hintsDisabled = dataStore.data.map { preferences ->
        preferences[hintsDisabledKey] ?: false
    }

    suspend fun setTimer(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[timerKey] = enabled
        }
    }
    val timerEnabled = dataStore.data.map { preferences ->
        preferences[timerKey] ?: true
    }

    suspend fun setResetTimer(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[resetTimerKey] = enabled
        }
    }
    val resetTimerEnabled = dataStore.data.map { preferences ->
        preferences[resetTimerKey] ?: true
    }

    suspend fun setHighlightMistakes(value: Int) {
        dataStore.edit { settings ->
            settings[highlightMistakesKey] = value
        }
    }
    val highlightMistakes = dataStore.data.map { preferences ->
        preferences[highlightMistakesKey] ?: 1
    }

    suspend fun setSameValuesHighlight(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[highlightIdenticalKey] = enabled
        }
    }
    val highlightIdentical = dataStore.data.map { preferences ->
        preferences[highlightIdenticalKey] ?: true
    }

    suspend fun setRemainingUse(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[remainingUseKey] = enabled
        }
    }
    val remainingUse = dataStore.data.map { preferences ->
        preferences[remainingUseKey] ?: true
    }

    suspend fun setPositionLines(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[positionLinesKey] = enabled
        }
    }
    val positionLines = dataStore.data.map { preferences ->
        preferences[positionLinesKey] ?: true
    }

    suspend fun setAutoEraseNotes(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[autoEraseNotesKey] = enabled
        }
    }
    val autoEraseNotes = dataStore.data.map { preferences ->
        preferences[autoEraseNotesKey] ?: true
    }

    suspend fun setInputMethod(value: Int) {
        dataStore.edit { settings ->
            settings[inputMethodKey] = value
        }
    }
    val inputMethod = dataStore.data.map { preferences ->
        preferences[inputMethodKey] ?: 1
    }

    suspend fun setFontSize(value: Int) {
        dataStore.edit { settings ->
            settings[fontSizeKey] = value
        }
    }
    val fontSize = dataStore.data.map { preferences ->
        preferences[fontSizeKey] ?: 1
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[keepScreenOnKey] = enabled
        }
    }
    val keepScreenOn = dataStore.data.map { preferences ->
        preferences[keepScreenOnKey] ?: false
    }

    suspend fun setFirstGame(value: Boolean) {
        dataStore.edit { settings ->
            settings[firstGameKey] = value
        }
    }
    val firstGame = dataStore.data.map { preferences ->
        preferences[firstGameKey] ?: true
    }
}