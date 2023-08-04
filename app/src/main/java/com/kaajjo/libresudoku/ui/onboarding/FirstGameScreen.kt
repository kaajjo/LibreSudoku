package com.kaajjo.libresudoku.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.game.NotesMenu
import com.kaajjo.libresudoku.ui.game.components.ToolbarItem
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme

@Composable
fun FirstGameDialog(
    onFinished: () -> Unit,
    onDismiss: () -> Unit = { }
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.padding(top = 12.dp),
                    text = stringResource(R.string.first_game_dialog_title),
                    style = MaterialTheme.typography.titleMedium
                )
                FirstGameScreen()
                FilledTonalButton(
                    modifier = Modifier.padding(bottom = 12.dp),
                    onClick = onFinished
                ) {
                    Text(stringResource(R.string.first_game_dialog_got_it))
                }
            }
        }
    }
}

@Composable
fun FirstGameScreen() {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val toolbarWeight by remember { mutableFloatStateOf(0.35f) }
        ToolRow {
            ToolbarItem(
                modifier = Modifier.weight(toolbarWeight),
                painter = painterResource(R.drawable.ic_round_undo_24),
                onClick = { }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.toolbar_undo_description)
            )
        }
        ToolRow {
            ToolbarItem(
                modifier = Modifier.weight(toolbarWeight),
                painter = painterResource(R.drawable.ic_lightbulb_stars_24),
                onClick = { }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.toolbar_hint_description)
            )
        }
        ToolRow {
            var notesMenu by remember { mutableStateOf(false) }
            var noteToggled by remember { mutableStateOf(false) }
            var renderNotes by remember { mutableStateOf(true) }
            Box(
                modifier = Modifier.weight(toolbarWeight)
            ) {
                NotesMenu(
                    expanded = notesMenu,
                    onDismiss = { notesMenu = false },
                    onComputeNotesClick = { },
                    onClearNotesClick = { },
                    renderNotes = renderNotes,
                    onRenderNotesClick = { renderNotes = !renderNotes })
                ToolbarItem(
                    toggled = noteToggled,
                    painter = painterResource(R.drawable.ic_round_edit_24),
                    onClick = { noteToggled = !noteToggled },
                    onLongClick = {
                        notesMenu = true
                    }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.toolbar_notes_description)
            )
        }
        ToolRow {
            ToolbarItem(
                modifier = Modifier.weight(toolbarWeight),
                painter = painterResource(R.drawable.ic_eraser_24),
                onClick = { }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.toolbar_erase_description)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ToolRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        content()
    }
}

@Preview
@Composable
fun ScreenPreview() {
    LibreSudokuTheme {
        FirstGameDialog(
            onFinished = { }
        )
    }
}