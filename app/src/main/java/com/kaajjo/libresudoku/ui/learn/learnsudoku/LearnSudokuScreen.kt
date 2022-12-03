package com.kaajjo.libresudoku.ui.learn.learnsudoku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.learn.components.LearnRowItem

@Composable
fun LearnSudokuScreen(
    helpNavController: NavController
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyColumn {
            item {
                LearnRowItem(
                    title = stringResource(R.string.learn_sudoku_rules),
                    onClick = { helpNavController.navigate("sudoku_rules") }
                )
                LearnRowItem(
                    title = stringResource(R.string.learn_basic_title),
                    onClick = { helpNavController.navigate("sudoku_basic") }
                )
                LearnRowItem(
                    title = stringResource(R.string.naked_pairs_title),
                    onClick = { helpNavController.navigate("sudoku_naked_pairs") }
                )
                LearnRowItem(
                    title = stringResource(R.string.learn_hidden_pairs_title),
                    onClick = { helpNavController.navigate("sudoku_hidden_pairs") }
                )
            }
        }
    }
}
