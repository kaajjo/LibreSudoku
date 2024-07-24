package com.kaajjo.libresudoku.ui.settings.gameplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SwitchAccessShortcut
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.settings.SelectionDialog
import com.kaajjo.libresudoku.ui.settings.SettingsScaffoldLazyColumn
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun SettingsGameplayScreen(
    viewModel: SettingsGameplayViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    var inputMethodDialog by rememberSaveable { mutableStateOf(false) }

    val inputMethod by viewModel.inputMethod.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_INPUT_METHOD)
    val mistakesLimit by viewModel.mistakesLimit.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_MISTAKES_LIMIT)
    val hintDisabled by viewModel.disableHints.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_HINTS_DISABLED)
    val timerEnabled by viewModel.timer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_SHOW_TIMER)
    val resetTimer by viewModel.canResetTimer.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_GAME_RESET_TIMER)
    val funKeyboardOverNum by viewModel.funKeyboardOverNum.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM
    )

    SettingsScaffoldLazyColumn(
        titleText = stringResource(R.string.pref_gameplay),
        navigator = navigator
    ) {  paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            item {
                PreferenceRow(
                    title = stringResource(R.string.pref_input),
                    subtitle = when (inputMethod) {
                        0 -> stringResource(R.string.pref_input_cell_first)
                        1 -> stringResource(R.string.pref_input_digit_first)
                        else -> ""
                    },
                    onClick = { inputMethodDialog = true },
                    painter = rememberVectorPainter(Icons.Outlined.EditNote)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_mistakes_limit),
                    subtitle = stringResource(R.string.pref_mistakes_limit_summ),
                    checked = mistakesLimit,
                    onClick = { viewModel.updateMistakesLimit(!mistakesLimit) },
                    painter = rememberVectorPainter(Icons.Outlined.Block)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_disable_hints),
                    subtitle = stringResource(R.string.pref_disable_hints_summ),
                    checked = hintDisabled,
                    onClick = { viewModel.updateHintDisabled(!hintDisabled) },
                    painter = rememberVectorPainter(Icons.Outlined.VisibilityOff)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_show_timer),
                    checked = timerEnabled,
                    onClick = { viewModel.updateTimer(!timerEnabled) },
                    painter = rememberVectorPainter(Icons.Outlined.Schedule)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_reset_timer),
                    checked = resetTimer,
                    onClick = { viewModel.updateCanResetTimer(!resetTimer) },
                    painter = rememberVectorPainter(Icons.Outlined.HistoryToggleOff)
                )
            }

            item {
                PreferenceRowSwitch(
                    title = stringResource(R.string.pref_fun_keyboard_over_num),
                    subtitle = stringResource(R.string.pref_fun_keyboard_over_num_subtitle),
                    checked = funKeyboardOverNum,
                    onClick = {
                        viewModel.updateFunKeyboardOverNum(!funKeyboardOverNum)
                    },
                    painter = rememberVectorPainter(Icons.Outlined.SwitchAccessShortcut)
                )
            }
        }

        if (inputMethodDialog) {
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
                onDismiss = { inputMethodDialog = false }
            )
        }
    }
}