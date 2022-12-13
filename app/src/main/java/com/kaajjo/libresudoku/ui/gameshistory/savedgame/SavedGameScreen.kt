package com.kaajjo.libresudoku.ui.gameshistory.savedgame

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.gameshistory.EmptyScreen
import com.kaajjo.libresudoku.ui.util.Route
import kotlinx.coroutines.launch
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun SavedGameScreen(
    navController: NavController,
    viewModel: SavedGameViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_game_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        LaunchedEffect(key1 = Unit) {
            viewModel.updateGame()
        }
        if(viewModel.savedGame != null && viewModel.boardEntity != null &&
            viewModel.parsedCurrentBoard.isNotEmpty() && viewModel.parsedInitialBoard.isNotEmpty()
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
            ) {
                val fontSize by remember { mutableStateOf(if(viewModel.boardEntity!!.type == GameType.Default6x6) 34.sp else 28.sp) }
                val pagerState = rememberPagerState()
                val pages = listOf(stringResource(R.string.saved_game_current), stringResource(R.string.saved_game_initial))
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
                            text = { Text(text = title, maxLines = 2, overflow = TextOverflow.Ellipsis) }
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
                        when(page) {
                            0 -> Board(
                                board = viewModel.parsedCurrentBoard,
                                notes = viewModel.notes,
                                modifier = boardModifier,
                                mainTextSize = fontSize,
                                selectedCell = Cell(-1,-1) ,
                                onClick = { }
                            )
                            1 -> Board(
                                board = viewModel.parsedInitialBoard,
                                modifier = boardModifier,
                                mainTextSize = fontSize,
                                selectedCell = Cell(-1,-1),
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

                    LaunchedEffect(key1 = viewModel.parsedInitialBoard) {
                        progress = viewModel.getProgressFilled()
                        viewModel.isSolved()
                    }
                    val textStyle = MaterialTheme.typography.bodyLarge
                    Text(
                        text = stringResource(R.string.saved_game_progress, progress.second, progress.first) ,
                        style = textStyle
                    )
                    Text(
                        text = viewModel.savedGame?.let {
                            when {
                                it.mistakes >= 3 -> stringResource(R.string.saved_game_mistakes_limit)
                                it.giveUp -> stringResource(R.string.saved_game_give_up)
                                it.let { it.completed && !it.canContinue } -> stringResource(R.string.saved_game_completed)
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
                            viewModel.savedGame!!.timer.toKotlinDuration().toComponents { minutes, seconds, _ ->
                                String.format(" %02d:%02d", minutes, seconds)
                            }
                        )
                    )

                    if(viewModel.savedGame!!.canContinue) {
                        FilledTonalButton(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            onClick = { navController.navigate(
                                "game/${viewModel.savedGame!!.uid}/${true}"
                            ) {
                                popUpTo(Route.HISTORY)
                            }}) {
                            Text(stringResource(R.string.action_continue))
                        }
                    }
                }
            }
        } else {
            EmptyScreen(stringResource(R.string.empty_screen_something_went_wrong))
        }
    }
}