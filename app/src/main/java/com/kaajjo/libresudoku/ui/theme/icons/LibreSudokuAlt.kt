package com.kaajjo.libresudoku.ui.theme.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import com.kaajjo.libresudoku.ui.util.iconFromXmlPath

val Icons.Rounded.LibreSudokuIconAlt by lazy {
    iconFromXmlPath(
        pathStr = """
                M 40 0 h 1.9 v 60 h -1.9 z M 20 0 h 1.9 v 60 h -1.9 z M 0 20 l 0 -2 l 60 0 l 0 2 z M 0 40 l 0 -2 l 60 0 l 0 2 z
            """.trimIndent(),
        viewportWidth = 60f,
        viewportHeight = 60f,
        fillColor = Color(0xFFC0C0C0.toInt())
    )
}