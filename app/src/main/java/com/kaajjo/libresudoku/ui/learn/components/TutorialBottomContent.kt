package com.kaajjo.libresudoku.ui.learn.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R

@Composable
@OptIn(ExperimentalAnimationApi::class)
fun TutorialBottomContent(
    steps: List<String>,
    step: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AnimatedContent(targetState = steps[step]) { stepText ->
            Column {
                Text(stepText)
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = step > 0,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically { it }
            ) {
                FilledTonalButton(onClick = onPreviousClick) {
                    Text(stringResource(R.string.page_previous))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            AnimatedVisibility(
                visible = step < steps.size - 1,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically { it }
            ) {
                FilledTonalButton(onClick = onNextClick) {
                    Text(stringResource(R.string.page_next))
                }
            }
        }
    }
}