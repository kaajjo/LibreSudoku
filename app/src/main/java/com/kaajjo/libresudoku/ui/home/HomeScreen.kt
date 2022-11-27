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
import androidx.navigation.NavController
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import kotlinx.coroutines.runBlocking

@Composable
fun HomeScreen(
    navController: NavController,
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
                navController.navigate("game/${viewModel.insertedBoardUid}/${saved}")
            }
        }

        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.headlineLarge
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPicker(
                text = when(viewModel.selectedDifficulty) {
                    GameDifficulty.Unspecified -> stringResource(R.string.difficulty_unspecified)
                    GameDifficulty.Simple -> stringResource(R.string.difficulty_simple)
                    GameDifficulty.Easy -> stringResource(R.string.difficulty_easy)
                    GameDifficulty.Moderate -> stringResource(R.string.difficulty_moderate)
                    GameDifficulty.Hard -> stringResource(R.string.difficulty_hard)
                    GameDifficulty.Challenge -> stringResource(R.string.difficulty_challenge)
                    GameDifficulty.Custom -> stringResource(R.string.difficulty_custom)
                },
                onLeftClick = { viewModel.setDifficulty(-1) },
                onRightClick = { viewModel.setDifficulty(1) }
            )
            HorizontalPicker(
                text = when(viewModel.selectedType) {
                    GameType.Unspecified -> stringResource(R.string.type_unspecified)
                    GameType.Default6x6 -> stringResource(R.string.type_default_6x6)
                    GameType.Default9x9 -> stringResource(R.string.type_default_9x9)
                    GameType.Default12x12 -> stringResource(R.string.type_default_12x12)
                },
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
                Text(
                    text = stringResource(R.string.action_play)
                )
            }
            if(lastGame.value != null && !lastGame.value!!.completed) {
                Button(onClick = {
                    navController.navigate("game/${lastGame.value!!.uid}/true")
                }) {
                    Text(
                        text = stringResource(R.string.action_continue)
                    )
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
            color = MaterialTheme.colorScheme.surface
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
            Text(
                text = text
            )
        }
        IconButton(onClick = onRightClick) {
            Icon(
                painter = painterResource(R.drawable.ic_round_keyboard_arrow_right_24),
                contentDescription = null
            )
        }
    }
}