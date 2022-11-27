package com.kaajjo.libresudoku.ui.settings

import android.os.Build
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
import androidx.navigation.NavController
import com.kaajjo.libresudoku.R
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
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val scope = rememberCoroutineScope()
    // enterAlways feels laggy
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
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
        val highlightMistakes = viewModel.highlightMistakes.collectAsState(initial = 0)
        val inputMethod = viewModel.inputMethod.collectAsState(initial = 0)
        val darkTheme = viewModel.darkTheme.collectAsState(initial = 0)
        val fontSize = viewModel.fontSize.collectAsState(initial = 1)
        if(viewModel.mistakesDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_mistakes_check),
                selections = listOf(
                    stringResource(R.string.pref_mistakes_check_off),
                    stringResource(R.string.pref_mistakes_check_violations),
                    stringResource(R.string.pref_mistakes_check_final)
                ),
                selected = highlightMistakes.value,
                onSelect = { index ->
                    viewModel.updateMistakesHighlight(index)
                },
                onDismiss = { viewModel.mistakesDialog = false }
            )
        } else if(viewModel.darkModeDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_dark_theme),
                selections = listOf(
                    stringResource(R.string.pref_dark_theme_follow),
                    stringResource(R.string.pref_dark_theme_off),
                    stringResource(R.string.pref_dark_theme_on)
                ),
                selected = darkTheme.value,
                onSelect = { index ->
                    viewModel.updateDarkTheme(index)
                },
                onDismiss = { viewModel.darkModeDialog = false }
            )
        } else if(viewModel.fontSizeDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_board_font_size),
                selections = listOf(
                    stringResource(R.string.pref_board_font_size_small),
                    stringResource(R.string.pref_board_font_size_medium),
                    stringResource(R.string.pref_board_font_size_large)
                ),
                selected = fontSize.value,
                onSelect = { index ->
                    viewModel.updateFontSize(index)
                },
                onDismiss = { viewModel.fontSizeDialog = false }
            )
        }
        else if(viewModel.inputMethodDialog) {
            SelectionDialog(
                title = stringResource(R.string.pref_input),
                selections = listOf(
                    stringResource(R.string.pref_input_cell_first),
                    stringResource(R.string.pref_input_digit_first)
                ),
                selected = inputMethod.value,
                onSelect = { index ->
                    viewModel.updateInputMethod(index)
                },
                onDismiss = { viewModel.inputMethodDialog = false }
            )
        }  else if(viewModel.resetStatsDialog) {
            val context = LocalContext.current
            AlertDialog(
                title = { Text(stringResource(R.string.pref_delete_stats)) },
                text = { Text(stringResource(R.string.pref_delete_stats_text)) },
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
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ){

                SettingsCategory(
                    title = stringResource(R.string.pref_appearance)
                )
                PreferenceRow(
                    title = stringResource(R.string.pref_dark_theme),
                    subtitle = when(darkTheme.value) {
                        0 -> stringResource(R.string.pref_dark_theme_follow)
                        1 -> stringResource(R.string.pref_dark_theme_off)
                        2 -> stringResource(R.string.pref_dark_theme_on)
                        else -> ""
                    },
                    onClick = { viewModel.darkModeDialog = true }
                )
                val dynamicColors = viewModel.dynamicColors.collectAsState(initial = true)
                val amoledBlackState = viewModel.amoledBlack.collectAsState(initial = false)
                val currentTheme: State<String?> = viewModel.currentTheme.collectAsState(initial = null)
                val currentThemeValue = if(currentTheme.value == null) {
                    null
                } else {
                    when(currentTheme.value) {
                        "green" -> AppTheme.Green
                        "pink" -> AppTheme.Pink
                        "yellow" -> AppTheme.Yellow
                        "lavender" -> AppTheme.Lavender
                        "black_and_white" -> AppTheme.BlackAndWhite
                        else -> AppTheme.Green
                    }
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
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        item {
                            LibreSudokuTheme(
                                dynamicColor = true,
                                darkTheme = when(darkTheme.value) {
                                    0 -> isSystemInDarkTheme()
                                    1 -> false
                                    else -> true
                                },
                                amoled = amoledBlackState.value
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .padding(start = 8.dp, end = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AppThemePreviewItem(
                                        selected = dynamicColors.value,
                                        onClick =  {
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
                            title = when(theme) {
                                AppTheme.Green -> stringResource(R.string.theme_green)
                                AppTheme.Pink -> stringResource(R.string.theme_peach)
                                AppTheme.Yellow -> stringResource(R.string.theme_yellow)
                                AppTheme.Lavender -> stringResource(R.string.theme_lavender)
                                AppTheme.BlackAndWhite -> stringResource(R.string.theme_black_and_white)
                            },
                            colorScheme = appTheme.getTheme(theme, when(darkTheme.value) {
                                0 -> isSystemInDarkTheme()
                                1 -> false
                                else -> true
                            }),
                            onClick = {
                                viewModel.updateDynamicColors(false)
                                viewModel.updateCurrentTheme(theme)
                            },
                            selected = currentThemeValue == theme && !dynamicColors.value,
                            amoledBlack = amoledBlackState.value,
                            darkTheme = darkTheme.value,

                        )
                    }
                }
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_pure_black),
                    checked = amoledBlackState.value,
                    onClick = {
                        viewModel.updateAmoledBlack(!amoledBlackState.value)
                    }
                )
                PreferenceRow(
                    title = stringResource(R.string.pref_board_font_size),
                    subtitle = when(fontSize.value) {
                        0 -> stringResource(R.string.pref_board_font_size_small)
                        1 -> stringResource(R.string.pref_board_font_size_medium)
                        2 -> stringResource(R.string.pref_board_font_size_large)
                        else -> ""
                    },
                    onClick = { viewModel.fontSizeDialog = true }
                )
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsCategory(
                    title = stringResource(R.string.pref_gameplay)
                )
                PreferenceRow(
                    title = stringResource(R.string.pref_input),
                    subtitle = when(inputMethod.value) {
                        0 -> stringResource(R.string.pref_input_cell_first)
                        1 -> stringResource(R.string.pref_input_digit_first)
                        else -> ""
                    },
                    onClick = { viewModel.inputMethodDialog = true }
                )
                val mistakesLimit = viewModel.mistakesLimit.collectAsState(initial = false)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_mistakes_limit),
                    subtitle = stringResource(R.string.pref_mistakes_limit_sub),
                    checked = mistakesLimit.value,
                    onClick = { viewModel.updateMistakesLimit(!mistakesLimit.value) }
                )

                val hintDisabled = viewModel.disableHints.collectAsState(initial = false)
                PreferenceRowSwitch(
                    title =  stringResource(R.string.pref_disable_hints),
                    subtitle = stringResource(R.string.pref_disable_hints_sub),
                    checked = hintDisabled.value,
                    onClick = { viewModel.updateHintDisabled(!hintDisabled.value) }
                )

                val timerEnabled = viewModel.timer.collectAsState(initial = true)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_timer),
                    checked = timerEnabled.value,
                    onClick = { viewModel.updateTimer(!timerEnabled.value) }
                )

                val resetTimer = viewModel.canResetTimer.collectAsState(initial = true)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_reset_timer),
                    checked = resetTimer.value,
                    onClick = { viewModel.updateCanResetTimer(!resetTimer.value) }
                )

                Divider(
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsCategory(
                    title = stringResource(R.string.pref_assistance)
                )
                PreferenceRow(
                    title = stringResource(R.string.pref_mistakes_check),
                    subtitle = when(highlightMistakes.value) {
                        0 -> stringResource(R.string.pref_mistakes_check_off)
                        1 -> stringResource(R.string.pref_mistakes_check_violations)
                        2 -> stringResource(R.string.pref_mistakes_check_final)
                        else -> stringResource(R.string.pref_mistakes_check_off)
                    },
                    onClick = { viewModel.mistakesDialog = true }
                )

                val highlightIdentical = viewModel.highlightIdentical.collectAsState(initial = true)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_highlight_identical),
                    subtitle = stringResource(R.string.pref_highlight_identical_sub),
                    checked = highlightIdentical.value,
                    onClick = {
                        viewModel.updateHighlightIdentical(!highlightIdentical.value)
                    }
                )
                val remainingUse = viewModel.remainingUse.collectAsState(initial = true)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_remaining_uses),
                    subtitle = stringResource(R.string.pref_remaining_uses_sub),
                    checked = remainingUse.value,
                    onClick = { viewModel.updateRemainingUse(!remainingUse.value) }
                )
                val positionLines = viewModel.positionLines.collectAsState(initial = true)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_position_lines),
                    subtitle = stringResource(R.string.pref_position_lines_sub),
                    checked = positionLines.value,
                    onClick = { viewModel.updatePositionLines(!positionLines.value) }
                )
                val autoEraseNotes = viewModel.autoEraseNotes.collectAsState(initial = false)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_auto_erase_notes),
                    checked = autoEraseNotes.value,
                    onClick = { viewModel.updateAutoEraseNotes(!autoEraseNotes.value) }
                )
                Divider(
                    modifier = Modifier.fillMaxWidth()
                )
                SettingsCategory(
                    title = stringResource(R.string.pref_other)
                )
                val keepScreenOn = viewModel.keepScreenOn.collectAsState(initial = false)
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_keep_screen_on),
                    checked = keepScreenOn.value,
                    onClick = {
                        viewModel.updateKeepScreenOn(!keepScreenOn.value)
                    }
                )
                val context = LocalContext.current
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
                if(viewModel.launchedFromGame == null || viewModel.launchedFromGame == false) {
                    PreferenceRow(
                        title = stringResource(R.string.pref_delete_stats),
                        onClick = {
                            viewModel.resetStatsDialog = true
                        }
                    )
                }
            }
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
    ){
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
                if(amoledBlack && (darkTheme == 0 && isSystemInDarkTheme() || darkTheme == 2)) {
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