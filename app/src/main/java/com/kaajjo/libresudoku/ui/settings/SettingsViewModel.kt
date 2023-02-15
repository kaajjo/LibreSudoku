package com.kaajjo.libresudoku.ui.settings

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.data.database.AppDatabase
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.data.datastore.TipCardsDataStore
import com.kaajjo.libresudoku.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
@Inject constructor(
    private val settingsDataManager: AppSettingsManager,
    private val tipCardsDataStore: TipCardsDataStore,
    private val appDatabase: AppDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    @Inject
    lateinit var appThemeDataStore: ThemeSettingsManager

    val launchedFromGame by mutableStateOf(savedStateHandle.get<Boolean>("fromGame"))
    var resetStatsDialog by mutableStateOf(false)

    var darkModeDialog by mutableStateOf(false)
    var fontSizeDialog by mutableStateOf(false)
    var inputMethodDialog by mutableStateOf(false)
    var mistakesDialog by mutableStateOf(false)
    var languagePickDialog by mutableStateOf(false)

    val darkTheme by lazy {
        appThemeDataStore.darkTheme
    }

    fun updateDarkTheme(value: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            appThemeDataStore.setDarkTheme(value)
        }

    val dynamicColors by lazy {
        appThemeDataStore.dynamicColors
    }

    fun updateDynamicColors(enabled: Boolean) =
        viewModelScope.launch {
            appThemeDataStore.setDynamicColors(enabled)
        }

    val amoledBlack by lazy {
        appThemeDataStore.amoledBlack
    }

    fun updateAmoledBlack(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            appThemeDataStore.setAmoledBlack(enabled)
        }


    val mistakesLimit = settingsDataManager.mistakesLimit
    fun updateMistakesLimit(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setMistakesLimit(enabled)
        }

    val timer = settingsDataManager.timerEnabled
    fun updateTimer(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setTimer(enabled)
        }

    val canResetTimer = settingsDataManager.resetTimerEnabled
    fun updateCanResetTimer(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setResetTimer(enabled)
        }

    val highlightIdentical = settingsDataManager.highlightIdentical
    fun updateHighlightIdentical(enabled: Boolean) =
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setSameValuesHighlight(enabled)
        }

    val disableHints = settingsDataManager.hintsDisabled
    fun updateHintDisabled(disabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setHintsDisabled(disabled)
        }
    }

    val remainingUse = settingsDataManager.remainingUse
    fun updateRemainingUse(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setRemainingUse(enabled)
        }
    }

    val autoEraseNotes = settingsDataManager.autoEraseNotes
    fun updateAutoEraseNotes(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setAutoEraseNotes(enabled)
        }
    }

    val highlightMistakes = settingsDataManager.highlightMistakes
    fun updateMistakesHighlight(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setHighlightMistakes(index)
        }
    }

    val inputMethod = settingsDataManager.inputMethod
    fun updateInputMethod(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setInputMethod(value)
        }
    }

    fun resetTipCards() {
        viewModelScope.launch {
            tipCardsDataStore.setStreakCard(true)
            tipCardsDataStore.setRecordCard(true)
        }
    }

    fun deleteAllTables() {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
    }

    val fontSize = settingsDataManager.fontSize
    fun updateFontSize(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setFontSize(value)
        }
    }

    val currentTheme by lazy {
        appThemeDataStore.currentTheme
    }

    fun updateCurrentTheme(theme: AppTheme) {
        viewModelScope.launch(Dispatchers.IO) {
            appThemeDataStore.setCurrentTheme(theme)
        }
    }

    val keepScreenOn = settingsDataManager.keepScreenOn
    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setKeepScreenOn(enabled)
        }
    }

    fun getCurrentLocaleString(context: Context): String {
        val langs = getLangs(context)
        langs.forEach {
            Log.d("lang", "${it.key} ${it.value}")
        }
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales == LocaleListCompat.getEmptyLocaleList()) {
            return context.getString(R.string.pref_app_language_default)
        }
        return getDisplayName(locales.toLanguageTags())
    }

    fun getLangs(context: Context): Map<String, String> {
        val langs = mutableListOf<Pair<String, String>>()
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "name") {
                        val langTag = parser.getAttributeValue(i)
                        val displayName = getDisplayName(langTag)
                        if (displayName.isNotEmpty()) {
                            langs.add(Pair(langTag, displayName))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        langs.sortBy { it.second }
        langs.add(0, Pair("", context.getString(R.string.pref_app_language_default)))

        return langs.toMap()
    }

    private fun getDisplayName(lang: String?): String {
        if (lang == null) {
            return ""
        }

        val locale = when (lang) {
            "" -> LocaleListCompat.getAdjustedDefault()[0]
            else -> Locale.forLanguageTag(lang)
        }
        return locale!!.getDisplayName(locale).replaceFirstChar { it.uppercase(locale) }
    }
}