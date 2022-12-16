package com.kaajjo.libresudoku.ui.gameshistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.kaajjo.libresudoku.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import kotlinx.coroutines.coroutineScope
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesHistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
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
        },
    ) { innerPadding ->
        val savedGamesState by viewModel.savedGames.collectAsState(initial = emptyList())

        val boards by viewModel.getBoards().collectAsState(initial = emptyList())
        if (savedGamesState.isNotEmpty() && boards.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                LazyColumn(
                    modifier = Modifier.disableSplitMotionEvents()
                ) {
                    itemsIndexed(savedGamesState.reversed()) { index, savedGame ->
                        val board = boards.first { board -> board.uid == savedGame.uid }
                        board.let {
                            SudokuHistoryItem(
                                board = savedGamesState.reversed()[index].currentBoard,
                                savedGame = savedGame,
                                difficulty = stringResource(it.difficulty.resName),
                                type = stringResource(it.type.resName),
                                onClick = {
                                    navController.navigate(
                                        "saved_game/${savedGame.uid}"
                                    )
                                }
                            )
                            if(index < savedGamesState.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            EmptyScreen(text = stringResource(R.string.history_no_games))
        }
    }
}

@Composable
fun EmptyScreen(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "(っ˘̩╭╮˘̩)っ",
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun SudokuHistoryItem(
    board: String,
    difficulty: String,
    type: String,
    savedGame: SavedGame,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { }
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .size(130.dp)
                    .align(Alignment.CenterVertically)
            ) {
                BoardPreview(
                    size = sqrt(board.length.toFloat()).toInt(),
                    boardString = board
                )
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
            ) {
                Column {
                    Text("$difficulty $type")
                    Text(
                        text = stringResource(R.string.history_item_time) + savedGame.timer.toKotlinDuration()
                            .toComponents { minutes, seconds, _ ->
                                String.format(" %02d:%02d", minutes, seconds)
                            }
                    )
                    Text(stringResource(R.string.history_item_id, savedGame.uid))
                }

                Spacer(modifier = Modifier.height(12.dp))
                if(savedGame.canContinue) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.large
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.can_continue_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

// https://stackoverflow.com/questions/69901608/how-to-disable-simultaneous-clicks-on-multiple-items-in-jetpack-compose-list-c
fun Modifier.disableSplitMotionEvents() =
    pointerInput(Unit) {
        coroutineScope {
            var currentId: Long = -1L
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { pointerInfo ->
                        when {
                            pointerInfo.pressed && currentId == -1L -> currentId =
                                pointerInfo.id.value
                            pointerInfo.pressed.not() && currentId == pointerInfo.id.value -> currentId =
                                -1
                            pointerInfo.id.value != currentId && currentId != -1L -> pointerInfo.consume()
                            else -> Unit
                        }
                    }
                }
            }
        }
    }