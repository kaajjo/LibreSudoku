package com.kaajjo.libresudoku.ui.learn.learnsudoku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.learn.components.TutorialBase
import com.kaajjo.libresudoku.ui.learn.components.TutorialBottomContent

@Composable
fun LearnNakedPairs(
    helpNavController: NavController
) {
    TutorialBase(
        title = stringResource(R.string.naked_pairs_title),
        helpNavController = helpNavController
    ) {
        val sudokuParser = SudokuParser()
        val board by remember {
            mutableStateOf(
                sudokuParser.parseBoard(
                    board = ".......................................9........68...............................",
                    gameType = GameType.Default9x9,
                    emptySeparator = '.'
                ).toList()
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
            stringResource(R.string.naked_pairs_explanation),
            stringResource(R.string.naked_pairs_end),
        )
        val stepsCell = listOf(
            listOf(Cell(4, 4), Cell(4, 5))
        )
        var step by remember { mutableIntStateOf(0) }
        LaunchedEffect(key1 = step) {
            when (step) {
                0 -> {
                    notes = listOf(
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
                        Note(5, 5, 5)
                    )
                }
                1 -> {
                    notes = listOf(
                        Note(3, 3, 1),
                        Note(3, 3, 4),
                        Note(3, 3, 5),
                        Note(3, 4, 1),
                        Note(3, 4, 4),
                        Note(3, 4, 5),
                        Note(3, 4, 7),
                        Note(3, 5, 4),
                        Note(3, 5, 5),
                        Note(3, 5, 7),
                        Note(4, 4, 2),
                        Note(4, 4, 3),
                        Note(4, 5, 2),
                        Note(4, 5, 3),
                        Note(5, 5, 5)
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Board(
                board = board,
                notes = notes,
                cellsToHighlight = if (step < stepsCell.size) stepsCell[step] else null,
                onClick = { },
                selectedCell = Cell(-1, -1),
                boardColors = LocalBoardColors.current
            )
            TutorialBottomContent(
                steps = steps,
                step = step,
                onPreviousClick = { if (step > 0) step-- },
                onNextClick = { if (step < (steps.size - 1)) step++ }
            )
        }
    }
}

