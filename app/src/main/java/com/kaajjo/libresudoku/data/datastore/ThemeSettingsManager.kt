package com.kaajjo.libresudoku.data.datastore

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.materialkolor.PaletteStyle
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

    // colorful sudoku board with dynamic theme colors
    private val monetSudokuBoardKey = booleanPreferencesKey("monet_sudoku_board")

    private val boardCrossHighlightKey = booleanPreferencesKey("board_cross_highlight")

    // seed color for the custom dynamic color scheme
    private val themeSeedColorKey = intPreferencesKey("theme_seed_color")

    // palette style for the custom dynamic color scheme
    private val paletteStyleKey = intPreferencesKey("palette_style")

    private val isUserDefinedSeedColorKey = booleanPreferencesKey("is_user_defined_seed_color")

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

    suspend fun setMonetSudokuBoard(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[monetSudokuBoardKey] = enabled
        }
    }

    val monetSudokuBoard = dataStore.data.map { preferences ->
        preferences[monetSudokuBoardKey] ?: PreferencesConstants.DEFAULT_MONET_SUDOKU_BOARD
    }

    suspend fun setBoardCrossHighlight(enabled: Boolean) {
        dataStore.edit { settings ->
            settings[boardCrossHighlightKey] = enabled
        }
    }

    val boardCrossHighlight = dataStore.data.map { preferences ->
        preferences[boardCrossHighlightKey] ?: PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT
    }

    suspend fun setCurrentThemeColor(color: Color) {
        dataStore.edit { prefs ->
            prefs[themeSeedColorKey] = color.toArgb()
        }
    }

    val themeColorSeed = dataStore.data.map { prefs ->
        Color(prefs[themeSeedColorKey] ?: Color.Green.toArgb())
    }

    suspend fun setPaletteStyle(style: PaletteStyle) {
        dataStore.edit { prefs ->
            val index = paletteStyles.find { it.first == style }?.second ?: 0
            prefs[paletteStyleKey] = index
        }
    }

    val themePaletteStyle = dataStore.data.map { prefs ->
        val index = prefs[paletteStyleKey] ?: 0
        if (index in paletteStyles.indices) {
            paletteStyles[index].first
        } else {
            paletteStyles.first().first
        }
    }

    suspend fun setIsUserDefinedSeedColor(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[isUserDefinedSeedColorKey] = value
        }
    }

    val isUserDefinedSeedColor = dataStore.data.map { prefs ->
        prefs[isUserDefinedSeedColorKey] ?: false
    }


    companion object {
        val paletteStyles = listOf(
            PaletteStyle.TonalSpot to 0,
            PaletteStyle.Neutral to 1,
            PaletteStyle.Vibrant to 2,
            PaletteStyle.Expressive to 3,
            PaletteStyle.Rainbow to 4,
            PaletteStyle.FruitSalad to 5,
            PaletteStyle.Monochrome to 6,
            PaletteStyle.Fidelity to 7,
            PaletteStyle.Content to 8,
        )

        fun getPaletteStyle(index: Int) =
            if (index in paletteStyles.indices) paletteStyles[index].first else paletteStyles[0].first

        fun getPaletteIndex(paletteStyle: PaletteStyle, default: Int = 0) =
            paletteStyles.find { it.first == paletteStyle }?.second ?: default
    }
}