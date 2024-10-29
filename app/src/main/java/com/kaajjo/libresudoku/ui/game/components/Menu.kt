package com.kaajjo.libresudoku.ui.game.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kaajjo.libresudoku.R

@Composable
fun GameMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onGiveUpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_give_up)) },
                onClick = {
                    onGiveUpClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings_title)) },
                onClick = {
                    onSettingsClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.export)) },
                onClick = {
                    onExportClick()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun UndoRedoMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRedoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.redo)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = null
                    )
                },
                onClick = {
                    onRedoClick()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun NotesMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onComputeNotesClick: () -> Unit,
    onClearNotesClick: () -> Unit,
    renderNotes: Boolean,
    onRenderNotesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = MaterialTheme.shapes.large)) {
        DropdownMenu(
            modifier = modifier,
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_compute_notes)) },
                onClick = {
                    onComputeNotesClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(R.string.action_clear_notes))
                },
                onClick = {
                    onClearNotesClick()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.action_show_notes))
                        Checkbox(checked = renderNotes, onCheckedChange = { onRenderNotesClick() })
                    }
                },
                onClick = onRenderNotesClick
            )
        }
    }
}
