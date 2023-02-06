package com.kaajjo.libresudoku.ui.gameshistory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.ui.components.AnimatedIconFilterChip
import com.kaajjo.libresudoku.ui.components.CustomModalBottomSheet
import com.kaajjo.libresudoku.ui.components.EmptyScreen
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import com.kaajjo.libresudoku.ui.create_edit_sudoku.GameStateFilter
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.time.toKotlinDuration

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class
)
@Composable
fun GamesHistoryScreen(
    navigateBack: () -> Unit,
    navigateSavedGame: (Long) -> Unit,
    viewModel: HistoryViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    BackHandler(viewModel.drawerState.isVisible) {
        coroutineScope.launch {
            viewModel.drawerState.hide()
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            if (!viewModel.drawerState.isVisible) {
                                viewModel.drawerState.show()
                            }
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_filter_list_24),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) { innerPadding ->
        val games by viewModel.games.collectAsState(initial = emptyMap())
        if (games.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(innerPadding)
            ) {
                val lazyListState = rememberLazyListState()
                var filteredAndSortedBoards by remember {
                    mutableStateOf(
                        emptyList<Pair<SavedGame, SudokuBoard>>()
                    )
                }
                LaunchedEffect(
                    viewModel.sortType,
                    viewModel.sortEntry,
                    viewModel.filterDifficulties,
                    viewModel.filterGameTypes,
                    viewModel.filterByGameState
                ) {
                    filteredAndSortedBoards = viewModel.applySortAndFilter(games.toList())
                    lazyListState.animateScrollToItem(0)
                }

                ScrollbarLazyColumn(
                    modifier = Modifier
                        .disableSplitMotionEvents(),
                    state = lazyListState
                ) {
                    itemsIndexed(
                        filteredAndSortedBoards,
                        key = { _, game -> game.first.uid }
                    ) { index, game ->
                        SudokuHistoryItem(
                            board = game.first.currentBoard,
                            savedGame = game.first,
                            difficulty = stringResource(game.second.difficulty.resName),
                            type = stringResource(game.second.type.resName),
                            onClick = {
                                navigateSavedGame(game.first.uid)

                            }
                        )
                        if (index < filteredAndSortedBoards.size - 1) {
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
        } else {
            EmptyScreen(stringResource(R.string.history_no_games))
        }
    }

    CustomModalBottomSheet(
        drawerState = viewModel.drawerState,
        sheetContent = {
            Column {
                Text(
                    text = stringResource(R.string.sort_label),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedIconFilterChip(
                        selected = viewModel.sortType == SortType.Ascending,
                        label = stringResource(R.string.sort_ascending),
                        onClick = {
                            viewModel.switchSortType()
                        }
                    )
                    enumValues<SortEntry>().forEach {
                        AnimatedIconFilterChip(
                            selected = it == viewModel.sortEntry,
                            label = stringResource(it.resName),
                            onClick = {
                                viewModel.selectSortEntry(it)
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.filter_label),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GameDifficulty.Easy,
                        GameDifficulty.Moderate,
                        GameDifficulty.Hard,
                        GameDifficulty.Challenge,
                        GameDifficulty.Custom,
                    ).forEach {
                        AnimatedIconFilterChip(
                            selected = viewModel.filterDifficulties.contains(it),
                            label = stringResource(it.resName),
                            onClick = {
                                viewModel.selectFilter(it)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GameType.Default9x9,
                        GameType.Default6x6,
                        GameType.Default12x12,
                    ).forEach {
                        AnimatedIconFilterChip(
                            selected = viewModel.filterGameTypes.contains(it),
                            label = stringResource(it.resName),
                            onClick = {
                                viewModel.selectFilter(it)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        GameStateFilter.All,
                        GameStateFilter.Completed,
                        GameStateFilter.InProgress,
                    ).forEach {
                        AnimatedIconFilterChip(
                            selected = it == viewModel.filterByGameState,
                            label = stringResource(it.resName),
                            onClick = {
                                viewModel.selectFilter(it)
                            }
                        )
                    }
                }
            }
        }
    )
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
                        text = stringResource(
                            R.string.history_item_time,
                            savedGame.timer.toKotlinDuration()
                                .toComponents { minutes, seconds, _ ->
                                    String.format(" %02d:%02d", minutes, seconds)
                                }
                        )
                    )
                    Text(stringResource(R.string.history_item_id, savedGame.uid))
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (savedGame.canContinue) {
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