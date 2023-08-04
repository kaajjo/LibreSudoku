package com.kaajjo.libresudoku.ui.game

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.game.components.DefaultGameKeyboard
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import com.kaajjo.libresudoku.ui.game.components.ToolbarItem
import com.kaajjo.libresudoku.ui.onboarding.FirstGameDialog
import com.kaajjo.libresudoku.ui.util.ReverseArrangement

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun GameScreen(
    navigateBack: () -> Unit,
    navigateSettings: () -> Unit,
    viewModel: GameViewModel
) {
    val localView = LocalView.current // vibration

    val firstGame by viewModel.firstGame.collectAsStateWithLifecycle(initialValue = false)
    if (firstGame) {
        viewModel.pauseTimer()
        FirstGameDialog(
            onFinished = {
                viewModel.setFirstGameFalse()
                viewModel.startTimer()
            }
        )
    }

    var restartButtonAngleState by remember { mutableFloatStateOf(0f) }
    val restartButtonAnimation: Float by animateFloatAsState(
        targetValue = restartButtonAngleState,
        animationSpec = tween(durationMillis = 250)
    )

    LaunchedEffect(Unit) {
        if (!viewModel.endGame && !viewModel.gameCompleted) {
            viewModel.startTimer()
        }
    }

    val resetTimer by viewModel.resetTimerOnRestart.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_GAME_RESET_TIMER)

    LaunchedEffect(viewModel.gameCompleted) {
        if (viewModel.gameCompleted) {
            viewModel.onGameComplete()
            viewModel.endGame = true
        }
    }


    val mistakesLimit by viewModel.mistakesLimit.collectAsStateWithLifecycle(
        initialValue = PreferencesConstants.DEFAULT_MISTAKES_LIMIT
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    AnimatedVisibility(visible = viewModel.endGame && (viewModel.mistakesCount >= PreferencesConstants.MISTAKES_LIMIT || viewModel.giveUp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.showSolution = !viewModel.showSolution }
                            ) {
                                AnimatedContent(
                                    if (viewModel.showSolution) stringResource(R.string.action_show_mine_sudoku)
                                    else stringResource(R.string.action_show_solution)
                                ) {
                                    Text(it)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = !viewModel.endGame) {
                        val rotationAngle by animateFloatAsState(
                            targetValue = if (viewModel.gamePlaying) 0f else 360f
                        )
                        IconButton(onClick = {
                            if (!viewModel.gamePlaying) viewModel.startTimer() else viewModel.pauseTimer()
                            viewModel.currCell = Cell(-1, -1, 0)
                        }) {
                            Icon(
                                modifier = Modifier.rotate(rotationAngle),
                                painter = painterResource(
                                    if (viewModel.gamePlaying) {
                                        R.drawable.ic_round_pause_24
                                    } else {
                                        R.drawable.ic_round_play_24
                                    }
                                ),
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = !viewModel.endGame) {
                        IconButton(onClick = { viewModel.restartDialog = true }) {
                            Icon(
                                modifier = Modifier.rotate(restartButtonAnimation),
                                painter = painterResource(R.drawable.ic_round_replay_24),
                                contentDescription = null
                            )
                        }
                    }
                    AnimatedVisibility(visible = !viewModel.endGame) {
                        Box {
                            IconButton(onClick = { viewModel.showMenu = !viewModel.showMenu }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null
                                )
                            }
                            GameMenu(
                                expanded = viewModel.showMenu,
                                onDismiss = { viewModel.showMenu = false },
                                onGiveUpClick = {
                                    viewModel.pauseTimer()
                                    viewModel.giveUpDialog = true
                                },
                                onSettingsClick = {
                                    navigateSettings()
                                    viewModel.showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { scaffoldPaddings ->
        Column(
            modifier = Modifier
                .padding(scaffoldPaddings)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            val errorHighlight by viewModel.mistakesMethod.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES)
            AnimatedVisibility(visible = !viewModel.endGame) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TopBoardSection(stringResource(viewModel.gameDifficulty.resName))

                    if (mistakesLimit && errorHighlight != 0) {
                        TopBoardSection(
                            stringResource(
                                R.string.mistakes_number_out_of,
                                viewModel.mistakesCount,
                                3
                            )
                        )
                    }

                    val timerEnabled by viewModel.timerEnabled.collectAsStateWithLifecycle(
                        initialValue = PreferencesConstants.DEFAULT_SHOW_TIMER
                    )
                    AnimatedVisibility(visible = timerEnabled || viewModel.endGame) {
                        TopBoardSection(viewModel.timeText)
                    }
                }
            }

            var renderNotes by remember { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                val remainingUse by viewModel.remainingUse.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_REMAINING_USES)
                val highlightIdentical by viewModel.identicalHighlight.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_HIGHLIGHT_IDENTICAL
                )
                val positionLines by viewModel.positionLines.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_POSITION_LINES
                )
                val boardBlur by animateDpAsState(targetValue = if (viewModel.gamePlaying || viewModel.endGame) 0.dp else 10.dp)
                val scale by animateFloatAsState(targetValue = if (viewModel.gamePlaying || viewModel.endGame) 1f else 0.90f)
                val crossHighlight by viewModel.crossHighlight.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT
                )

                val fontSizeFactor by viewModel.fontSize.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR)
                val fontSizeValue by remember(fontSizeFactor, viewModel.gameType) {
                    mutableStateOf(
                        viewModel.getFontSize(factor = fontSizeFactor)
                    )
                }

                Board(
                    modifier = Modifier
                        .blur(boardBlur)
                        .scale(scale, scale),
                    board = if (!viewModel.showSolution) viewModel.gameBoard else viewModel.solvedBoard,
                    size = viewModel.size,
                    mainTextSize = fontSizeValue,
                    notes = viewModel.notes,
                    selectedCell = viewModel.currCell,
                    onClick = { cell ->
                        viewModel.processInput(
                            cell = cell,
                            remainingUse = remainingUse,
                        )
                    },
                    onLongClick = { cell ->
                        if (viewModel.processInput(cell, remainingUse, longTap = true)) {
                            localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    identicalNumbersHighlight = highlightIdentical,
                    errorsHighlight = errorHighlight != 0,
                    positionLines = positionLines,
                    enabled = viewModel.gamePlaying && !viewModel.endGame,
                    questions = !(viewModel.gamePlaying || viewModel.endGame) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
                    renderNotes = renderNotes && !viewModel.showSolution,
                    zoomable = viewModel.gameType == GameType.Default12x12,
                    boardColors = LocalBoardColors.current,
                    crossHighlight = crossHighlight
                )
            }

            val funKeyboardOverNum by viewModel.funKeyboardOverNum.collectAsStateWithLifecycle(
                initialValue = PreferencesConstants.DEFAULT_FUN_KEYBOARD_OVER_NUM
            )

            AnimatedContent(!viewModel.endGame) { contentState ->
                if (contentState) {
                    Column(
                        verticalArrangement = if (funKeyboardOverNum) ReverseArrangement else Arrangement.Top
                    ) {
                        val remainingUse by viewModel.remainingUse.collectAsStateWithLifecycle(
                            initialValue = PreferencesConstants.DEFAULT_REMAINING_USES
                        )
                        DefaultGameKeyboard(
                            size = viewModel.size,
                            remainingUses = if (remainingUse) viewModel.remainingUsesList else null,
                            onClick = {
                                viewModel.processInputKeyboard(number = it)
                            },
                            onLongClick = {
                                viewModel.processInputKeyboard(
                                    number = it,
                                    longTap = true
                                )
                            },
                            selected = viewModel.digitFirstNumber
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                UndoRedoMenu(
                                    expanded = viewModel.showUndoRedoMenu,
                                    onDismiss = { viewModel.showUndoRedoMenu = false },
                                    onRedoClick = { viewModel.toolbarClick(ToolBarItem.Redo) }
                                )
                                ToolbarItem(
                                    painter = painterResource(R.drawable.ic_round_undo_24),
                                    onClick = { viewModel.toolbarClick(ToolBarItem.Undo) },
                                    onLongClick = { viewModel.showUndoRedoMenu = true }
                                )

                            }
                            val hintsDisabled by viewModel.disableHints.collectAsStateWithLifecycle(
                                initialValue = PreferencesConstants.DEFAULT_HINTS_DISABLED
                            )
                            if (!hintsDisabled) {
                                ToolbarItem(
                                    modifier = Modifier.weight(1f),
                                    painter = painterResource(R.drawable.ic_lightbulb_stars_24),
                                    onClick = { viewModel.toolbarClick(ToolBarItem.Hint) }
                                )
                            }

                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                NotesMenu(
                                    expanded = viewModel.showNotesMenu,
                                    onDismiss = { viewModel.showNotesMenu = false },
                                    onComputeNotesClick = { viewModel.computeNotes() },
                                    onClearNotesClick = { viewModel.clearNotes() },
                                    renderNotes = renderNotes,
                                    onRenderNotesClick = { renderNotes = !renderNotes }
                                )
                                ToolbarItem(
                                    painter = painterResource(R.drawable.ic_round_edit_24),
                                    toggled = viewModel.notesToggled,
                                    onClick = { viewModel.toolbarClick(ToolBarItem.Note) },
                                    onLongClick = {
                                        if (viewModel.gamePlaying) {
                                            localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                            viewModel.showNotesMenu = true
                                        }
                                    }
                                )

                            }
                            ToolbarItem(
                                modifier = Modifier.weight(1f),
                                painter = painterResource(R.drawable.ic_eraser_24),
                                toggled = viewModel.eraseButtonToggled,
                                onClick = {
                                    viewModel.toolbarClick(ToolBarItem.Remove)
                                },
                                onLongClick = {
                                    if (viewModel.gamePlaying) {
                                        localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        viewModel.toggleEraseButton()
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // Game completed section

                    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle(
                        initialValue = emptyList()
                    )

                    AfterGameStats(
                        modifier = Modifier.fillMaxWidth(),
                        difficulty = viewModel.gameDifficulty,
                        type = viewModel.gameType,
                        hintsUsed = viewModel.hintsUsed,
                        mistakesMade = viewModel.mistakesMade,
                        mistakesLimit = mistakesLimit,
                        mistakesLimitCount = viewModel.mistakesCount,
                        giveUp = viewModel.giveUp,
                        notesTaken = viewModel.notesTaken,
                        records = allRecords,
                        timeText = viewModel.timeText
                    )
                }
            }
        }
    }

    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_KEEP_SCREEN_ON)
    if (keepScreenOn) {
        KeepScreenOn()
    }

    // dialogs
    if (viewModel.restartDialog) {
        viewModel.pauseTimer()
        AlertDialog(
            title = { Text(stringResource(R.string.action_reset_game)) },
            text = { Text(stringResource(R.string.reset_game_text)) },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.restartDialog = false
                    viewModel.startTimer()
                }) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    restartButtonAngleState -= 360
                    viewModel.resetGame(resetTimer)
                    viewModel.restartDialog = false
                    viewModel.startTimer()
                }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            onDismissRequest = {
                viewModel.restartDialog = false
                viewModel.startTimer()
            }
        )
    } else if (viewModel.giveUpDialog) {
        viewModel.pauseTimer()
        AlertDialog(
            title = { Text(stringResource(R.string.action_give_up)) },
            text = { Text(stringResource(R.string.give_up_text)) },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.giveUpDialog = false
                    viewModel.startTimer()
                }) {
                    Text(stringResource(R.string.dialog_no))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.giveUp()
                    viewModel.giveUpDialog = false
                    viewModel.pauseTimer()
                }) {
                    Text(stringResource(R.string.dialog_yes))
                }
            },
            onDismissRequest = {
                viewModel.giveUpDialog = false
                viewModel.startTimer()
            },
        )
    }

    LaunchedEffect(viewModel.mistakesMethod) {
        viewModel.checkMistakesAll()
    }

    // so that the timer doesn't run in the background
    // https://stackoverflow.com/questions/66546962/jetpack-compose-how-do-i-refresh-a-screen-when-app-returns-to-foreground/66807899#66807899
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (viewModel.gamePlaying) viewModel.startTimer()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.pauseTimer()
                viewModel.currCell = Cell(-1, -1, 0)
            }

            Lifecycle.Event.ON_DESTROY -> viewModel.pauseTimer()
            else -> {}
        }
    }
}

@Composable
fun NotesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onComputeNotesClick: () -> Unit,
    onClearNotesClick: () -> Unit,
    renderNotes: Boolean,
    onRenderNotesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_compute_notes)) },
                onClick = {
                    onComputeNotesClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.action_clear_notes))
                },
                onClick = {
                    onClearNotesClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.action_show_notes))
                        Checkbox(checked = renderNotes, onCheckedChange = { onRenderNotesClick() })
                    }
                },
                onClick = onRenderNotesClick
            )
        }
    }
}

@Composable
fun UndoRedoMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.redo)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Redo,
                        contentDescription = null
                    )
                },
                onClick = {
                    onRedoClick()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun GameMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onGiveUpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_give_up)) },
                onClick = {
                    onGiveUpClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_title)) },
                onClick = {
                    onSettingsClick()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun TopBoardSection(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
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
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

@Composable
fun KeepScreenOn() = AndroidView({ View(it).apply { keepScreenOn = true } })