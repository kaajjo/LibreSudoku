package com.kaajjo.libresudoku.ui.gameshistory.savedgame

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.core.utils.SudokuUtils
import com.kaajjo.libresudoku.data.database.model.SavedGame
import com.kaajjo.libresudoku.data.database.model.SudokuBoard
import com.kaajjo.libresudoku.data.database.repository.BoardRepository
import com.kaajjo.libresudoku.data.database.repository.SavedGameRepository
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.gameshistory.EmptyScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.pow
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
                            0 -> Board(board = viewModel.parsedCurrentBoard, modifier = boardModifier, mainTextSize = fontSize, selectedCell = Cell(-1,-1), onClick = { })
                            1 -> Board(board = viewModel.parsedInitialBoard, modifier = boardModifier, mainTextSize = fontSize, selectedCell = Cell(-1,-1), onClick = { })
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
                    val context = LocalContext.current
                    Text(
                        text = stringResource(
                            R.string.saved_game_difficulty,
                            viewModel.getDifficultyString(viewModel.boardEntity!!.difficulty, context)
                        ),
                        style = textStyle
                    )
                    Text(
                        text = stringResource(
                            R.string.saved_game_type,
                            viewModel.getGameTypeString(viewModel.boardEntity!!.type, context)
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
                                popUpTo("history")
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

@HiltViewModel
class SavedGameViewModel
@Inject constructor(
    private val boardRepository: BoardRepository,
    private val savedGameRepository: SavedGameRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val boardUid = savedStateHandle.get<Long>("uid")
    fun updateGame() {
        viewModelScope.launch(Dispatchers.IO) {
            boardEntity = boardRepository.get(boardUid ?: 0)
            savedGame = savedGameRepository.get(boardUid ?: 0)

            boardEntity?.let {  boardEntity ->
                savedGame?.let { savedGame ->
                    withContext(Dispatchers.Default) {
                        val sudokuParser = SudokuParser()
                        parsedInitialBoard = sudokuParser.parseBoard(boardEntity.initialBoard, boardEntity.type)
                        parsedCurrentBoard = sudokuParser.parseBoard(savedGame.currentBoard, boardEntity.type)
                    }
                }
            }
        }
    }

    var causeMistakesLimit by mutableStateOf(false)
    var correctSolution by mutableStateOf(false)
    fun isSolved(): Boolean {
        val sudokuParser = SudokuParser()

        if(savedGame?.let {
                it.mistakes >= 3
            } == true) {
            causeMistakesLimit = true
            return true
        }

        if(boardEntity == null || savedGame == null || parsedInitialBoard.isEmpty()) return false
        boardEntity?.let { boardEntity ->
            val solvedBoard = sudokuParser.parseBoard(boardEntity.solvedBoard, boardEntity.type)
            if(solvedBoard.size != parsedCurrentBoard.size) return false
            for (i in 0 until boardEntity.type.size) {
                for (j in 0 until boardEntity.type.size) {
                    if (solvedBoard[i][j].value != parsedCurrentBoard[i][j].value) {
                        return false
                    }
                }
            }
        }
        correctSolution = true
        return true
    }
    fun getProgressFilled(): Pair<Int, Int> {
        var size = 0
        val count = boardEntity?.let { boardEntity ->
            boardEntity.type.let { type ->
                size = (type.sectionWidth * type.sectionHeight)
                    .toDouble()
                    .pow(2.0)
                    .toInt()

                size - parsedCurrentBoard.let { board ->
                    board.sumOf { cells -> cells.count { cell -> cell.value == 0} }
                }
            }
        } ?: 0
        return Pair(size, count)
    }

    var savedGame by mutableStateOf<SavedGame?>(null)

    var boardEntity by mutableStateOf<SudokuBoard?>(null)

    var parsedInitialBoard by mutableStateOf(emptyList<List<Cell>>())

    var parsedCurrentBoard by mutableStateOf(emptyList<List<Cell>>())

    fun getDifficultyString(difficulty: GameDifficulty, context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getDifficultyString(difficulty, context)
    }

    fun getGameTypeString(gameType: GameType, context: Context): String {
        val sudokuUtils = SudokuUtils()
        return sudokuUtils.getGameTypeString(gameType, context)
    }
}