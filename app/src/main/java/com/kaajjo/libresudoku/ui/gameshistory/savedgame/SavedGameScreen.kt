package com.kaajjo.libresudoku.ui.gameshistory.savedgame

import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.EmptyScreen
import com.kaajjo.libresudoku.ui.components.board.Board
import kotlinx.coroutines.launch
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun SavedGameScreen(
    navigateBack: () -> Unit,
    navigatePlayGame: (Long) -> Unit,
    navigateToFolder: (Long) -> Unit,
    viewModel: SavedGameViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.game_id, viewModel.boardUid ?: -1)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.export_string_title))
                                    },
                                    onClick = {
                                        viewModel.exportDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        LaunchedEffect(key1 = Unit) {
            viewModel.updateGame()
        }
        if (viewModel.savedGame != null && viewModel.boardEntity != null &&
            viewModel.parsedCurrentBoard.isNotEmpty() && viewModel.parsedInitialBoard.isNotEmpty()
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                val fontSizeFactor by viewModel.fontSize.collectAsState(initial = PreferencesConstants.DEFAULT_FONT_SIZE_FACTOR)
                var fontSizeValue by remember {
                    mutableStateOf(
                        viewModel.getFontSize(factor = fontSizeFactor)
                    )
                }
                LaunchedEffect(fontSizeFactor) {
                    fontSizeValue = viewModel.getFontSize(fontSizeFactor)
                }
                val pagerState = rememberPagerState()
                val pages = listOf(
                    stringResource(R.string.saved_game_current),
                    stringResource(R.string.saved_game_initial)
                )
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    pages.forEachIndexed { index, title ->
                        val coroutineScope = rememberCoroutineScope()
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index, 0f)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
                val boardScale = remember { Animatable(0.3f) }
                LaunchedEffect(key1 = Unit) {
                    boardScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = LinearOutSlowInEasing
                        )
                    )
                }
                val boardModifier = Modifier
                    .padding(10.dp)
                    .scale(boardScale.value)
                Column {
                    HorizontalPager(
                        state = pagerState,
                        count = 2,
                        modifier = Modifier
                            .wrapContentHeight()
                            .padding(top = 8.dp)
                    ) { page ->
                        when (page) {
                            0 -> Board(
                                board = viewModel.parsedCurrentBoard,
                                notes = viewModel.notes,
                                modifier = boardModifier,
                                mainTextSize = fontSizeValue,
                                selectedCell = Cell(-1, -1),
                                onClick = { }
                            )

                            1 -> Board(
                                board = viewModel.parsedInitialBoard,
                                modifier = boardModifier,
                                mainTextSize = fontSizeValue,
                                selectedCell = Cell(-1, -1),
                                onClick = { }
                            )
                        }
                    }
                }


                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .fillMaxWidth()
                ) {
                    var progress by remember { mutableStateOf(viewModel.getProgressFilled()) }

                    LaunchedEffect(viewModel.parsedInitialBoard) {
                        progress = viewModel.getProgressFilled()
                        viewModel.isSolved()
                    }

                    val gameFolder by viewModel.gameFolder.collectAsStateWithLifecycle()
                    gameFolder?.let {
                        AssistChip(
                            leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                            onClick = { navigateToFolder(it.uid) },
                            label = { Text(it.name) }
                        )
                    }

                    val textStyle = MaterialTheme.typography.bodyLarge
                    Text(
                        text = stringResource(
                            R.string.saved_game_progress_percentage,
                            progress
                        ),
                        style = textStyle
                    )
                    Text(
                        text = viewModel.savedGame?.let {
                            when {
                                it.mistakes >= 3 -> stringResource(R.string.saved_game_mistakes_limit)
                                it.giveUp -> stringResource(R.string.saved_game_give_up)
                                it.completed && !it.canContinue -> stringResource(R.string.saved_game_completed)
                                else -> stringResource(R.string.saved_game_in_progress)
                            }
                        } ?: ""
                    )
                    Text(
                        text = stringResource(
                            R.string.saved_game_difficulty,
                            stringResource(viewModel.boardEntity!!.difficulty.resName)
                        ),
                        style = textStyle
                    )
                    Text(
                        text = stringResource(
                            R.string.saved_game_type,
                            stringResource(viewModel.boardEntity!!.type.resName)
                        ),
                        style = textStyle
                    )
                    Text(
                        text = stringResource(R.string.saved_game_time,
                            viewModel.savedGame!!.timer.toKotlinDuration()
                                .toComponents { minutes, seconds, _ ->
                                    String.format(" %02d:%02d", minutes, seconds)
                                }
                        )
                    )

                    if (viewModel.savedGame!!.canContinue) {
                        FilledTonalButton(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = { navigatePlayGame(viewModel.savedGame!!.uid) }
                        ) {
                            Text(stringResource(R.string.action_continue))
                        }
                    }
                }
            }
        } else {
            EmptyScreen(stringResource(R.string.empty_screen_something_went_wrong))
        }

        if (viewModel.exportDialog) {
            val context = LocalContext.current
            val clipboardManager = LocalClipboardManager.current
            viewModel.boardEntity?.let {
                ExportDialog(
                    onDismiss = { viewModel.exportDialog = false },
                    boardString = it.initialBoard
                        .replace('0', '.')
                        .uppercase(),
                    onClickCopy = {
                        clipboardManager.setText(
                            AnnotatedString(
                                it.initialBoard
                                    .replace('0', '.')
                                    .uppercase()
                            )
                        )
                        // Android 13 and higher have its own notification when copying
                        if (SDK_INT < 33) {
                            Toast.makeText(
                                context,
                                R.string.export_string_state_copied,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDialog(
    onDismiss: () -> Unit,
    boardString: String,
    onClickCopy: () -> Unit
) {
    AlertDialog(
        title = { Text(stringResource(R.string.export_string_title)) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        text = {
            Column {
                Text(stringResource(R.string.export_string_text))
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    value = boardString,
                    onValueChange = { },
                    readOnly = true
                )
                FilledTonalButton(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally),
                    onClick = {
                        onClickCopy()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.export_string_copy))
                }
            }
        }
    )
}