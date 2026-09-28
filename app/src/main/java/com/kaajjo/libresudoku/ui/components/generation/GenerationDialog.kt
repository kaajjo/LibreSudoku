package com.kaajjo.libresudoku.ui.components.generation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R

/**
 * Displays cancellable generation progress or a retryable save failure.
 *
 * @param state Current session state; Idle and Completed display no dialog.
 * @param onRetry Retries saving the already generated puzzle after a save failure.
 * @param onCancel Cancels the session when the button is pressed or the dialog is dismissed.
 */
@Composable
fun GenerationDialog(
    state: GenerationState,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val progress = when (state) {
        is GenerationState.Running -> state.progress
        is GenerationState.SaveFailed -> state.progress
        GenerationState.Idle, GenerationState.Completed -> return
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(stringResource(when (state) {
                is GenerationState.SaveFailed -> R.string.generation_save_error_title
                else -> R.string.dialog_generating
            }))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state) {
                    is GenerationState.Running -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    is GenerationState.SaveFailed -> Text(stringResource(R.string.generation_save_error_message))
                    else -> Unit
                }
                if (progress.total > 1) {
                    Text(stringResource(R.string.generating_number_of, progress.completed, progress.total))
                    if (state !is GenerationState.Running && progress.completed > 0) {
                        Text(stringResource(R.string.generation_saved_progress))
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state is GenerationState.SaveFailed) {
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.generation_retry))
                    }
                }
                TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}
