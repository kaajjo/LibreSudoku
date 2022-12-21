package com.kaajjo.libresudoku.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import com.kaajjo.libresudoku.R
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.runBlocking

@Composable
fun HomeScreen(
    navigatePlayGame: (Pair<Long, Boolean>) -> Unit,
    viewModel: HomeViewModel
) {
    if(viewModel.showContinueGameDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.dialog_new_game)) },
            text = { Text(stringResource(R.string.dialog_new_game_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.showContinueGameDialog = false
                    viewModel.giveUpLastGame()
                    viewModel.generate()
                }) {
                    Text(stringResource(R.string.dialog_new_game_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showContinueGameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            onDismissRequest = {
                viewModel.showContinueGameDialog = false
            }
        )
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {
        if(viewModel.isGenerating || viewModel.isSolving) {
            GeneratingDialog(
                onDismiss = { },
                text = if(viewModel.isGenerating) stringResource(R.string.dialog_generating) else stringResource(R.string.dialog_solving)
            )
        }
        LaunchedEffect(viewModel.generated) {
            if(viewModel.generated) {
                viewModel.solve()
            }
        }

        val lastGame = viewModel.lastSaved.collectAsState(initial = null)
        if(viewModel.generated
            && !viewModel.isGenerating
            && !viewModel.isSolving
            && viewModel.solved
        ) {
            viewModel.generated = false
            viewModel.solved = false

            runBlocking {
                viewModel.saveToDatabase()
                val saved = if(lastGame.value != null) !lastGame.value?.completed!! else false
                navigatePlayGame(Pair(viewModel.insertedBoardUid, saved))
            }
        }

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPicker(
                text = stringResource(viewModel.selectedDifficulty.resName),
                onLeftClick = { viewModel.setDifficulty(-1) },
                onRightClick = { viewModel.setDifficulty(1) }
            )
            HorizontalPicker(
                text = stringResource(viewModel.selectedType.resName),
                onLeftClick = { viewModel.setType(-1) },
                onRightClick = { viewModel.setType(1) }
            )

            Button(onClick = {
                if(lastGame.value != null && !lastGame.value!!.completed) {
                    viewModel.showContinueGameDialog = true
                } else {
                    viewModel.giveUpLastGame()
                    viewModel.generate()
                }
            }) {
                Text(stringResource(R.string.action_play))
            }
            if(lastGame.value != null && !lastGame.value!!.completed) {
                Button(onClick = {
                    navigatePlayGame(Pair(lastGame.value!!.uid, true))
                }) {
                    Text(stringResource(R.string.action_continue))
                }
            }
        }
    }
}

@Composable
fun GeneratingDialog(
    onDismiss: () -> Unit,
    text: String
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Column(
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HorizontalPicker(
    modifier: Modifier = Modifier,
    text: String,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        IconButton(onClick = onLeftClick) {
            Icon(
                painter = painterResource(R.drawable.ic_round_keyboard_arrow_left_24),
                contentDescription = null
            )
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn() with fadeOut() }
        ) {
            Text(text)
        }
        IconButton(onClick = onRightClick) {
            Icon(
                painter = painterResource(R.drawable.ic_round_keyboard_arrow_right_24),
                contentDescription = null
            )
        }
    }
}