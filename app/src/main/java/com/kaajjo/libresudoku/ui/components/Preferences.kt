package com.kaajjo.libresudoku.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreferenceRow(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter? = null,
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium
) {
    val height = if (subtitle != null) 72.dp else 56.dp

    val titleStyle = MaterialTheme.typography.bodyLarge
    val subtitleTextStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(shape)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (painter != null) {
            Icon(
                painter = painter,
                modifier = Modifier
                    .padding(start = 12.dp, end = 14.dp)
                    .size(24.dp),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = null,
            )
        }
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .weight(1f),
        ) {
            Text(
                text = title,
                style = titleStyle,
            )
            if (subtitle != null) {
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = subtitle,
                    style = subtitleTextStyle,
                )
            }
        }
        if (action != null) {
            Box(
                Modifier
                    .widthIn(min = 56.dp)
                    .padding(end = 12.dp),
            ) {
                action()
            }
        }
    }
}

@Composable
fun PreferenceRowSwitch(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter? = null,
    onClick: () -> Unit = { },
    subtitle: String? = null,
    checked: Boolean,
) {
    val icon: (@Composable () -> Unit)? = if (checked) {
        {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(SwitchDefaults.IconSize),
            )
        }
    } else {
        null
    }

    PreferenceRow(
        modifier = modifier,
        title = title,
        painter = painter,
        onClick = onClick,
        subtitle = subtitle,
        action = {
            Switch(
                thumbContent = icon,
                checked = checked,
                onCheckedChange = { onClick() }
            )
        },
    )
}


@LightDarkPreview
@Composable
private fun PreferenceRowPreview() {
    LibreSudokuTheme {
        Surface {
            Column {
                PreferenceRow(
                    title = "Preference row title",
                    subtitle = "Preference summary"
                )
                PreferenceRow(
                    title = "Preference row with icon",
                    subtitle = "Preference with icon",
                    painter = painterResource(R.drawable.ic_settings_24)
                )
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun PreferenceRowSwitchPreview() {
    LibreSudokuTheme {
        Surface {
            Column {
                PreferenceRowSwitch(
                    title = "Preference row with switch",
                    subtitle = "Preference summary",
                    onClick = { },
                    checked = false
                )
                PreferenceRowSwitch(
                    title = "Preference row with switch and icon",
                    subtitle = "Preference summary",
                    painter = painterResource(R.drawable.ic_settings_24),
                    onClick = { },
                    checked = false
                )
                PreferenceRowSwitch(
                    title = "Preference row with switch",
                    subtitle = "Preference summary",
                    onClick = { },
                    checked = true
                )
                PreferenceRowSwitch(
                    title = "Preference row with switch and icon",
                    subtitle = "Preference summary",
                    painter = painterResource(R.drawable.ic_settings_24),
                    onClick = { },
                    checked = true
                )
            }
        }
    }
}