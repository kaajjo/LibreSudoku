package com.kaajjo.libresudoku.ui.settings

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTitle
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTopAppBar
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.rememberTopAppBarScrollBehavior
import com.kaajjo.libresudoku.ui.settings.components.AppThemePreviewItem
import com.kaajjo.libresudoku.ui.theme.AppColorScheme
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import java.time.ZonedDateTime
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    navigateBoardSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = rememberTopAppBarScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CollapsingTopAppBar(
                collapsingTitle = CollapsingTitle.medium(titleText = stringResource(R.string.settings_title)),
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        val highlightMistakes by viewModel.highlightMistakes.collectAsStateWithLifecycle(
            initialValue = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES
        )
        val inputMethod by viewModel.inputMethod.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_INPUT_METHOD)
        val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_DARK_THEME)
        val fontSize by viewModel.fontSize.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR)
        val dateFormat by viewModel.dateFormat.collectAsStateWithLifecycle(initialValue = "")
        val dynamicColors by viewModel.dynamicColors.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_DYNAMIC_COLORS)
        val amoledBlackState by viewModel.amoledBlack.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_AMOLED_BLACK)
        val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SELECTED_THEME)
        val hintDisabled by viewModel.disableHints.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_HINTS_DISABLED)
        val mistakesLimit by viewModel.mistakesLimit.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_MISTAKES_LIMIT)
        val timerEnabled by viewModel.timer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SHOW_TIMER)
        val resetTimer by viewModel.canResetTimer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_GAME_RESET_TIMER)
        val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON)
        val autoEraseNotes by viewModel.autoEraseNotes.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES)
        val highlightIdentical by viewModel.highlightIdentical.collectAsStateWithLifecycle(
            initialValue = PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL
        )
        val remainingUse by viewModel.remainingUse.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_REMAINING_USES)

        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            item {
                SettingsCategory(
                    title = stringResource(R.string.pref_appearance)
                )
                PreferenceRow(
                    title = stringResource(R.string.pref_dark_theme),
                    subtitle = when (darkTheme) {
                        0 -> stringResource(R.string.pref_dark_theme_follow)
                        1 -> stringResource(R.string.pref_dark_theme_off)
                        2 -> stringResource(R.string.pref_dark_theme_on)
                        else -> ""
                    },
                    onClick = { viewModel.darkModeDialog = true }
                )
            }

            item {
                val currentThemeValue = when (currentTheme) {
                    PreferencesConstants.GREEN_THEME_KEY -> AppTheme.Green
                    PreferencesConstants.BLUE_THEME_KEY -> AppTheme.Blue
                    PreferencesConstants.PEACH_THEME_KEY -> AppTheme.Peach
                    PreferencesConstants.YELLOW_THEME_KEY -> AppTheme.Yellow
                    PreferencesConstants.LAVENDER_THEME_KEY -> AppTheme.Lavender
                    PreferencesConstants.BLACK_AND_WHITE_THEME_KEY -> AppTheme.BlackAndWhite
                    else -> AppTheme.Green
                }

                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.pref_app_theme)
                )
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp)
                ) {
                    val appTheme = AppColorScheme()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            LibreSudokuTheme(
                                dynamicColor = true,
                                darkTheme = when (darkTheme) {
                                    0 -> isSystemInDarkTheme()
                                    1 -> false
                                    else -> true
                                },
                                amoled = amoledBlackState
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .padding(start = 8.dp, end = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AppThemePreviewItem(
                                        selected = dynamicColors,
                                        onClick = {
                                            viewModel.updateDynamicColors(true)
                                        },
                                        colorScheme = MaterialTheme.colorScheme,
                                        shapes = MaterialTheme.shapes
                                    )
                                    Text(
                                        text = stringResource(R.string.theme_dynamic),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    items(enumValues<AppTheme>()) { theme ->
                        AppThemeItem(
                            title = when (theme) {
                                AppTheme.Green -> stringResource(R.string.theme_green)
                                AppTheme.Blue -> stringResource(R.string.theme_blue)
                                AppTheme.Peach -> stringResource(R.string.theme_peach)
                                AppTheme.Yellow -> stringResource(R.string.theme_yellow)
                                AppTheme.Lavender -> stringResource(R.string.theme_lavender)
                                AppTheme.BlackAndWhite -> stringResource(R.string.theme_black_and_white)
                            },
                            colorScheme = appTheme.getTheme(
                                theme, when (darkTheme) {
                                    0 -> isSystemInDarkTheme()
                                    1 -> false
                                    else -> true
                                }
                            ),
                            onClick = {
                                viewModel.updateDynamicColors(false)
                                viewModel.updateCurrentTheme(theme)
                            },
                            selected = currentThemeValue == theme && !dynamicColors,
                            amoledBlack = amoledBlackState,
                            darkTheme = darkTheme,

                            )
                    }
                }
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_pure_black),
                    checked = amoledBlackState,
                    onClick = {
                        viewModel.updateAmoledBlack(!amoledBlackState)
                    }
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_board_theme_title),
                    subtitle = stringResource(R.string.pref_board_theme_subtitle),
                    onClick = navigateBoardSettings
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_board_font_size),
                    subtitle = when (fontSize) {
                        0 -> stringResource(R.string.pref_board_font_size_small)
                        1 -> stringResource(R.string.pref_board_font_size_medium)
                        2 -> stringResource(R.string.pref_board_font_size_large)
                        else -> ""
                    },
                    onClick = { viewModel.fontSizeDialog = true }
                )
            }

            item {
                var currentLanguage by remember {
                    mutableStateOf(
                        getCurrentLocaleString(context)
                    )
                }
                LaunchedEffect(viewModel.languagePickDialog) {
                    currentLanguage = getCurrentLocaleString(context)
                }
                PreferenceRow(
                    title = stringResource(R.string.pref_app_language),
                    subtitle = currentLanguage,
                    onClick = { viewModel.languagePickDialog = true }
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_date_format),
                    subtitle = "${dateFormat.ifEmpty { stringResource(R.string.label_default) }} (${
                        ZonedDateTime.now().format(AppSettingsManager.dateFormat(dateFormat))
                    })",
                    onClick = { viewModel.dateFormatDialog = true }
                )
            }

            item {
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )

                SettingsCategory(
                    title = stringResource(R.string.pref_gameplay)
                )

                PreferenceRow(
                    title = stringResource(R.string.pref_input),
                    subtitle = when (inputMethod) {
                        0 -> stringResource(R.string.pref_input_cell_first)
                        1 -> stringResource(R.string.pref_input_digit_first)
                        else -> ""
                    },
                    onClick = { viewModel.inputMethodDialog = true }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_mistakes_limit),
                    subtitle = stringResource(R.string.pref_mistakes_limit_summ),
                    checked = mistakesLimit,
                    onClick = { viewModel.updateMistakesLimit(!mistakesLimit) }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_disable_hints),
                    subtitle = stringResource(R.string.pref_disable_hints_summ),
                    checked = hintDisabled,
                    onClick = { viewModel.updateHintDisabled(!hintDisabled) }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_timer),
                    checked = timerEnabled,
                    onClick = { viewModel.updateTimer(!timerEnabled) }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_reset_timer),
                    checked = resetTimer,
                    onClick = { viewModel.updateCanResetTimer(!resetTimer) }
                )
            }

            item {
                val funKeyboardOverNum by viewModel.funKeyboardOverNum.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM
                )
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_fun_keyboard_over_num),
                    subtitle = stringResource(R.string.pref_fun_keyboard_over_num_subtitle),
                    checked = funKeyboardOverNum,
                    onClick = {
                        viewModel.updateFunKeyboardOverNum(!funKeyboardOverNum)
                    }
                )
            }

            item {
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )

                SettingsCategory(
                    title = stringResource(R.string.pref_assistance)
                )

                PreferenceRow(
                    title = stringResource(R.string.pref_mistakes_check),
                    subtitle = when (highlightMistakes) {
                        0 -> stringResource(R.string.pref_mistakes_check_off)
                        1 -> stringResource(R.string.pref_mistakes_check_violations)
                        2 -> stringResource(R.string.pref_mistakes_check_final)
                        else -> stringResource(R.string.pref_mistakes_check_off)
                    },
                    onClick = { viewModel.mistakesDialog = true }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_highlight_identical),
                    subtitle = stringResource(R.string.pref_highlight_identical_summ),
                    checked = highlightIdentical,
                    onClick = {
                        viewModel.updateHighlightIdentical(!highlightIdentical)
                    }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_remaining_uses),
                    subtitle = stringResource(R.string.pref_remaining_uses_summ),
                    checked = remainingUse,
                    onClick = { viewModel.updateRemainingUse(!remainingUse) }
                )

            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_auto_erase_notes),
                    checked = autoEraseNotes,
                    onClick = { viewModel.updateAutoEraseNotes(!autoEraseNotes) }
                )
            }


            item {
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )

                SettingsCategory(
                    title = stringResource(R.string.pref_other)
                )
                val saveLastSelectedDifficultyType by viewModel.saveLastSelectedDifficultyType
                    .collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SAVE_LAST_SELECTED_DIFF_TYPE)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_save_last_diff_and_type),
                    subtitle = stringResource(R.string.pref_save_last_diff_and_type_subtitle),
                    checked = saveLastSelectedDifficultyType,
                    onClick = {
                        viewModel.updateSaveLastSelectedDifficultyType(!saveLastSelectedDifficultyType)
                    }
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_keep_screen_on),
                    checked = keepScreenOn,
                    onClick = {
                        viewModel.updateKeepScreenOn(!keepScreenOn)
                    }
                )
            }

            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_reset_tipcards),
                    onClick = {
                        viewModel.resetTipCards()
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.resources.getString(R.string.pref_tipcards_reset)
                            )
                        }
                    }
                )
            }

            item {
                if (viewModel.launchedFromGame == null || viewModel.launchedFromGame == false) {
                    PreferenceRow(
                        title = stringResource(R.string.pref_delete_stats),
                        onClick = {
                            viewModel.resetStatsDialog = true
                        }
                    )
                }
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_crash_reporting),
                    subtitle = stringResource(R.string.pref_crash_reporting_subtitle),
                    checked = viewModel.crashReportingEnabled,
                    onClick = {
                        viewModel.updateCrashReportingEnabled(!viewModel.crashReportingEnabled)
                    }
                )
            }
        }

        if (viewModel.mistakesDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_mistakes_check),
                selections = listOf(
                    stringResource(R.string.pref_mistakes_check_off),
                    stringResource(R.string.pref_mistakes_check_violations),
                    stringResource(R.string.pref_mistakes_check_final)
                ),
                selected = highlightMistakes,
                onSelect = { index ->
                    viewModel.updateMistakesHighlight(index)
                },
                onDismiss = { viewModel.mistakesDialog = false }
            )
        } else if (viewModel.darkModeDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_dark_theme),
                selections = listOf(
                    stringResource(R.string.pref_dark_theme_follow),
                    stringResource(R.string.pref_dark_theme_off),
                    stringResource(R.string.pref_dark_theme_on)
                ),
                selected = darkTheme,
                onSelect = { index ->
                    viewModel.updateDarkTheme(index)
                },
                onDismiss = { viewModel.darkModeDialog = false }
            )
        } else if (viewModel.fontSizeDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_board_font_size),
                selections = listOf(
                    stringResource(R.string.pref_board_font_size_small),
                    stringResource(R.string.pref_board_font_size_medium),
                    stringResource(R.string.pref_board_font_size_large)
                ),
                selected = fontSize,
                onSelect = { index ->
                    viewModel.updateFontSize(index)
                },
                onDismiss = { viewModel.fontSizeDialog = false }
            )
        } else if (viewModel.inputMethodDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_input),
                selections = listOf(
                    stringResource(R.string.pref_input_cell_first),
                    stringResource(R.string.pref_input_digit_first)
                ),
                selected = inputMethod,
                onSelect = { index ->
                    viewModel.updateInputMethod(index)
                },
                onDismiss = { viewModel.inputMethodDialog = false }
            )
        } else if (viewModel.resetStatsDialog) {
            AlertDialog(
                title = { Text(stringResource(R.string.pref_delete_stats)) },
                text = { Text(stringResource(R.string.pref_delete_stats_summ)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllTables()
                        viewModel.resetStatsDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                context.resources.getString(R.string.action_deleted)
                            )
                        }
                    }) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                dismissButton = {
                    FilledTonalButton(onClick = { viewModel.resetStatsDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
                onDismissRequest = { viewModel.resetStatsDialog = false }
            )
        } else if (viewModel.languagePickDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_app_language),
                entries = getLangs(context),
                selected = getCurrentLocaleTag(),
                onSelect = { localeKey ->
                    val locale = if (localeKey == "") {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(localeKey)
                    }
                    AppCompatDelegate.setApplicationLocales(locale)
                    viewModel.languagePickDialog = false
                },
                onDismiss = { viewModel.languagePickDialog = false }
            )
        } else if (viewModel.dateFormatDialog) {
            DateFormatDialog(
                title = stringResource(R.string.pref_date_format),
                entries = DateFormats.associateWith { dateFormatEntry ->
                    val dateString = ZonedDateTime.now().format(
                        when (dateFormatEntry) {
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
                                DateTimeFormatter.ofPattern(dateFormatEntry)
                            }
                        }
                    )
                    "${dateFormatEntry.ifEmpty { stringResource(R.string.label_default) }} ($dateString)"
                },
                customDateFormatText =
                if (!DateFormats.contains(dateFormat))
                    "$dateFormat (${
                        ZonedDateTime.now().format(DateTimeFormatter.ofPattern(dateFormat))
                    })"
                else stringResource(R.string.pref_date_format_custom_label),
                selected = dateFormat,
                onSelect = { format ->
                    if (format == "custom") {
                        viewModel.customFormatDialog = true
                    } else {
                        viewModel.updateDateFormat(format)
                    }
                    viewModel.dateFormatDialog = false
                },
                onDismiss = { viewModel.dateFormatDialog = false },

                )
        }

        if (viewModel.customFormatDialog) {
            var customDateFormat by rememberSaveable {
                mutableStateOf(
                    if (DateFormats.contains(
                            dateFormat
                        )
                    ) "" else dateFormat
                )
            }
            var invalidCustomDateFormat by rememberSaveable { mutableStateOf(false) }
            var dateFormatPreview by rememberSaveable { mutableStateOf("") }

            SetDateFormatPatternDialog(
                onConfirm = {
                    if (viewModel.checkCustomDateFormat(customDateFormat)) {
                        viewModel.updateDateFormat(customDateFormat)
                        invalidCustomDateFormat = false
                        viewModel.customFormatDialog = false
                    } else {
                        invalidCustomDateFormat = true
                    }
                },
                onDismissRequest = { viewModel.customFormatDialog = false },
                onTextValueChange = { text ->
                    customDateFormat = text
                    if (invalidCustomDateFormat) invalidCustomDateFormat = false

                    dateFormatPreview = if (viewModel.checkCustomDateFormat(customDateFormat)) {
                        ZonedDateTime.now()
                            .format(DateTimeFormatter.ofPattern(customDateFormat))
                    } else {
                        ""
                    }
                },
                customDateFormat = customDateFormat,
                invalidCustomDateFormat = invalidCustomDateFormat,
                datePreview = dateFormatPreview
            )
        }
    }
}

