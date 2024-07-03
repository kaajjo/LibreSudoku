package com.kaajjo.libresudoku.ui.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.kaajjo.color_picker.picker.selector.SelectorRectSaturationValueHSV
import com.kaajjo.color_picker.picker.ui.slider.SliderHueHSV
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.theme.ColorUtils.harmonizeWithPrimary

@Composable
fun ColorSelection(
    color: Int,
    onColorChange: (Int) -> Unit
) {
    val color1 = Color(color)
    val hsv by remember { mutableStateOf(FloatArray(3)) }
    LaunchedEffect(color, color1) {
        android.graphics.Color.colorToHSV(color1.toArgb(), hsv)
    }
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    val saturation = hsv[1]
    val value = hsv[2]

    Column {

        Spacer(Modifier.height(16.dp))
        SelectorRectSaturationValueHSV(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(MaterialTheme.shapes.small),
            hue = hue,
            saturation = saturation,
            value = value
        ) { s, v ->
            onColorChange(
                Color.hsv(hue, s, v).toArgb()
            )
        }
        Spacer(Modifier.height(8.dp))
        SliderHueHSV(
            hue = hue,
            saturation = saturation,
            value = value,
            onValueChange = { h ->
                hue = h
                onColorChange(
                    Color.hsv(hue, saturation, value).toArgb()
                )
            },
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        LaunchedEffect(color1) {
            if (hue != hsv[0]) hue = hsv[0]
        }
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ColorPickerDialog(
    currentColor: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onHexColorClick: () -> Unit,
    onRandomColorClick: () -> Unit,
    onColorChange: (Int) -> Unit,
    onPaste: () -> Unit
) {
    AlertDialog(
        icon = {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.color_picker_title)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        onDismissRequest = onDismiss,
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onRandomColorClick)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(currentColor))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp),
                                tint = animateColorAsState(
                                    if (ColorUtils.calculateLuminance(currentColor) >= 0.5f) {
                                        Color.DarkGray
                                    } else {
                                        Color.LightGray
                                    }, label = ""
                                ).value
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#" + currentColor.toHexString(HexFormat.UpperCase),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClick = onHexColorClick)
                                .padding(2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clickable(onClick = onPaste)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                MaterialTheme.colorScheme
                                    .surfaceColorAtElevation(6.dp)
                                    .harmonizeWithPrimary()
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
                ColorSelection(
                    color = currentColor,
                    onColorChange = { onColorChange(it) }
                )
            }
        }
    )
}
