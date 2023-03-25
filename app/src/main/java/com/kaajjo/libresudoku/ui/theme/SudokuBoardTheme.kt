package com.kaajjo.libresudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.kaajjo.libresudoku.ui.theme.ColorUtils.blend
import com.kaajjo.libresudoku.ui.theme.ColorUtils.harmonizeWithPrimary

object BoardColors {
    inline val foregroundColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface.blend(
            MaterialTheme.colorScheme.primary,
            fraction = 0.65f
        )

    inline val notesColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.blend(
            MaterialTheme.colorScheme.secondary,
            0.4f
        )
    inline val altForegroundColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.blend(
            MaterialTheme.colorScheme.secondary,
            0.5f
        ).copy(alpha = 0.85f)

    inline val errorColor: Color
        @Composable
        get() = Color(230, 67, 83).harmonizeWithPrimary()

    inline val highlightColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.secondary

    inline val thickLineColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f)

    inline val thinLineColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f)
}

interface SudokuBoardColors {
    val foregroundColor: Color
    val notesColor: Color
    val altForegroundColor: Color
    val errorColor: Color
    val highlightColor: Color
    val thickLineColor: Color
    val thinLineColor: Color
}

class SudokuBoardColorsImpl(
    override val foregroundColor: Color = Color.White,
    override val notesColor: Color = Color.White,
    override val altForegroundColor: Color = Color.White,
    override val errorColor: Color = Color.White,
    override val highlightColor: Color = Color.White,
    override val thickLineColor: Color = Color.White,
    override val thinLineColor: Color = Color.White,
) : SudokuBoardColors
