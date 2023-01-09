package com.kaajjo.libresudoku.ui.settings

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.settings.components.AppThemePreviewItem
import com.kaajjo.libresudoku.ui.theme.AppColorScheme
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navigateBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
    ) {
        val highlightMistakes by viewModel.highlightMistakes.collectAsState(initial = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES)
        val inputMethod by viewModel.inputMethod.collectAsState(initial = PreferencesConstants.DEFAULT_INPUT_METHOD)
        val darkTheme by viewModel.darkTheme.collectAsState(initial = PreferencesConstants.DEFAULT_DARK_THEME)
        val fontSize by viewModel.fontSize.collectAsState(initial = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
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
                val dynamicColors by viewModel.dynamicColors.collectAsState(initial = PreferencesConstants.DEFAULT_DYNAMIC_COLORS)
                val amoledBlackState by viewModel.amoledBlack.collectAsState(initial = PreferencesConstants.DEFAULT_AMOLED_BLACK)
                val currentTheme by viewModel.currentTheme.collectAsState(initial = PreferencesConstants.DEFAULT_SELECTED_THEME)

                val currentThemeValue = when (currentTheme) {
                    PreferencesConstants.GREEN_THEME_KEY -> AppTheme.Green
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

                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_pure_black),
                    checked = amoledBlackState,
                    onClick = {
                        viewModel.updateAmoledBlack(!amoledBlackState)
                    }
                )

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

                val currentLanguage by remember {
                    mutableStateOf(
                        viewModel.getCurrentLocaleString(context)
                    )
                }
                PreferenceRow(
                    title = stringResource(R.string.pref_app_language),
                    subtitle = currentLanguage,
                    onClick = { viewModel.languagePickDialog = true }
                )

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

                val mistakesLimit by viewModel.mistakesLimit.collectAsState(initial = PreferencesConstants.DEFAULT_MISTAKES_LIMIT)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_mistakes_limit),
                    subtitle = stringResource(R.string.pref_mistakes_limit_summ),
                    checked = mistakesLimit,
                    onClick = { viewModel.updateMistakesLimit(!mistakesLimit) }
                )

                val hintDisabled by viewModel.disableHints.collectAsState(initial = PreferencesConstants.DEFAULT_HINTS_DISABLED)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_disable_hints),
                    subtitle = stringResource(R.string.pref_disable_hints_summ),
                    checked = hintDisabled,
                    onClick = { viewModel.updateHintDisabled(!hintDisabled) }
                )

                val timerEnabled by viewModel.timer.collectAsState(initial = PreferencesConstants.DEFAULT_SHOW_TIMER)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_timer),
                    checked = timerEnabled,
                    onClick = { viewModel.updateTimer(!timerEnabled) }
                )

                val resetTimer by viewModel.canResetTimer.collectAsState(initial = PreferencesConstants.DEFAULT_GAME_RESET_TIMER)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_reset_timer),
                    checked = resetTimer,
                    onClick = { viewModel.updateCanResetTimer(!resetTimer) }
                )

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

                val highlightIdentical by viewModel.highlightIdentical.collectAsState(initial = PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_highlight_identical),
                    subtitle = stringResource(R.string.pref_highlight_identical_summ),
                    checked = highlightIdentical,
                    onClick = {
                        viewModel.updateHighlightIdentical(!highlightIdentical)
                    }
                )

                val remainingUse by viewModel.remainingUse.collectAsState(initial = PreferencesConstants.DEFAULT_REMAINING_USES)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_remaining_uses),
                    subtitle = stringResource(R.string.pref_remaining_uses_summ),
                    checked = remainingUse,
                    onClick = { viewModel.updateRemainingUse(!remainingUse) }
                )

                val positionLines by viewModel.positionLines.collectAsState(initial = PreferencesConstants.DEFAULT_POSITION_LINES)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_position_lines),
                    subtitle = stringResource(R.string.pref_position_lines_summ),
                    checked = positionLines,
                    onClick = { viewModel.updatePositionLines(!positionLines) }
                )

                val autoEraseNotes by viewModel.autoEraseNotes.collectAsState(initial = PreferencesConstants.DEFAULT_AUTO_ERASE_NOTES)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_auto_erase_notes),
                    checked = autoEraseNotes,
                    onClick = { viewModel.updateAutoEraseNotes(!autoEraseNotes) }
                )

                Divider(
                    modifier = Modifier.fillMaxWidth()
                )

                SettingsCategory(
                    title = stringResource(R.string.pref_other)
                )

                val keepScreenOn by viewModel.keepScreenOn.collectAsState(initial = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_keep_screen_on),
                    checked = keepScreenOn,
                    onClick = {
                        viewModel.updateKeepScreenOn(!keepScreenOn)
                    }
                )

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

                if (viewModel.launchedFromGame == null || viewModel.launchedFromGame == false) {
                    PreferenceRow(
                        title = stringResource(R.string.pref_delete_stats),
                        onClick = {
                            viewModel.resetStatsDialog = true
                        }
                    )
                }
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
            LanguagePicker(
                title = stringResource(R.string.pref_app_language),
                entries = viewModel.getLangs(context),
                selected = viewModel.getCurrentLocaleString(context),
                onSelect = { localeKey ->
                    val locale = if (localeKey == "") {
                        LocaleListCompat.getEmptyLocaleList()
                    } else {
                        LocaleListCompat.forLanguageTags(localeKey)
                    }
                    AppCompatDelegate.setApplicationLocales(locale)
                },
                onDismiss = { viewModel.languagePickDialog = false }
            )
        }
    }
}

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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
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