package com.kaajjo.libresudoku.ui.import_from_file

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyVerticalGrid
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import com.kaajjo.libresudoku.ui.util.isScrolledToStart
import com.kaajjo.libresudoku.ui.util.isScrollingUp
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ImportFromFileScreen(
    viewModel: ImportFromFileViewModel,
    navigateBack: () -> Unit
) {
    BackHandler {
        navigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val gamesToImport by viewModel.sudokuListToImport.collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(viewModel.fileUri) {
        viewModel.fileUri?.let { fileUri ->
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.let { stream ->
                viewModel.readData(InputStreamReader(stream))
            }
        }
    }

    var setFolderNameDialog by rememberSaveable { mutableStateOf(false) }

    val lazyGridState = rememberLazyGridState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.import_from_file_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = lazyGridState.isScrollingUp() && !lazyGridState.isScrolledToStart(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { lazyGridState.animateScrollToItem(0) }
                    }
                ) {
                    Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                }
            }
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    pluralStringResource(
                        R.plurals.number_puzzles_to_import,
                        gamesToImport.size,
                        gamesToImport.size
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        var gameTypeMenuExpanded by remember { mutableStateOf(false) }
                        val dropDownIconRotation by animateFloatAsState(if (gameTypeMenuExpanded) 180f else 0f)
                        TextButton(onClick = { gameTypeMenuExpanded = !gameTypeMenuExpanded }) {
                            AnimatedContent(stringResource(viewModel.difficultyForImport.resName)) { text ->
                                Text(text)
                            }
                            Icon(
                                modifier = Modifier.rotate(dropDownIconRotation),
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                        ImportDifficultyMenu(
                            expanded = gameTypeMenuExpanded,
                            onDismissRequest = { gameTypeMenuExpanded = false },
                            onClick = { difficulty -> viewModel.setDifficulty(difficulty) }
                        )
                    }
                    FilledTonalButton(
                        enabled = gamesToImport.isNotEmpty(),
                        onClick = {
                            if (viewModel.folderUid == -1L) {
                                setFolderNameDialog = true
                            } else {
                                viewModel.saveImported()
                            }
                        }) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
            HorizontalDivider()
            var span by remember { mutableIntStateOf(1) }
            ScrollbarLazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(130.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                spanCount = span,
                content = {
                    items(
                        items = gamesToImport,
                        span = { GridItemSpan(1).also { span = maxLineSpan } }
                    ) { item ->
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            BoardPreview(
                                size = 9,
                                boardString = item,
                                boardColors = LocalBoardColors.current
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                        }
                    }
                }
            )
            if (viewModel.isLoading) {
                Dialog(onDismissRequest = { }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }

    if (setFolderNameDialog) {
        var value by remember { mutableStateOf("") }
        var isError by rememberSaveable { mutableStateOf(false) }

        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        AlertDialog(
            title = { Text(stringResource(R.string.create_folder)) },
            text = {
                OutlinedTextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    isError = isError,
                    singleLine = true,
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.create_folder_name)) }
                )
            },
            onDismissRequest = { setFolderNameDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (value.isNotEmpty() && value.length < 128) {
                            viewModel.saveImported(value)
                            setFolderNameDialog = false
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { setFolderNameDialog = false }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (viewModel.isSaving) {
        Dialog(onDismissRequest = { }) {
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
                                text = stringResource(R.string.import_saving),
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

    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) {
            navigateBack()
        }
    }
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    LaunchedEffect(importError) {
        if (importError) {
            Toast.makeText(
                context,
                context.getString(R.string.import_from_file_fail),
                Toast.LENGTH_SHORT
            ).show()
            navigateBack()
        }
    }
}

@Composable
private fun ImportDifficultyMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onClick: (GameDifficulty) -> Unit
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            listOf(
                GameDifficulty.Easy,
                GameDifficulty.Moderate,
                GameDifficulty.Hard,
                GameDifficulty.Challenge,
                GameDifficulty.Custom,
            ).forEach {
                DropdownMenuItem(
                    text = {
                        Text(stringResource(it.resName))
                    },
                    onClick = {
                        onClick(it)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}