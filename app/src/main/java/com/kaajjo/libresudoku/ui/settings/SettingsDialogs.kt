package com.kaajjo.libresudoku.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.util.isScrolledToEnd
import com.kaajjo.libresudoku.ui.util.isScrolledToStart

@Composable
fun SelectionDialog(
    title: String,
    selections: List<String>,
    selected: Int = 0,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        text = {
            Column {
                selections.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                onSelect(index)
                                onDismiss()
                            },
                        verticalAlignment = CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = {
                                onSelect(index)
                                onDismiss()
                            }
                        )
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SelectionDialog(
    title: String,
    entries: Map<String, String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        text = {
            Box {
                val lazyListState = rememberLazyListState()

                if (!lazyListState.isScrolledToStart()) Divider(Modifier.align(Alignment.TopCenter))
                if (!lazyListState.isScrolledToEnd()) Divider(Modifier.align(Alignment.BottomCenter))

                ScrollbarLazyColumn(state = lazyListState) {
                    items(entries.toList()) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onSelect(item.first)
                                },
                            verticalAlignment = CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == item.first,
                                onClick = {
                                    onSelect(item.first)
                                }
                            )
                            Text(
                                text = item.second,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun DateFormatDialog(
    title: String,
    entries: Map<String, String>,
    customDateFormatText: String,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        text = {
            Box {
                val lazyListState = rememberLazyListState()

                if (!lazyListState.isScrolledToStart()) Divider(Modifier.align(Alignment.TopCenter))
                if (!lazyListState.isScrolledToEnd()) Divider(Modifier.align(Alignment.BottomCenter))

                ScrollbarLazyColumn(state = lazyListState) {
                    items(entries.toList()) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onSelect(item.first)
                                },
                            verticalAlignment = CenterVertically
                        ) {
                            RadioButton(
                                selected = selected == item.first,
                                onClick = {
                                    onSelect(item.first)
                                }
                            )
                            Text(
                                text = item.second,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable {
                                    onSelect("custom")
                                },
                            verticalAlignment = CenterVertically
                        ) {
                            RadioButton(
                                selected = !entries.containsKey(selected),
                                onClick = {
                                    onSelect("custom")
                                }
                            )
                            Text(
                                text = customDateFormatText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun SetDateFormatPatternDialog(
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    onTextValueChange: (String) -> Unit,
    customDateFormat: String,
    invalidCustomDateFormat: Boolean,
    datePreview: String = ""
) {
    AlertDialog(
        title = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.pref_date_format_custom),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.pref_date_format_custom_summ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = datePreview,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = customDateFormat,
                    onValueChange = onTextValueChange,
                    singleLine = true,
                    isError = invalidCustomDateFormat,
                    label = {
                        Text(stringResource(R.string.pref_date_format_custom_textfield_label))
                    },
                    keyboardActions = KeyboardActions(
                        onDone = { onConfirm() }
                    )
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = { onConfirm() }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}