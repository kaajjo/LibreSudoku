package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.kaajjo.libresudoku.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeSettingsManager @Inject constructor(@ApplicationContext context: Context) {
    private val Context.createDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_theme")
    private val dataStore = context.createDataStore

    // material you colors
    private val dynamicColorsKey = booleanPreferencesKey("dynamic_colors")
    // 0 - system default, 1 - off, 2 - on
    private val darkThemeKey = intPreferencesKey("dark_theme")
    // amoled black theme, 0 - disabled, 1 enabled
    private val amoledBlackKey = booleanPreferencesKey("amoled_black")
    // current app theme
    private val currentThemeKey = stringPreferencesKey("current_theme")

    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[dynamicColorsKey] = enabled
        }
    }
    val dynamicColors = dataStore.data.map { preferences ->
        preferences[dynamicColorsKey] ?: true
    }

    suspend fun setDarkTheme(value: Int) {
        dataStore.edit { settings ->
            settings[darkThemeKey] = value
        }
    }
    val darkTheme = dataStore.data.map { preferences ->
        preferences[darkThemeKey] ?: 0
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[amoledBlackKey] = enabled
        }
    }
    val amoledBlack = dataStore.data.map { preferences ->
        preferences[amoledBlackKey] ?: false
    }

    suspend fun setCurrentTheme(appTheme: AppTheme) {
        val stringTheme = when(appTheme) {
            AppTheme.Green -> "green"
            AppTheme.Pink -> "pink"
            AppTheme.Yellow -> "yellow"
            AppTheme.Lavender -> "lavender"
            AppTheme.BlackAndWhite -> "black_and_white"
        }
        dataStore.edit { settings ->
            settings[currentThemeKey] = stringTheme
        }
    }
    val currentTheme = dataStore.data.map { preferences ->
        preferences[currentThemeKey] ?: "green"
    }
}