package com.kaajjo.libresudoku.ui.game

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.game.components.DefaultGameKeyboard
import com.kaajjo.libresudoku.ui.game.components.ToolBardItem
import com.kaajjo.libresudoku.ui.game.components.ToolBoardItem
import com.kaajjo.libresudoku.ui.onboarding.FirstGameDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    navController: NavController,
    viewModel: GameViewModel
) {
    val firstGame by viewModel.firstGame.collectAsState(initial = false)
    if(firstGame) {
        viewModel.pauseTimer()
        FirstGameDialog(
            onFinished = {
                viewModel.setFirstGameFalse()
                viewModel.startTimer()
            }
        )
    }
    val keepScreenOn by viewModel.keepScreenOn.collectAsState(initial = false)
    if(keepScreenOn) {
        KeepScreenOn()
    }

    // so that the timer doesn't run in the background
    // https://stackoverflow.com/questions/66546962/jetpack-compose-how-do-i-refresh-a-screen-when-app-returns-to-foreground/66807899#66807899
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> { if(viewModel.gamePlaying) viewModel.startTimer() }
            Lifecycle.Event.ON_PAUSE -> {
                viewModel.pauseTimer()
                viewModel.currCell = Cell(-1, -1, 0)
            }
            Lifecycle.Event.ON_DESTROY -> {
                viewModel.pauseTimer()
            }
            else -> { }
        }
    }

    var restartButtonAngleState by remember { mutableStateOf(0f) }
    val restartButtonAnimation: Float by animateFloatAsState(
        targetValue = restartButtonAngleState,
        animationSpec = tween(durationMillis = 250)
    )
    LaunchedEffect(Unit) {
        if(!viewModel.endGame && !viewModel.gameCompleted) {
            viewModel.startTimer()
        }
    }
    val resetTimer by viewModel.resetTimerOnRestart.collectAsState(initial = true)
    if(viewModel.restartDialog) {
        viewModel.pauseTimer()
        AlertDialog(
            title = { Text(stringResource(R.string.action_reset_game)) },
            text = { Text(stringResource(R.string.reset_game_text))},
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
                    restartButtonAngleState += 360
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
    } else if(viewModel.giveUpDialog) {
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
    } else if(viewModel.gameCompleted) {
        AlertDialog(
            title = { Text(stringResource(R.string.game_completed)) },
            text = { Text(stringResource(R.string.game_completed_text)) },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.gameCompleted = false
                    viewModel.endGame = true
                }) {
                    Text(stringResource(R.string.action_stay))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.action_exit))
                }
            },
            onDismissRequest = {
                viewModel.gameCompleted = false
                viewModel.endGame = true
            }
        )
    } else if(viewModel.mistakesLimitDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.game_over)) },
            text = { Text(stringResource(R.string.game_over_mistakes)) },
            dismissButton = {
                TextButton(onClick = {
                    navController.popBackStack()
                }) {
                    Text(stringResource(R.string.action_exit))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.mistakesLimitDialog = false
                    viewModel.endGame = true
                }) {
                    Text(stringResource(R.string.action_stay))
                }
            },
            onDismissRequest = {
                viewModel.mistakesLimitDialog = false
                viewModel.endGame = true
            },
        )
    }
    LaunchedEffect(key1 = viewModel.gameCompleted) {
        if(viewModel.gameCompleted) {
            viewModel.onGameComplete()
        }
    }

    val showAnswerInteractionSource = remember { MutableInteractionSource() }
    val showAnswerPressed by showAnswerInteractionSource.collectIsPressedAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
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
                actions = {
                    AnimatedVisibility(visible = viewModel.endGame && (viewModel.mistakesCount >= 3 || viewModel.giveUp)) {
                        Row (
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                interactionSource = showAnswerInteractionSource,
                                onClick = { }
                            ) {
                                Text(stringResource(R.string.action_show_solution))
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
                                onDismiss = { viewModel.showMenu = false},
                                onGiveUpClick = {
                                    viewModel.pauseTimer()
                                    viewModel.giveUpDialog = true
                                },
                                onSettingsClick = {
                                    navController.navigate("settings/?fromGame=true")
                                    viewModel.showMenu = false
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(start = 12.dp, end = 12.dp, top = 48.dp)
        ) {
            val errorHighlight by viewModel.mistakesMethod.collectAsState(initial = 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                val context = LocalContext.current
                var difficultyText by remember { mutableStateOf("") }
                LaunchedEffect(key1 = viewModel.boardEntity) {
                    difficultyText = when(viewModel.boardEntity.difficulty) {
                        GameDifficulty.Unspecified -> context.getString(R.string.difficulty_unspecified)
                        GameDifficulty.Simple -> context.getString(R.string.difficulty_simple)
                        GameDifficulty.Easy -> context.getString(R.string.difficulty_easy)
                        GameDifficulty.Moderate -> context.getString(R.string.difficulty_moderate)
                        GameDifficulty.Hard -> context.getString(R.string.difficulty_hard)
                        GameDifficulty.Challenge -> context.getString(R.string.difficulty_challenge)
                        GameDifficulty.Custom -> context.getString(R.string.difficulty_custom)
                    }
                }
                TopBoardSection(text = difficultyText)

                val mistakesLimit = viewModel.mistakesLimit.collectAsState(initial = false)
                if(mistakesLimit.value && errorHighlight != 0) {
                    TopBoardSection(text = stringResource(R.string.mistakes_number_out_of, viewModel.mistakesCount, 3))
                }

                val timerEnabled = viewModel.timerEnabled.collectAsState(initial = false)
                if(timerEnabled.value) {
                    TopBoardSection(
                        text = viewModel.timeText
                    )
                }
            }

            var renderNotes by remember { mutableStateOf(true) }
            val inputMethod = viewModel.inputMethod.collectAsState(initial = 0)
            val configuration = LocalConfiguration.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(configuration.screenWidthDp.dp)
                    .padding(vertical = 12.dp)
            ) {
                val remainingUse by viewModel.remainingUse.collectAsState(initial = true)
                val highlightIdentical by viewModel.identicalHighlight.collectAsState(initial = true)
                val positionLines by viewModel.positionLines.collectAsState(initial = true)
                val boardBlur by animateDpAsState(targetValue = if(viewModel.gamePlaying || viewModel.endGame) 0.dp else 10.dp)
                val scale by animateFloatAsState(targetValue = if(viewModel.gamePlaying || viewModel.endGame) 1f else 0.90f)

                val fontSizeFactor by viewModel.fontSize.collectAsState(initial = 1)
                var fontSizeValue by remember { mutableStateOf(
                    viewModel.getFontSize(factor = fontSizeFactor)
                )
                }
                LaunchedEffect(key1 = fontSizeFactor) {
                    fontSizeValue = viewModel.getFontSize(factor = fontSizeFactor)
                }

                val localView = LocalView.current // vibration
                Board(
                    modifier = Modifier
                        .blur(boardBlur)
                        .scale(scale, scale),
                    board = if(!showAnswerPressed) viewModel.gameBoard else viewModel.solvedBoard,
                    size = viewModel.size,
                    mainTextSize = fontSizeValue,
                    notes = viewModel.notes,
                    selectedCell = viewModel.currCell,
                    onClick = { cell ->
                        viewModel.processInput(
                            inputMethod = inputMethod.value,
                            cell = cell,
                            remainingUse = remainingUse,
                        )
                    },
                    onLongClick = { cell ->
                        if(viewModel.processInput(
                                inputMethod = inputMethod.value,
                                cell = cell,
                                remainingUse = remainingUse,
                                longTap = true
                            )) {
                            localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    identicalNumbersHighlight = highlightIdentical,
                    errorsHighlight = errorHighlight != 0,
                    positionLines = positionLines,
                    enabled = viewModel.gamePlaying && !viewModel.endGame,
                    questions = !(viewModel.gamePlaying || viewModel.endGame) && Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
                    renderNotes = renderNotes && !showAnswerPressed
                )
            }

            AnimatedVisibility(visible = !viewModel.endGame) {
                val remainingUse = viewModel.remainingUse.collectAsState(initial = true)
                DefaultGameKeyboard(
                    size = viewModel.size,
                    remainingUses = if(remainingUse.value) viewModel.remainingUsesList else null,
                    onClick = {
                        viewModel.processInputKeyboard(
                            number = it,
                            inputMethod = inputMethod.value
                        )
                    },
                    onLongClick = {
                        viewModel.processInputKeyboard(
                            number = it,
                            inputMethod = inputMethod.value,
                            longTap = true
                        )
                    },
                    selected = viewModel.digitFirstNumber
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ToolBoardItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_round_undo_24),
                    onClick = { viewModel.toolBoardClick(ToolBardItem.Undo) }
                )
                val hintsDisabled = viewModel.disableHints.collectAsState(initial = false)
                if (!hintsDisabled.value) {
                    ToolBoardItem(
                        modifier = Modifier.weight(1f),
                        painter = painterResource(R.drawable.ic_lightbulb_stars_24),
                        onClick = { viewModel.toolBoardClick(ToolBardItem.Hint) }
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    val localView = LocalView.current
                    NotesMenu(
                        expanded = viewModel.showNotesMenu,
                        onDismiss = { viewModel.showNotesMenu = false },
                        onComputeNotesClick = { viewModel.computeNotes() },
                        onClearNotesClick = { viewModel.clearNotes() },
                        renderNotes = renderNotes,
                        onRenderNotesClick = { renderNotes = !renderNotes }
                    )
                    ToolBoardItem(
                        painter = painterResource(R.drawable.ic_round_edit_24),
                        toggled = viewModel.notesToggled,
                        onClick = { viewModel.toolBoardClick(ToolBardItem.Note) },
                        onLongClick = {
                            if(viewModel.gamePlaying) {
                                localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                viewModel.showNotesMenu = true
                            }
                        }
                    )

                }
                ToolBoardItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_eraser_24),
                    onClick = {
                        viewModel.toolBoardClick(ToolBardItem.Remove)
                    }
                )
            }
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
                        Checkbox(checked = renderNotes, onCheckedChange = { onRenderNotesClick() } )
                    }
                },
                onClick = onRenderNotesClick
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
                text = { Text(stringResource(R.string.settings)) },
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
    modifier: Modifier = Modifier,
    text: String
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ){
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