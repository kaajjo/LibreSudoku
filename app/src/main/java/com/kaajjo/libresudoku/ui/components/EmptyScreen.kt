package com.kaajjo.libresudoku.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

@Composable
fun EmptyScreen(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = { }
) {
    val emptyFace by remember {
        mutableStateOf(
            listOf(
                "Σ(ಠ_ಠ)",
                "(･Д･。",
                "(っ˘̩╭╮˘̩)っ",
                "ಥ_ಥ"
            ).random()
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emptyFace,
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            modifier = Modifier.padding(vertical = 8.dp),
            text = text,
            textAlign = TextAlign.Center
        )
        content()
    }
}

@LightDarkPreview
@Composable
private fun EmptyScreenPreview() {
    LibreSudokuTheme {
        Surface {
            EmptyScreen("There is so empty...")
        }
    }
}