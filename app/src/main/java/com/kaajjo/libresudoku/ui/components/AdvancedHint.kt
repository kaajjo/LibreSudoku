package com.kaajjo.libresudoku.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.qqwing.advanced_hint.AdvancedHintData
import com.materialkolor.ktx.blend
import com.materialkolor.ktx.harmonize

@androidx.compose.runtime.Composable
fun AdvancedHintContainer(
    advancedHintData: AdvancedHintData,
    onApplyClick: (() -> Unit)?,
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                advancedHintData.let {
                    BackHandler {
                        onBackClick()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .background(
                                with(MaterialTheme.colorScheme) {
                                    primary.blend(secondaryContainer, 0.75f)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                tint = with(MaterialTheme.colorScheme) {
                                    onSecondaryContainer.harmonize(primary)
                                }
                            )
                            Text(
                                text = stringResource(it.titleRes),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = with(MaterialTheme.colorScheme) {
                                    onSecondaryContainer.harmonize(primary)
                                }
                            )
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SettingsSuggest,
                                contentDescription = null
                            )
                        }
                    }
                    Text(
                        text = stringResource(
                            it.textResWithArg.first,
                            *it.textResWithArg.second.toTypedArray()
                        ),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = onBackClick,
            ) {
                Text(stringResource(R.string.nav_back))
            }
            FilledTonalButton(
                onClick = onApplyClick ?: { },
                enabled = onApplyClick != null
            ) {
                Text(stringResource(R.string.action_apply))
            }
        }
    }
}