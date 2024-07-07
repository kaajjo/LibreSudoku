package com.kaajjo.libresudoku.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


fun iconFromXmlPath(
    pathStr: String,
    viewportWidth: Float = 24f,
    viewportHeight: Float = 24f,
    defaultWidth: Dp = 24.dp,
    defaultHeight: Dp = 24.dp,
    fillColor: Color = Color.White,
): ImageVector {
    val fillBrush = SolidColor(fillColor)
    val strokeBrush = SolidColor(fillColor)

    return ImageVector.Builder(
        defaultWidth = defaultWidth,
        defaultHeight = defaultHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
    ).run {
        addPath(
            pathData = addPathNodes(pathStr),
            name = "",
            fill = fillBrush,
            stroke = strokeBrush,
        )
        build()
    }
}
