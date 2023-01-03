package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.LightDarkPreview
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@Composable
fun Board(
    modifier: Modifier = Modifier,
    board: List<List<Cell>>,
    size: Int = board.size,
    notes: List<Note>? = null,
    mainTextSize: TextUnit = when (size) {
        6 -> 32.sp
        9 -> 26.sp
        12 -> 18.sp
        else -> 14.sp
    },
    noteTextSize: TextUnit = when (size) {
        6 -> 18.sp
        9 -> 12.sp
        else -> 14.sp
    },
    selectedCell: Cell,
    onClick: (Cell) -> Unit,
    onLongClick: (Cell) -> Unit = { },
    identicalNumbersHighlight: Boolean = true,
    errorsHighlight: Boolean = true,
    positionLines: Boolean = true,
    enabled: Boolean = true,
    questions: Boolean = false,
    renderNotes: Boolean = true,
    cellsToHighlight: List<Cell>? = null,
    zoomable: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()

        // single cell size
        var cellSize by remember { mutableStateOf(maxWidth / size.toFloat()) }
        // 1/3 of single cell size, used for notes
        val cellSizeDiv by remember { mutableStateOf(cellSize / 3f) }
        // lines and font color
        val foregroundColor = MaterialTheme.colorScheme.onSurface
        // locked numbers
        val altForegroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)

        // highlight (cells)
        val highlightColor = MaterialTheme.colorScheme.outline

        var vertThick by remember { mutableStateOf(floor(sqrt(size.toFloat())).toInt()) }
        var horThick by remember { mutableStateOf(ceil(sqrt(size.toFloat())).toInt()) }
        LaunchedEffect(size) {
            cellSize = maxWidth / size.toFloat()
            vertThick = floor(sqrt(size.toFloat())).toInt()
            horThick = ceil(sqrt(size.toFloat())).toInt()
        }

        var fontSizePx = with(LocalDensity.current) { mainTextSize.toPx() }
        val noteSizePx = with(LocalDensity.current) { noteTextSize.toPx() }


        // paints
        // numbers
        var textPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = foregroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }
        // errors
        var errorTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = Color(230, 67, 83).toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }
        // locked numbers
        var lockedTextPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = altForegroundColor.toArgb()
                    isAntiAlias = true
                    textSize = fontSizePx
                }
            )
        }

        // notes
        val notePaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = foregroundColor.toArgb()
                    isAntiAlias = true
                    textSize = noteSizePx
                }
            )
        }
        // number font width
        var width by remember { mutableStateOf(textPaint.measureText("1")) }

        val context = LocalContext.current
        LaunchedEffect(mainTextSize) {
            fontSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                mainTextSize.value,
                context.resources.displayMetrics
            )
            textPaint = Paint().apply {
                color = foregroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
            width = textPaint.measureText("1")

            errorTextPaint = Paint().apply {
                color = Color(230, 67, 83).toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }

            lockedTextPaint = Paint().apply {
                color = altForegroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
        }

        var zoom by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        LaunchedEffect(enabled) {
            zoom = 1f
            offset = Offset.Zero
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoomable, enabled) {
                    detectTransformGestures(
                        onGesture = { gestureCentroid, gesturePan, gestureZoom, _ ->
                            if (zoomable && enabled) {
                                val oldScale = zoom
                                val newScale = (zoom * gestureZoom).coerceIn(1f..3f)

                                offset = (offset + gestureCentroid / oldScale) -
                                        (gestureCentroid / newScale + gesturePan / oldScale)

                                zoom = newScale
                                if (offset.x < 0) {
                                    offset = Offset(0f, offset.y)
                                } else if (maxWidth - offset.x < maxWidth / zoom) {
                                    offset = offset.copy(x = maxWidth - maxWidth / zoom)
                                }
                                if (offset.y < 0) {
                                    offset = Offset(offset.x, 0f)
                                } else if (maxWidth - offset.y < maxWidth / zoom) {
                                    offset = offset.copy(y = maxWidth - maxWidth / zoom)
                                }
                            }
                        }
                    )
                }
                .pointerInput(key1 = enabled, key2 = board) {
                    detectTapGestures(
                        onTap = {
                            if (enabled) {
                                val totalOffset = it / zoom + offset
                                val row = floor((totalOffset.y) / cellSize).toInt()
                                val column = floor((totalOffset.x) / cellSize).toInt()
                                onClick(board[row][column])
                            }
                        },
                        onLongPress = {
                            if (enabled) {
                                val totalOffset = it / zoom + offset
                                val row = floor((totalOffset.y) / cellSize).toInt()
                                val column = floor((totalOffset.x) / cellSize).toInt()
                                onLongClick(board[row][column])
                            }
                        }
                    )
                }
                .graphicsLayer {
                    translationX = -offset.x * zoom
                    translationY = -offset.y * zoom
                    scaleX = zoom
                    scaleY = zoom
                    TransformOrigin(0f, 0f).also { transformOrigin = it }
                }
        ) {
            if (selectedCell.row >= 0 && selectedCell.col >= 0) {
                // current cell
                drawRect(
                    color = highlightColor.copy(alpha = 0.3f),
                    topLeft = Offset(
                        x = selectedCell.col * cellSize,
                        y = selectedCell.row * cellSize
                    ),
                    size = Size(cellSize, cellSize)
                )
                if (positionLines) {
                    // vertical position line
                    drawRect(
                        color = highlightColor.copy(alpha = 0.1f),
                        topLeft = Offset(
                            x = selectedCell.col * cellSize,
                            y = 0f
                        ),
                        size = Size(cellSize, maxWidth)
                    )
                    // horizontal position line
                    drawRect(
                        color = highlightColor.copy(alpha = 0.1f),
                        topLeft = Offset(
                            x = 0f,
                            y = selectedCell.row * cellSize
                        ),
                        size = Size(maxWidth, cellSize)
                    )
                }
            }
            if (identicalNumbersHighlight) {
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        if (board[i][j].value == selectedCell.value && board[i][j].value != 0) {
                            drawRect(
                                color = highlightColor.copy(alpha = 0.3f),
                                topLeft = Offset(
                                    x = board[i][j].col * cellSize,
                                    y = board[i][j].row * cellSize
                                ),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
            cellsToHighlight?.forEach {
                drawRect(
                    color = highlightColor.copy(alpha = 0.5f),
                    topLeft = Offset(
                        x = it.col * cellSize,
                        y = it.row * cellSize
                    ),
                    size = Size(cellSize, cellSize)
                )
            }

            // frame field
            drawRoundRect(
                color = foregroundColor.copy(alpha = 0.8f),
                topLeft = Offset(0f, 0f),
                size = Size(maxWidth, maxWidth),
                cornerRadius = CornerRadius(15f, 15f),
                style = Stroke(width = 8f)
            )

            // horizontal line
            for (i in 1 until size) {
                val isThickLine = i % horThick == 0
                drawLine(
                    color = if (isThickLine) {
                        foregroundColor.copy(0.8f)
                    } else {
                        foregroundColor.copy(0.6f)
                    },
                    start = Offset(cellSize * i.toFloat(), 0f),
                    end = Offset(cellSize * i.toFloat(), maxWidth),
                    strokeWidth = if (isThickLine) 8f else 5f
                )
            }
            // vertical line
            for (i in 1 until size) {
                val isThickLine = i % vertThick == 0
                if (maxWidth >= cellSize * i) {
                    drawLine(
                        color = if (isThickLine) {
                            foregroundColor.copy(0.8f)
                        } else {
                            foregroundColor.copy(0.6f)
                        },
                        start = Offset(0f, cellSize * i.toFloat()),
                        end = Offset(maxWidth, cellSize * i.toFloat()),
                        strokeWidth = if (isThickLine) 8f else 5f
                    )
                }
            }

            val textBounds = Rect()
            textPaint.getTextBounds("1", 0, 1, textBounds)

            // numbers
            drawIntoCanvas { canvas ->
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        if (board[i][j].value != 0) {
                            val paint = when {
                                board[i][j].error && errorsHighlight -> errorTextPaint
                                board[i][j].locked -> lockedTextPaint
                                else -> textPaint
                            }
                            canvas.nativeCanvas.drawText(
                                if (questions) "?" else board[i][j].value.toString(),
                                board[i][j].col * cellSize + (cellSize - width) / 2f,
                                (board[i][j].row * cellSize + cellSize) - (cellSize - textBounds.height()) / 2f,
                                paint
                            )
                        }
                    }
                }
            }

            // notes
            if (!notes.isNullOrEmpty() && !questions && renderNotes) {
                val noteBounds = Rect()
                notePaint.getTextBounds("1", 0, 1, noteBounds)
                val noteWidthHalf = notePaint.measureText("1") / 2f

                drawIntoCanvas { canvas ->
                    notes.forEach { note ->
                        val widthOffset = when (note.value) {
                            1, 4, 7 -> 0
                            2, 5, 8 -> 1
                            3, 6, 9 -> 2
                            else -> 0
                        }
                        val heightOffset = when (note.value) {
                            1, 2, 3 -> 0
                            4, 5, 6 -> 1
                            7, 8, 9 -> 2
                            else -> 0
                        }
                        canvas.nativeCanvas.drawText(
                            note.value.toString(),
                            note.col * cellSize + cellSizeDiv / 2f + (cellSizeDiv * widthOffset) - noteWidthHalf,
                            note.row * cellSize + cellSizeDiv / 2f + (cellSizeDiv * heightOffset) + noteBounds.height() / 2f,
                            notePaint
                        )
                    }
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun BoardPreviewLight() {
    LibreSudokuTheme {
        Surface {
            val sudokuParser = SudokuParser()
            val board by remember {
                mutableStateOf(
                    sudokuParser.parseBoard(
                        board = "....1........4.............7...........9........68...............5...............",
                        gameType = GameType.Default9x9,
                        emptySeparator = '.'
                    ).toList()
                )
            }
            val notes = listOf(Note(2, 3, 1), Note(2, 3, 5))
            Board(
                board = board,
                notes = notes,
                selectedCell = Cell(-1, -1),
                onClick = { }
            )
        }
    }
}