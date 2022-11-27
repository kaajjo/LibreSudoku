package com.kaajjo.libresudoku.ui.learn.learnsudoku

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
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
                    title = "EXAMPLE 1",
                    onClick = { }
                )
            }
        }
    }
}
