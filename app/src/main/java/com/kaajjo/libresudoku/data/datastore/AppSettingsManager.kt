package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaajjo.libresudoku.core.PreferencesConstants
import kotlinx.coroutines.flow.map
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
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

    // place function keyboard (undo, erase etc.) above the numbers keyboard
    private val funKeyboardOverNumKey = booleanPreferencesKey("fun_keyboard_over_numbers")

    // custom date format
    private val dateFormatKey = stringPreferencesKey("date_format")

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
        preferences[mistakesLimitKey] ?: PreferencesConstants.DEFAULT_MISTAKES_LIMIT
    }

    suspend fun setHintsDisabled(disabled: Boolean) {
        dataStore.edit { settings ->
            settings[hintsDisabledKey] = disabled
        }
    }

    val hintsDisabled = dataStore.data.map { preferences ->
        preferences[hintsDisabledKey] ?: PreferencesConstants.DEFAULT_HINTS_DISABLED
    }

    suspend fun setTimer(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[timerKey] = enabled
        }
    }

    val timerEnabled = dataStore.data.map { preferences ->
        preferences[timerKey] ?: PreferencesConstants.DEFAULT_SHOW_TIMER
    }

    suspend fun setResetTimer(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[resetTimerKey] = enabled
        }
    }

    val resetTimerEnabled = dataStore.data.map { preferences ->
        preferences[resetTimerKey] ?: PreferencesConstants.DEFAULT_GAME_RESET_TIMER
    }

    suspend fun setHighlightMistakes(value: Int) {
        dataStore.edit { settings ->
            settings[highlightMistakesKey] = value
        }
    }

    val highlightMistakes = dataStore.data.map { preferences ->
        preferences[highlightMistakesKey] ?: PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES
    }

    suspend fun setSameValuesHighlight(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[highlightIdenticalKey] = enabled
        }
    }

    val highlightIdentical = dataStore.data.map { preferences ->
        preferences[highlightIdenticalKey] ?: PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL
    }

    suspend fun setRemainingUse(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[remainingUseKey] = enabled
        }
    }

    val remainingUse = dataStore.data.map { preferences ->
        preferences[remainingUseKey] ?: PreferencesConstants.DEFAULT_REMAINING_USES
    }

    suspend fun setPositionLines(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[positionLinesKey] = enabled
        }
    }

    val positionLines = dataStore.data.map { preferences ->
        preferences[positionLinesKey] ?: PreferencesConstants.DEFAULT_POSITION_LINES
    }

    suspend fun setAutoEraseNotes(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[autoEraseNotesKey] = enabled
        }
    }

    val autoEraseNotes = dataStore.data.map { preferences ->
        preferences[autoEraseNotesKey] ?: PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES
    }

    suspend fun setInputMethod(value: Int) {
        dataStore.edit { settings ->
            settings[inputMethodKey] = value
        }
    }

    val inputMethod = dataStore.data.map { preferences ->
        preferences[inputMethodKey] ?: PreferencesConstants.DEFAULT_INPUT_METHOD
    }

    suspend fun setFontSize(value: Int) {
        dataStore.edit { settings ->
            settings[fontSizeKey] = value
        }
    }

    val fontSize = dataStore.data.map { preferences ->
        preferences[fontSizeKey] ?: PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[keepScreenOnKey] = enabled
        }
    }

    val keepScreenOn = dataStore.data.map { preferences ->
        preferences[keepScreenOnKey] ?: PreferencesConstants.DEFAULT_KEEP_SCREEN_ON
    }

    suspend fun setFirstGame(value: Boolean) {
        dataStore.edit { settings ->
            settings[firstGameKey] = value
        }
    }

    val firstGame = dataStore.data.map { preferences ->
        preferences[firstGameKey] ?: true
    }

    suspend fun setFunKeyboardOverNum(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[funKeyboardOverNumKey] = enabled
        }
    }

    val funKeyboardOverNumbers = dataStore.data.map { prefs ->
        prefs[funKeyboardOverNumKey] ?: PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM
    }

    suspend fun setDateFormat(format: String) {
        dataStore.edit { settings ->
            settings[dateFormatKey] = format
        }
    }

    val dateFormat = dataStore.data.map { prefs ->
        prefs[dateFormatKey] ?: ""
    }

    companion object {
        fun dateFormat(format: String): DateTimeFormatter = when (format) {
            "" -> {
                DateTimeFormatter.ofPattern(
                    DateTimeFormatterBuilder.getLocalizedDateTimePattern(
                        FormatStyle.SHORT,
                        null,
                        IsoChronology.INSTANCE,
                        Locale.getDefault()
                    )
                )
            }

            else -> {
                DateTimeFormatter.ofPattern(format, Locale.getDefault())
            }
        }
    }
}