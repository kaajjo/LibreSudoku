package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.core.Cell
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun BoardPreview(
    modifier: Modifier = Modifier,
    size: Int = 9,
    boardString: String? = null,
    board: List<List<Cell>>? = null,
    mainTextSize: TextUnit = when(size){
        6 -> 16.sp
        9 -> 11.sp
        12 -> 22.sp
        else -> 22.sp
    }
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()

        var cellSize by remember { mutableStateOf( maxWidth / size.toFloat()) }
        val foregroundColor = MaterialTheme.colorScheme.onSurface

        var vertThick by remember { mutableStateOf(floor(sqrt(size.toFloat())).toInt()) }
        var horThick by remember { mutableStateOf(ceil(sqrt(size.toFloat())).toInt()) }

        LaunchedEffect(size) {
            cellSize = maxWidth / size.toFloat()
            vertThick = floor(sqrt(size.toFloat())).toInt()
            horThick = ceil(sqrt(size.toFloat())).toInt()
        }
        val fontSizePx = with(LocalDensity.current) { mainTextSize.toPx() }

        val textPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = foregroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }
        val width by remember { mutableStateOf(textPaint.measureText("1")) }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            drawRoundRect(
                color = foregroundColor,
                topLeft = Offset(0f, 0f),
                size = Size(maxWidth, maxWidth),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 6f)
            )

            for (i in 1 until size) {
                val isThickLine = i % horThick == 0
                drawLine(
                    color = if (isThickLine) {
                        foregroundColor.copy(0.9f)
                    } else {
                        foregroundColor.copy(0.6f)
                    },
                    start = Offset(cellSize * i.toFloat(), 0f),
                    end = Offset(cellSize * i.toFloat(), maxWidth),
                    strokeWidth = if (isThickLine) 4f else 2f
                )
            }
            for (i in 1 until size) {
                val isThickLine = i % vertThick == 0
                drawLine(
                    color = if (isThickLine) {
                        foregroundColor.copy(0.9f)
                    } else {
                        foregroundColor.copy(0.6f)
                    },
                    start = Offset(0f, cellSize * i.toFloat()),
                    end = Offset(maxWidth, cellSize * i.toFloat()),
                    strokeWidth = if (isThickLine) 4f else 2f
                )
            }

            val textBounds = Rect()
            textPaint.getTextBounds("1", 0, 1, textBounds)

            drawIntoCanvas { canvas ->
                if(board != null) {
                    for (i in 0 until size) {
                        for (j in 0 until size) {
                            if (board[i][j].value != 0) {
                                canvas.nativeCanvas.drawText(
                                    board[i][j].value.toString(),
                                    board[i][j].col * cellSize + (cellSize - width) / 2f,
                                    (board[i][j].row * cellSize + cellSize) - (cellSize - textBounds.height()) / 2f,
                                    textPaint
                                )
                            }
                        }
                    }
                } else if(boardString != null && boardString.length == size * size) {
                    for (i in 0 until size) {
                        for (j in 0 until size) {
                            if (boardString[size * j + i] != '0') {
                                canvas.nativeCanvas.drawText(
                                    boardString[size * j + i].toString(),
                                    i * cellSize + (cellSize - width) / 2f,
                                    j * cellSize + cellSize - (cellSize - textBounds.height()) / 2f,
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}