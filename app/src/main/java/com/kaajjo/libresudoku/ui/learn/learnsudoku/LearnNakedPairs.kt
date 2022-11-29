package com.kaajjo.libresudoku.ui.learn.learnsudoku

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.learn.components.TutorialBase

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LearnNakedPairs(
    helpNavController: NavController
) {
    TutorialBase(
        title = stringResource(R.string.learn_naked_pairs_title),
        helpNavController = helpNavController
    ) {
        val sudokuParser = SudokuParser()
        val board by remember {
            mutableStateOf(
                sudokuParser.parseBoard(
                    board = ".......................................9........68...............................",
                    gameType = GameType.Default9x9,
                    emptySeparator = '.').toList()
            )
        }
        var notes by remember {
            mutableStateOf(
                listOf(
                    Note(3, 3, 1),
                    Note(3, 3, 2),
                    Note(3, 3, 4),
                    Note(3, 3, 5),
                    Note(3, 4, 1),
                    Note(3, 4, 2),
                    Note(3, 4, 4),
                    Note(3, 4, 5),
                    Note(3, 4, 7),
                    Note(3, 5, 2),
                    Note(3, 5, 4),
                    Note(3, 5, 5),
                    Note(3, 5, 7),
                    Note(4, 4, 2),
                    Note(4, 4, 3),
                    Note(4, 5, 2),
                    Note(4, 5, 3),
                    Note(5, 5, 2),
                    Note(5, 5, 3),
                    Note(5, 5, 5),
                )
            )
        }
        val steps = listOf(
            stringResource(R.string.learn_naked_pairs_explanation),
            stringResource(R.string.learn_naked_pairs_end),
        )
        val stepsCell = listOf(
            listOf(Cell(4,4),Cell(4,5))
        )
        var step by remember { mutableStateOf(0) }
        LaunchedEffect(key1 = step) {
            when(step) {
                0 -> {
                    notes = listOf(Note(3,3,1),Note(3,3,2),Note(3,3,4),Note(3,3,5),Note(3,4,1),Note(3,4,2),Note(3,4,4),Note(3,4,5),Note(3,4,7),Note(3,5,2),Note(3,5,4),Note(3,5,5),Note(3,5,7),Note(4,4,2),Note(4,4,3),Note(4,5,2),Note(4,5,3),Note(5,5,2),Note(5,5,3),Note(5,5,5))
                }
                1 -> {
                    notes = listOf(Note(3,3,1),Note(3,3,4),Note(3,3,5),Note(3,4,1),Note(3,4,4),Note(3,4,5),Note(3,4,7),Note(3,5,4),Note(3,5,5),Note(3,5,7),Note(4,4,2),Note(4,4,3),Note(4,5,2),Note(4,5,3),Note(5,5,5))
                }
            }
        }

        Column (
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Board(
                board = board,
                notes = notes,
                cellsToHighlight = if(step < stepsCell.size) stepsCell[step] else null,
                onClick = { },
                selectedCell = Cell(-1,-1)
            )
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(targetState = steps[step]) { stepText ->
                    Column {
                        Text(stepText)
                    }
                }
                Row(
                    modifier = Modifier
                        .align(CenterHorizontally)
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if(step > 0) {
                        FilledTonalButton(onClick = { step-- }) {
                            Text(stringResource(R.string.page_previous))
                        }
                    }
                    if(step < steps.size - 1) {
                        FilledTonalButton(onClick = { step++ }) {
                            Text(stringResource(R.string.page_next))
                        }
                    }
                }
            }
        }
    }
}