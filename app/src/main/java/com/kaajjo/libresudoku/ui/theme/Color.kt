package com.kaajjo.libresudoku.ui.theme

import androidx.annotation.FloatRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

object ColorUtils {

    fun Color.blend(
        color: Color,
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
    ): Color = ColorUtils.blendARGB(this.toArgb(), color.toArgb(), fraction).toColor()

    fun Color.darken(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
    ): Color = blend(color = Color.Black, fraction = fraction)

    fun Color.lighten(
        @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
    ): Color = blend(color = Color.White, fraction = fraction)

    fun Int.toColor(): Color = Color(color = this)

    @Composable
    fun Color.harmonizeWithPrimary(
        @FloatRange(
            from = 0.0,
            to = 1.0
        ) fraction: Float = 0.2f
    ): Color = blend(MaterialTheme.colorScheme.primary, fraction)

}