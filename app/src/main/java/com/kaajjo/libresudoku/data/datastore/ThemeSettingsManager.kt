package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaajjo.libresudoku.core.PreferencesConstants
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
        // return dynamic theme true only if an android version >= 12
        preferences[dynamicColorsKey] ?: (SDK_INT >= VERSION_CODES.S)
    }

    suspend fun setDarkTheme(value: Int) {
        dataStore.edit { settings ->
            settings[darkThemeKey] = value
        }
    }

    val darkTheme = dataStore.data.map { preferences ->
        preferences[darkThemeKey] ?: PreferencesConstants.DEFAULT_DARK_THEME
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[amoledBlackKey] = enabled
        }
    }

    val amoledBlack = dataStore.data.map { preferences ->
        preferences[amoledBlackKey] ?: PreferencesConstants.DEFAULT_AMOLED_BLACK
    }

    suspend fun setCurrentTheme(appTheme: AppTheme) {
        val stringTheme = when (appTheme) {
            AppTheme.Green -> PreferencesConstants.GREEN_THEME_KEY
            AppTheme.Peach -> PreferencesConstants.PEACH_THEME_KEY
            AppTheme.Yellow -> PreferencesConstants.YELLOW_THEME_KEY
            AppTheme.Lavender -> PreferencesConstants.LAVENDER_THEME_KEY
            AppTheme.BlackAndWhite -> PreferencesConstants.BLACK_AND_WHITE_THEME_KEY
        }
        dataStore.edit { settings ->
            settings[currentThemeKey] = stringTheme
        }
    }

    val currentTheme = dataStore.data.map { preferences ->
        preferences[currentThemeKey] ?: PreferencesConstants.DEFAULT_SELECTED_THEME
    }
}