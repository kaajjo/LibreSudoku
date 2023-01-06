package com.kaajjo.libresudoku.ui.customsudoku.createsudoku

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.game.components.DefaultGameKeyboard
import com.kaajjo.libresudoku.ui.game.components.ToolBarItem
import com.kaajjo.libresudoku.ui.game.components.ToolbarItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSudokuScreen(
    navigateBack: () -> Unit,
    viewModel: CreateSudokuViewModel
) {
    var importStringDialog by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.create_sudoku_title))
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.create_set_from_string))
                                    },
                                    onClick = {
                                        importStringDialog = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            val inputMethod by viewModel.inputMethod.collectAsState(initial = 1)
            val highlightIdentical by viewModel.highlightIdentical.collectAsState(initial = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    var gameTypeMenuExpanded by remember { mutableStateOf(false) }
                    val dropDownIconRotation by animateFloatAsState(if(gameTypeMenuExpanded) 180f else 0f)
                    TextButton(onClick = { gameTypeMenuExpanded = !gameTypeMenuExpanded }) {
                        Text(stringResource(viewModel.gameType.resName))
                        Icon(
                            modifier = Modifier.rotate(dropDownIconRotation),
                            imageVector = Icons.Rounded.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                    GameTypeMenu(
                        expanded = gameTypeMenuExpanded,
                        onDismissRequest = { gameTypeMenuExpanded = false },
                        onClick = {
                            viewModel.changeGameType(it)
                        }
                    )
                }
                FilledTonalButton(
                    enabled = !viewModel.gameBoard.flatten().all { it.value == 0 },
                    onClick = {
                        if(viewModel.saveGame()) {
                            navigateBack()
                        }
                }) {
                    Text(stringResource(R.string.create_save))
                }
            }

            val fontSizeFactor by viewModel.fontSize.collectAsState(initial = 1)
            var fontSizeValue by remember {
                mutableStateOf(
                    viewModel.getFontSize(factor = fontSizeFactor)
                )
            }
            LaunchedEffect(fontSizeFactor, viewModel.gameType) {
                fontSizeValue = viewModel.getFontSize(factor = fontSizeFactor)
            }
            Board(
                modifier = Modifier.padding(vertical = 12.dp),
                board = viewModel.gameBoard,
                selectedCell = viewModel.currCell,
                onClick = { cell ->
                    viewModel.processInput(
                        inputMethod = inputMethod,
                        cell = cell
                    )
                },
                identicalNumbersHighlight = highlightIdentical
            )
            DefaultGameKeyboard(
                size = viewModel.gameType.size,
                remainingUses = null,
                onClick = {
                    viewModel.processInputKeyboard(
                        number = it,
                        inputMethod = inputMethod
                    )
                },
                onLongClick = {
                    viewModel.processInputKeyboard(
                        number = it,
                        inputMethod = inputMethod,
                        longTap = true
                    )
                },
                selected = viewModel.digitFirstNumber
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_round_undo_24),
                    onClick = { viewModel.toolbarClick(ToolBarItem.Undo) }
                )
                ToolbarItem(
                    modifier = Modifier.weight(1f),
                    painter = painterResource(R.drawable.ic_eraser_24),
                    onClick = {
                        viewModel.toolbarClick(ToolBarItem.Remove)
                    }
                )
            }

            if(importStringDialog) {
                ImportStringSudokuDialog(
                    textValue = viewModel.importStringValue,
                    onTextChange = {
                        viewModel.importStringValue = it
                        viewModel.importTextFieldError = false
                    },
                    isError = viewModel.importTextFieldError,
                    onConfirm = {
                        viewModel.setFromString(viewModel.importStringValue.trim()).also {
                            viewModel.importTextFieldError = !it
                            if(it) {
                                importStringDialog = false
                                viewModel.importStringValue = ""
                            }
                        }
                    },
                    onDismiss = { importStringDialog = false }
                )
            } else if(viewModel.multipleSolutionsDialog) {
                AlertDialog(
                    title = { Text(stringResource(R.string.create_incorrect_puzzle)) },
                    text = {
                        Text(stringResource(R.string.multiple_solution_text))
                    },
                    onDismissRequest = { viewModel.multipleSolutionsDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.multipleSolutionsDialog = false
                        }) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    }
                )
            } else if(viewModel.noSolutionsDialog) {
                AlertDialog(
                    title = { Text(stringResource(R.string.create_incorrect_puzzle)) },
                    text = {
                        Text(stringResource(R.string.no_solution_text))
                    },
                    onDismissRequest = { viewModel.noSolutionsDialog = false },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.noSolutionsDialog = false
                        }) {
                            Text(stringResource(R.string.dialog_ok))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GameTypeMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onClick: (GameType) -> Unit
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissRequest
        ) {
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.type_default_9x9))
                },
                onClick = {
                    onClick(GameType.Default9x9)
                    onDismissRequest()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.type_default_6x6))
                },
                onClick = {
                    onClick(GameType.Default6x6)
                    onDismissRequest()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportStringSudokuDialog(
    textValue: String,
    onTextChange: (String) -> Unit,
    isError: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = FocusRequester()
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    AlertDialog(
        title = { Text(stringResource(R.string.create_set_from_string)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_from_string_text))
                OutlinedTextField(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .focusRequester(focusRequester),
                    value = textValue,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm() }
                    ),
                    isError = isError,
                    onValueChange = onTextChange,
                    label = { Text(stringResource(R.string.create_from_string_hint)) }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.create_import_set))
            }
        }
    )
}