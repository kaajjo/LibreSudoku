package com.kaajjo.libresudoku.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import kotlinx.coroutines.runBlocking

@Composable
fun HomeScreen(
    navigatePlayGame: (Pair<Long, Boolean>) -> Unit,
    viewModel: HomeViewModel
) {
    var continueGameDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround
    ) {

        val lastGame by viewModel.lastSavedGame.collectAsStateWithLifecycle()

        if (viewModel.readyToPlay) {
            viewModel.readyToPlay = false

            runBlocking {
                //viewModel.saveToDatabase()
                val saved = lastGame?.completed ?: false
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
                onLeftClick = { viewModel.changeDifficulty(-1) },
                onRightClick = { viewModel.changeDifficulty(1) }
            )
            HorizontalPicker(
                text = stringResource(viewModel.selectedType.resName),
                onLeftClick = { viewModel.changeType(-1) },
                onRightClick = { viewModel.changeType(1) }
            )

            Spacer(Modifier.height(12.dp))

            if (lastGame != null && !lastGame!!.completed) {
                Button(onClick = {
                    lastGame?.let {
                        navigatePlayGame(Pair(it.uid, true))
                    }
                }) {
                    Text(stringResource(R.string.action_continue))
                }
                FilledTonalButton(onClick = {
                    continueGameDialog = true
                }) {
                    Text(stringResource(R.string.action_play))
                }
            } else {
                Button(onClick = {
                    viewModel.giveUpLastGame()
                    viewModel.startGame()
                }) {
                    Text(stringResource(R.string.action_play))
                }
            }
        }
    }


    if (viewModel.isGenerating || viewModel.isSolving) {
        GeneratingDialog(
            onDismiss = { },
            text = when {
                viewModel.isGenerating -> stringResource(R.string.dialog_generating)
                viewModel.isSolving -> stringResource(R.string.dialog_solving)
                else -> ""
            }
        )
    }

    if (continueGameDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.dialog_new_game)) },
            text = { Text(stringResource(R.string.dialog_new_game_text)) },
            confirmButton = {
                TextButton(onClick = {
                    continueGameDialog = false
                    viewModel.giveUpLastGame()
                    viewModel.startGame()
                }) {
                    Text(stringResource(R.string.dialog_new_game_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { continueGameDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            onDismissRequest = {
                continueGameDialog = false
            }
        )
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
    ) {
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