package com.kaajjo.libresudoku.ui.folders

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.board.BoardPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: FoldersViewModel,
    navigateBack: () -> Unit,
    navigateExploreFolder: (Int) -> Unit,
    navigateImportSudokuFile: (String) -> Unit,
    navigateViewSavedGame: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var contentUri by remember { mutableStateOf<Uri?>(null) }
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {
            Log.d("FoldersScreen;openDocumentLauncher", "result uri: ${it.toString()}")
            contentUri = it
        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = CreateDocument("application/sdm"),
        onResult = { uri ->
            if (uri != null && viewModel.selectedFolder != null) {
                coroutineScope.launch(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(viewModel.generateFolderExportData())
                        outputStream.close()
                    }
                }
            }
        }
    )

    var createFolderDialog by rememberSaveable { mutableStateOf(false) }
    var renameFolderDialog by rememberSaveable { mutableStateOf(false) }
    var deleteFolderDialog by rememberSaveable { mutableStateOf(false) }
    var folderActionBottomSheet by rememberSaveable { mutableStateOf(false) }
    var helpDialog by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val gamesToImport by viewModel.sudokuListToImport.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            if (gamesToImport.isEmpty()) {
                TopAppBar(
                    title = {
                        Text(stringResource(R.string.title_folders))
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
                        IconButton(onClick = { helpDialog = true }) {
                            Icon(Icons.Rounded.Help, contentDescription = null)
                        }
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
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.AddCircleOutline,
                                                contentDescription = null
                                            )
                                        },
                                        text = {
                                            Text(stringResource(R.string.folder_import))
                                        },
                                        onClick = {
                                            openDocumentLauncher.launch(arrayOf("*/*"))
                                            showMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.CreateNewFolder,
                                                contentDescription = null
                                            )
                                        },
                                        text = {
                                            Text(stringResource(R.string.create_folder))
                                        },
                                        onClick = {
                                            createFolderDialog = true
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = pluralStringResource(
                                R.plurals.number_puzzles_to_import,
                                gamesToImport.size
                            ),
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
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
        ) {
            val folders by viewModel.folders.collectAsStateWithLifecycle(initialValue = emptyList())
            val lastGames by viewModel.lastSavedGames.collectAsStateWithLifecycle(initialValue = emptyList())

            if (folders.isNotEmpty() && gamesToImport.isEmpty()) {
                LaunchedEffect(folders) {
                    viewModel.countPuzzlesInFolders(folders)
                }
                ScrollbarLazyColumn {
                    item {
                        if (lastGames.isNotEmpty()) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    text = stringResource(R.string.last_played_section_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp)
                                ) {
                                    items(lastGames) {
                                        ElevatedCard(
                                            modifier = Modifier
                                                .clip(CardDefaults.elevatedShape)
                                                .clickable { navigateViewSavedGame(it.uid) },
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .size(130.dp)
                                            ) {
                                                BoardPreview(
                                                    size = sqrt(it.currentBoard.length.toFloat()).toInt(),
                                                    boardString = it.currentBoard,
                                                    boardColors = LocalBoardColors.current
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(folders) { item ->
                        val puzzlesCount by remember(viewModel.puzzlesCountInFolder) {
                            mutableIntStateOf(
                                viewModel.puzzlesCountInFolder
                                    .firstOrNull { it.first == item.uid }?.second ?: 0
                            )
                        }
                        FolderItem(
                            name = item.name,
                            puzzlesCount = puzzlesCount,
                            onClick = {
                                navigateExploreFolder(item.uid.toInt())
                            },
                            onLongClick = {
                                viewModel.selectedFolder = item
                                coroutineScope.launch {
                                    folderActionBottomSheet = true
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (createFolderDialog) {
        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        var isError by rememberSaveable { mutableStateOf(false) }

        NameActionDialog(
            icon = {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null)
            },
            title = { Text(stringResource(R.string.create_folder)) },
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                if (isError) isError = false
            },
            isError = isError,
            onConfirm = {
                if (textFieldValue.text.isNotEmpty() && textFieldValue.text.length < 128) {
                    viewModel.createFolder(textFieldValue.text)
                    createFolderDialog = false
                } else {
                    isError = true
                }
            },
            onDismiss = {
                createFolderDialog = false
            }
        )
    } else if (renameFolderDialog) {
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = viewModel.selectedFolder?.name ?: "",
                    selection = TextRange((viewModel.selectedFolder?.name ?: "").length)
                )
            )
        }

        var isError by rememberSaveable { mutableStateOf(false) }

        NameActionDialog(
            title = { Text(stringResource(R.string.edit_name)) },
            icon = {
                Icon(Icons.Rounded.Edit, contentDescription = null)
            },
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                if (isError) isError = false
            },
            isError = isError,
            onConfirm = {
                if (textFieldValue.text.isNotEmpty() && textFieldValue.text.length < 128) {
                    viewModel.renameFolder(textFieldValue.text)
                } else {
                    isError = true
                }
                renameFolderDialog = false
            },
            onDismiss = {
                renameFolderDialog = false
            }
        )
    } else if (deleteFolderDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.delete_folder)) },
            text = {
                viewModel.selectedFolder?.let {
                    Text(stringResource(R.string.dialog_delete_folder_text, it.name))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder()
                    deleteFolderDialog = false
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    deleteFolderDialog = false
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            onDismissRequest = {
                deleteFolderDialog = false
            }
        )
    } else if (helpDialog) {
        AlertDialog(
            icon = {
                Icon(Icons.Rounded.Help, contentDescription = null)
            },
            title = { Text(stringResource(R.string.help)) },
            text = {
                Column {
                    Text(stringResource(R.string.folders_help))
                    Text(stringResource(R.string.folder_import_supported_types))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    helpDialog = false
                }) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
            onDismissRequest = {
                helpDialog = false
            }
        )
    }

    LaunchedEffect(contentUri) {
        contentUri?.let {
            navigateImportSudokuFile(Uri.encode(it.toString()))
        }
    }

    if (folderActionBottomSheet) {
        ModalBottomSheet(onDismissRequest = { folderActionBottomSheet = false }) {
            val actions = listOf(
                Pair(Icons.Rounded.Edit, stringResource(R.string.edit_name)),
                Pair(Icons.Rounded.Share, stringResource(R.string.export)),
                Pair(Icons.Rounded.Delete, stringResource(R.string.action_delete)),
            )
            viewModel.selectedFolder?.let {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Folder, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.folder_name, it.name),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                actions.forEachIndexed { index, action ->
                    Row(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .fillMaxWidth()
                            .clickable {
                                when (index) {
                                    0 -> renameFolderDialog = true
                                    1 -> {
                                        var fileName = ""
                                        viewModel.selectedFolder?.let {
                                            fileName += it.name + "-"
                                        }
                                        fileName += LocalDateTime
                                            .now()
                                            .format(
                                                DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
                                            ) ?: ""
                                        createDocumentLauncher.launch("$fileName.sdm")
                                    }

                                    2 -> deleteFolderDialog = true
                                }
                                coroutineScope.launch {
                                    folderActionBottomSheet = false
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            modifier = Modifier.padding(12.dp),
                            imageVector = action.first,
                            contentDescription = null
                        )
                        Text(action.second)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    name: String,
    puzzlesCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.puzzles_in_folder,
                        puzzlesCount,
                        puzzlesCount
                    ),
                    color = LocalContentColor.current.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameActionDialog(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    isError: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        title = title,
        icon = icon,
        text = {
            OutlinedTextField(
                modifier = Modifier.focusRequester(focusRequester),
                isError = isError,
                singleLine = true,
                value = value,
                onValueChange = onValueChange,
                label = { Text(stringResource(R.string.create_folder_name)) }
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
