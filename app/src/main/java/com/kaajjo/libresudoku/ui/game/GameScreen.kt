package com.kaajjo.libresudoku.ui.game

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHintData
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.destinations.SettingsAdvancedHintScreenDestination
import com.kaajjo.libresudoku.destinations.SettingsCategoriesScreenDestination
import com.kaajjo.libresudoku.ui.components.AdvancedHintContainer
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.game.components.DefaultGameKeyboard
import com.kaajjo.libresudoku.ui.game.components.GameMenu
import com.kaajjo.libresudoku.ui.game.components.NotesMenu
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import com.kaajjo.libresudoku.ui.game.components.ToolbarItem
import com.kaajjo.libresudoku.ui.game.components.UndoRedoMenu
import com.kaajjo.libresudoku.ui.game.models.GameUiEvent
import com.kaajjo.libresudoku.ui.game.models.GameUiSideEffect
import com.kaajjo.libresudoku.ui.game.models.GameUiState
import com.kaajjo.libresudoku.ui.onboarding.FirstGameDialog
import com.kaajjo.libresudoku.ui.util.ReverseArrangement
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(
    style = AnimatedNavigation::class,
    navArgsDelegate = GameScreenNavArgs::class
)
@Composable
fun GameScreen(
    viewModel: GameViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var restartButtonAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(viewModel, lifecycleOwner, navigator, clipboardManager, context, localView) {
        viewModel.effect
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
                when (effect) {
                    GameUiSideEffect.NavigateBack -> navigator.popBackStack()
                    GameUiSideEffect.OpenSettings -> navigator.navigate(
                        SettingsCategoriesScreenDestination(launchedFromGame = true)
                    )
                    GameUiSideEffect.OpenHintSettings -> navigator.navigate(
                        SettingsAdvancedHintScreenDestination
                    )
                    is GameUiSideEffect.CopyBoard -> {
                        clipboardManager.setText(AnnotatedString(effect.board))
                        if (SDK_INT < 33) {
                            Toast.makeText(
                                context,
                                R.string.export_string_state_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    GameUiSideEffect.HapticFeedback -> {
                        localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    }
                    GameUiSideEffect.GameRestarted -> restartButtonAngle -= 360f
                    GameUiSideEffect.SaveFailed -> Toast.makeText(
                        context,
                        R.string.game_save_error,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> viewModel.sendEvent(GameUiEvent.ScreenResumed)
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_DESTROY -> {
                viewModel.sendEvent(GameUiEvent.ScreenPaused)
            }
            else -> Unit
        }
    }
    DisposableEffect(viewModel) {
        onDispose { viewModel.sendEvent(GameUiEvent.ScreenPaused) }
    }

    if (uiState.settings.keepScreenOn) {
        KeepScreenOn()
    }

    BackHandler { viewModel.sendEvent(GameUiEvent.NavigateBack) }

    GameScreenContent(
        uiState = uiState,
        onEvent = viewModel::sendEvent,
        restartButtonAngle = restartButtonAngle
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameScreenContent(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    restartButtonAngle: Float
) {
    val settings = uiState.settings
    val sudokuUtils = remember { SudokuUtils() }
    val fontSize = remember(settings.fontSize, uiState.gameType) {
        sudokuUtils.getFontSize(uiState.gameType, settings.fontSize)
    }
    val restartButtonAnimation by animateFloatAsState(
        targetValue = restartButtonAngle,
        animationSpec = tween(durationMillis = 250),
        label = "restartButtonAnimation"
    )
    val boardBlur by animateDpAsState(
        targetValue = if (uiState.gamePlaying || uiState.endGame) 0.dp else 10.dp,
        label = "Game board blur"
    )
    val boardScale by animateFloatAsState(
        targetValue = if (uiState.gamePlaying || uiState.endGame) 1f else 0.90f,
        label = "Game board scale"
    )

    BackHandler(enabled = uiState.advancedHintMode) {
        onEvent(GameUiEvent.CancelAdvancedHint)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { onEvent(GameUiEvent.NavigateBack) }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = stringResource(R.string.nav_back)
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.loadError) {
                        AnimatedVisibility(
                            visible = uiState.endGame &&
                                    (uiState.mistakesCount >= PreferencesConstants.MISTAKES_LIMIT || uiState.giveUp)
                        ) {
                            FilledTonalButton(onClick = { onEvent(GameUiEvent.ToggleSolution) }) {
                                AnimatedContent(
                                    targetState = uiState.showSolution,
                                    label = "Show solution/mine button"
                                ) { showSolution ->
                                    Text(
                                        stringResource(
                                            if (showSolution) R.string.action_show_mine_sudoku
                                            else R.string.action_show_solution
                                        )
                                    )
                                }
                            }
                        }
                        AnimatedVisibility(visible = !uiState.endGame) {
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (uiState.gamePlaying) 0f else 360f,
                                label = "Play/Pause game icon rotation"
                            )
                            IconButton(onClick = { onEvent(GameUiEvent.TogglePause) }) {
                                Icon(
                                    modifier = Modifier.rotate(rotationAngle),
                                    painter = painterResource(
                                        if (uiState.gamePlaying) R.drawable.ic_round_pause_24
                                        else R.drawable.ic_round_play_24
                                    ),
                                    contentDescription = null
                                )
                            }
                        }
                        AnimatedVisibility(visible = !uiState.endGame) {
                            IconButton(onClick = { onEvent(GameUiEvent.ShowRestartDialog) }) {
                                Icon(
                                    modifier = Modifier.rotate(restartButtonAnimation),
                                    painter = painterResource(R.drawable.ic_round_replay_24),
                                    contentDescription = null
                                )
                            }
                        }
                        AnimatedVisibility(visible = !uiState.endGame) {
                            Box {
                                IconButton(onClick = {
                                    onEvent(GameUiEvent.SetMenuVisible(!uiState.showMenu))
                                }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = null)
                                }
                                GameMenu(
                                    expanded = uiState.showMenu,
                                    onDismiss = { onEvent(GameUiEvent.SetMenuVisible(false)) },
                                    onGiveUpClick = { onEvent(GameUiEvent.ShowGiveUpDialog) },
                                    onSettingsClick = { onEvent(GameUiEvent.OpenSettings) },
                                    onExportClick = { onEvent(GameUiEvent.ExportBoard) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { scaffoldPaddings ->
        if (uiState.isLoading || uiState.loadError) {
            Box(
                modifier = Modifier.fillMaxSize().padding(scaffoldPaddings),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(stringResource(R.string.game_load_error))
                        TextButton(onClick = { onEvent(GameUiEvent.RetryLoading) }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.padding(scaffoldPaddings).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            AnimatedVisibility(visible = !uiState.endGame) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TopBoardSection(stringResource(uiState.gameDifficulty.resName))
                    if (settings.mistakesLimit && settings.mistakesMethod != 0) {
                        TopBoardSection(
                            stringResource(
                                R.string.mistakes_number_out_of,
                                uiState.mistakesCount,
                                PreferencesConstants.MISTAKES_LIMIT
                            )
                        )
                    }
                    AnimatedVisibility(visible = settings.timerEnabled || uiState.endGame) {
                        TopBoardSection(uiState.timeText)
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    AnimatedVisibility(
                        visible = !uiState.gamePlaying && !uiState.endGame,
                        enter = expandVertically(clip = false) + fadeIn(),
                        exit = shrinkVertically(clip = false) + fadeOut()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).shadow(12.dp)
                        )
                    }
                }
                Board(
                    modifier = Modifier.blur(boardBlur).scale(boardScale, boardScale),
                    board = if (uiState.showSolution) uiState.solvedBoard else uiState.gameBoard,
                    size = uiState.size,
                    mainTextSize = fontSize,
                    autoFontSize = settings.fontSize == 0,
                    notes = uiState.notes,
                    selectedCell = uiState.currCell,
                    onClick = { cell ->
                        onEvent(GameUiEvent.CellTapped(cell.row, cell.col))
                    },
                    onLongClick = { cell ->
                        onEvent(GameUiEvent.CellTapped(cell.row, cell.col, longTap = true))
                    },
                    identicalNumbersHighlight = settings.identicalHighlight,
                    errorsHighlight = settings.mistakesMethod != 0,
                    positionLines = settings.positionLines,
                    notesToHighlight = if (uiState.digitFirstNumber > 0) {
                        uiState.notes.filter { it.value == uiState.digitFirstNumber }
                    } else {
                        emptyList()
                    },
                    enabled = uiState.gamePlaying && !uiState.endGame,
                    questions = !(uiState.gamePlaying || uiState.endGame) && SDK_INT < Build.VERSION_CODES.R,
                    renderNotes = uiState.renderNotes && !uiState.showSolution,
                    zoomable = uiState.gameType == GameType.Default12x12 || uiState.gameType == GameType.Killer12x12,
                    crossHighlight = settings.crossHighlight,
                    cages = uiState.cages,
                    cellsToHighlight = uiState.advancedHintData
                        ?.takeIf { uiState.advancedHintMode }
                        ?.let { it.helpCells + it.targetCell }
                )
            }

            AnimatedContent(uiState.advancedHintMode, label = "Advanced hint mode") { hintMode ->
                if (hintMode) {
                    GameHintContent(uiState, onEvent)
                } else {
                    AnimatedContent(!uiState.endGame, label = "Game controls") { playing ->
                        if (playing) {
                            GameControls(uiState, onEvent)
                        } else {
                            AfterGameStats(
                                modifier = Modifier.fillMaxWidth(),
                                difficulty = uiState.gameDifficulty,
                                type = uiState.gameType,
                                hintsUsed = uiState.hintsUsed,
                                mistakesMade = uiState.mistakesMade,
                                mistakesLimit = settings.mistakesLimit,
                                mistakesLimitCount = uiState.mistakesCount,
                                giveUp = uiState.giveUp,
                                notesTaken = uiState.notesTaken,
                                records = uiState.allRecords,
                                timeText = uiState.timeText
                            )
                        }
                    }
                }
            }
        }
    }

    if (!uiState.isLoading && !uiState.loadError && settings.firstGame) {
        FirstGameDialog(onFinished = { onEvent(GameUiEvent.FirstGameFinished) })
    }
    if (uiState.restartDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.action_reset_game)) },
            text = { Text(stringResource(R.string.reset_game_text)) },
            dismissButton = {
                TextButton(onClick = { onEvent(GameUiEvent.DismissRestartDialog) }) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(GameUiEvent.ConfirmRestart) }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            onDismissRequest = { onEvent(GameUiEvent.DismissRestartDialog) }
        )
    } else if (uiState.giveUpDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.action_give_up)) },
            text = { Text(stringResource(R.string.give_up_text)) },
            dismissButton = {
                TextButton(onClick = { onEvent(GameUiEvent.DismissGiveUpDialog) }) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
            confirmButton = {
                TextButton(onClick = { onEvent(GameUiEvent.ConfirmGiveUp) }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            onDismissRequest = { onEvent(GameUiEvent.DismissGiveUpDialog) }
        )
    }
}

@Composable
private fun GameHintContent(uiState: GameUiState, onEvent: (GameUiEvent) -> Unit) {
    if (uiState.advancedHintLoading) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator()
            TextButton(onClick = { onEvent(GameUiEvent.CancelAdvancedHint) }) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    } else {
        val hintData = uiState.advancedHintData
        AdvancedHintContainer(
            advancedHintData = hintData ?: AdvancedHintData(
                titleRes = R.string.advanced_hint_no_hint_title,
                textResWithArg = R.string.advanced_hint_no_hint to emptyList(),
                targetCell = Cell(-1, -1),
                helpCells = emptyList()
            ),
            onApplyClick = if (hintData != null) {
                { onEvent(GameUiEvent.ApplyAdvancedHint) }
            } else {
                null
            },
            onBackClick = { onEvent(GameUiEvent.CancelAdvancedHint) },
            onSettingsClick = { onEvent(GameUiEvent.OpenHintSettings) }
        )
    }
}

@Composable
private fun GameControls(uiState: GameUiState, onEvent: (GameUiEvent) -> Unit) {
    Column(
        verticalArrangement = if (uiState.settings.funKeyboardOverNum) ReverseArrangement else Arrangement.Top
    ) {
        DefaultGameKeyboard(
            size = uiState.size,
            remainingUses = uiState.remainingUsesList.takeIf { uiState.settings.remainingUse },
            onClick = { onEvent(GameUiEvent.NumberTapped(it)) },
            onLongClick = { onEvent(GameUiEvent.NumberTapped(it, longTap = true)) },
            selected = uiState.digitFirstNumber
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                UndoRedoMenu(
                    expanded = uiState.showUndoRedoMenu,
                    onDismiss = { onEvent(GameUiEvent.SetUndoRedoMenuVisible(false)) },
                    onRedoClick = { onEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Redo)) }
                )
                ToolbarItem(
                    painter = painterResource(R.drawable.ic_round_undo_24),
                    onClick = { onEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Undo)) },
                    onLongClick = { onEvent(GameUiEvent.SetUndoRedoMenuVisible(true)) }
                )
            }
            if (!uiState.settings.disableHints) {
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_lightbulb_stars_24),
                    onClick = { onEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Hint)) }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NotesMenu(
                    expanded = uiState.showNotesMenu,
                    onDismiss = { onEvent(GameUiEvent.SetNotesMenuVisible(false)) },
                    onComputeNotesClick = { onEvent(GameUiEvent.ComputeNotes) },
                    onClearNotesClick = { onEvent(GameUiEvent.ClearNotes) },
                    renderNotes = uiState.renderNotes,
                    onRenderNotesClick = { onEvent(GameUiEvent.ToggleRenderNotes) }
                )
                ToolbarItem(
                    painter = painterResource(R.drawable.ic_round_edit_24),
                    toggled = uiState.notesToggled,
                    onClick = { onEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Note)) },
                    onLongClick = { onEvent(GameUiEvent.SetNotesMenuVisible(true)) }
                )
            }
            ToolbarItem(
                modifier = Modifier.weight(1f),
                painter = painterResource(R.drawable.ic_eraser_24),
                toggled = uiState.eraseButtonToggled,
                onClick = { onEvent(GameUiEvent.ToolbarClicked(ToolBarItem.Remove)) },
                onLongClick = { onEvent(GameUiEvent.ToggleEraseMode) }
            )
            if (uiState.settings.advancedHintEnabled) {
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = rememberVectorPainter(Icons.Rounded.AutoAwesome),
                    onClick = { onEvent(GameUiEvent.RequestAdvancedHint) }
                )
            }
        }
    }
}

@Composable
fun TopBoardSection(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}

@Composable
fun KeepScreenOn() = AndroidView({ View(it).apply { keepScreenOn = true } })