private val DateFormats = listOf(
    "",
    "dd/MM/yy",
    "dd.MM.yy",
    "MM/dd/yy",
    "yyyy-MM-dd",
    "dd MMM yyyy",
    "MMM dd, yyyy"
)

@Composable
fun SettingsCategory(
    modifier: Modifier = Modifier,
    title: String
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 16.dp, top = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun AppThemeItem(
    title: String,
    colorScheme: ColorScheme,
    amoledBlack: Boolean,
    darkTheme: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .padding(start = 8.dp, end = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AppThemePreviewItem(
            selected = selected,
            onClick = onClick,
            colorScheme = colorScheme.copy(
                background =
                if (amoledBlack && (darkTheme == 0 && isSystemInDarkTheme() || darkTheme == 2)) {
                    Color.Black
                } else {
                    colorScheme.background
                }
            ),
            shapes = MaterialTheme.shapes
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall
        )
    }
}


private fun getCurrentLocaleString(context: Context): String {
    val langs = getLangs(context)
    langs.forEach {
        Log.d("lang", "${it.key} ${it.value}")
    }
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales == LocaleListCompat.getEmptyLocaleList()) {
        return context.getString(R.string.label_default)
    }
    return getDisplayName(locales.toLanguageTags())
}

private fun getCurrentLocaleTag(): String {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales == LocaleListCompat.getEmptyLocaleList()) {
        return ""
    }
    return locales.toLanguageTags()
}

private fun getLangs(context: Context): Map<String, String> {
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
    langs.add(0, Pair("", context.getString(R.string.label_default)))

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