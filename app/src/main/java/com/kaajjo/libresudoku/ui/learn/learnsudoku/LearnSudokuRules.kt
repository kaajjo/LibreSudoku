package com.kaajjo.libresudoku.ui.learn.learnsudoku

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.learn.components.TutorialBase

@Composable
fun LearnSudokuRules(
    helpNavController: NavController
) {
    TutorialBase(
        title = stringResource(R.string.learn_sudoku_rules),
        helpNavController = helpNavController
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var selectedCell by remember { mutableStateOf(Cell(-1, -1, 0)) }
            var secondSelectedCell by remember { mutableStateOf(Cell(-1, -1, 0)) }

            val previewBoard by remember {
                mutableStateOf(
                    listOf(
                        listOf(
                            Cell(0, 0, 0),
                            Cell(0, 1, 0),
                            Cell(0, 2, 0),
                            Cell(0, 3, 6),
                            Cell(0, 4, 0),
                            Cell(0, 5, 0),
                            Cell(0, 6, 0),
                            Cell(0, 7, 0),
                            Cell(0, 8, 0)
                        ),
                        listOf(
                            Cell(1, 0, 8),
                            Cell(1, 1, 2),
                            Cell(1, 2, 4),
                            Cell(1, 3, 7),
                            Cell(1, 4, 5),
                            Cell(1, 5, 3),
                            Cell(1, 6, 1),
                            Cell(1, 7, 6),
                            Cell(1, 8, 9)
                        ),
                        listOf(
                            Cell(2, 0, 0),
                            Cell(2, 1, 0),
                            Cell(2, 2, 0),
                            Cell(2, 3, 2),
                            Cell(2, 4, 0),
                            Cell(2, 5, 0),
                            Cell(2, 6, 0),
                            Cell(2, 7, 0),
                            Cell(2, 8, 0)
                        ),
                        listOf(
                            Cell(3, 0, 0),
                            Cell(3, 1, 0),
                            Cell(3, 2, 0),
                            Cell(3, 3, 5),
                            Cell(3, 4, 0),
                            Cell(3, 5, 0),
                            Cell(3, 6, 4),
                            Cell(3, 7, 7),
                            Cell(3, 8, 1)
                        ),
                        listOf(
                            Cell(4, 0, 0),
                            Cell(4, 1, 0),
                            Cell(4, 2, 0),
                            Cell(4, 3, 1),
                            Cell(4, 4, 0),
                            Cell(4, 5, 0),
                            Cell(4, 6, 3),
                            Cell(4, 7, 8),
                            Cell(4, 8, 6)
                        ),
                        listOf(
                            Cell(5, 0, 0),
                            Cell(5, 1, 0),
                            Cell(5, 2, 0),
                            Cell(5, 3, 4),
                            Cell(5, 4, 0),
                            Cell(5, 5, 0),
                            Cell(5, 6, 9),
                            Cell(5, 7, 2),
                            Cell(5, 8, 5)
                        ),
                        listOf(
                            Cell(6, 0, 0),
                            Cell(6, 1, 0),
                            Cell(6, 2, 0),
                            Cell(6, 3, 3),
                            Cell(6, 4, 0),
                            Cell(6, 5, 0),
                            Cell(6, 6, 0),
                            Cell(6, 7, 0),
                            Cell(6, 8, 0)
                        ),
                        listOf(
                            Cell(7, 0, 0),
                            Cell(7, 1, 0),
                            Cell(7, 2, 0),
                            Cell(7, 3, 9),
                            Cell(7, 4, 0),
                            Cell(7, 5, 0),
                            Cell(7, 6, 0),
                            Cell(7, 7, 0),
                            Cell(7, 8, 0)
                        ),
                        listOf(
                            Cell(8, 0, 0),
                            Cell(8, 1, 0),
                            Cell(8, 2, 0),
                            Cell(8, 3, 8),
                            Cell(8, 4, 0),
                            Cell(8, 5, 0),
                            Cell(8, 6, 0),
                            Cell(8, 7, 0),
                            Cell(8, 8, 0)
                        ),
                    )
                )
            }

            Text(stringResource(R.string.intro_what_is_sudoku))
            Text(stringResource(R.string.intro_rules))
            Board(
                board = previewBoard,
                size = 9,
                selectedCell = selectedCell,
                onClick = { selectedCell = it },
                boardColors = LocalBoardColors.current
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.sudoku_rules_mistakes))

            var highlightError by remember { mutableStateOf(false) }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = highlightError,
                    onCheckedChange = { highlightError = !highlightError })
                Text(stringResource(R.string.sudoku_rules_mistakes_highlight))
            }

            val errorBoard by remember {
                mutableStateOf(previewBoard.map { cells -> cells.map { cell -> cell.copy() } })
            }
            Board(
                board = errorBoard.also {
                    it[1][7].apply {
                        value = 6
                        error = true
                    }
                    it[3][6].apply {
                        value = 2
                        error = true
                    }
                    it[4][7].apply {
                        value = 6
                    }
                    it[4][8].apply {
                        value = 8
                    }
                },
                size = 9,
                errorsHighlight = highlightError,
                selectedCell = secondSelectedCell,
                onClick = { secondSelectedCell = it },
                boardColors = LocalBoardColors.current
            )
            Text(stringResource(R.string.sudoku_rules_mistakes_explanation))
        }
    }
}
