package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import android.util.TypedValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColors
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
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
        12 -> 24.sp
        else -> 14.sp
    },
    noteTextSize: TextUnit = when (size) {
        6 -> 18.sp
        9 -> 12.sp
        12 -> 7.sp
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
    zoomable: Boolean = false,
    boardColors: SudokuBoardColors = LocalBoardColors.current,
    crossHighlight: Boolean = false,
    cages: List<Cage> = emptyList()
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(4.dp)
    ) {
        val maxWidth = constraints.maxWidth.toFloat()

        // single cell size
        val cellSize by remember(size) { mutableFloatStateOf(maxWidth / size.toFloat()) }
        // div for notes in one row in cell
        val cellSizeDivWidth by remember(size) { mutableFloatStateOf(cellSize / ceil(sqrt(size.toFloat()))) }
        // div for note in one column in cell
        val cellSizeDivHeight by remember(size) { mutableFloatStateOf(cellSize / floor(sqrt(size.toFloat()))) }

        val errorColor = boardColors.errorColor
        val foregroundColor = boardColors.foregroundColor
        val thickLineColor = boardColors.thickLineColor
        val thinLineColor = boardColors.thinLineColor
        // locked numbers
        val altForegroundColor = boardColors.altForegroundColor
        val notesColor = boardColors.notesColor

        // highlight (cells)
        val highlightColor = boardColors.highlightColor

        val vertThick by remember(size) { mutableIntStateOf(floor(sqrt(size.toFloat())).toInt()) }
        val horThick by remember(size) { mutableIntStateOf(ceil(sqrt(size.toFloat())).toInt()) }

        var fontSizePx = with(LocalDensity.current) { mainTextSize.toPx() }
        var noteSizePx = with(LocalDensity.current) { noteTextSize.toPx() }
        var killerSumSizePx = with(LocalDensity.current) { noteTextSize.toPx() * 0.9f }

        val thinLineWidth = with(LocalDensity.current) { 1.3.dp.toPx() }
        val thickLineWidth = with(LocalDensity.current) { 1.3.dp.toPx() }

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
                    color = errorColor.toArgb()
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
        var notePaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = notesColor.toArgb()
                    isAntiAlias = true
                    textSize = noteSizePx
                }
            )
        }

        var killerSumPaint by remember {
            mutableStateOf(
                Paint().apply {
                    color = notesColor.toArgb()
                    isAntiAlias = true
                    textSize = killerSumSizePx
                }
            )
        }

        val context = LocalContext.current
        LaunchedEffect(mainTextSize, noteTextSize, boardColors) {
            fontSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                mainTextSize.value,
                context.resources.displayMetrics
            )
            noteSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                noteTextSize.value,
                context.resources.displayMetrics
            )
            killerSumSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                noteTextSize.value * 0.9f,
                context.resources.displayMetrics
            )
            textPaint = Paint().apply {
                color = foregroundColor.toArgb()
                isAntiAlias = true
                textSize = fontSizePx
            }
            notePaint = Paint().apply {
                color = notesColor.toArgb()
                isAntiAlias = true
                textSize = noteSizePx
            }
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
            killerSumPaint = Paint().apply {
                color = notesColor.toArgb()
                isAntiAlias = true
                textSize = killerSumSizePx
            }
        }

        var zoom by remember(enabled) { mutableFloatStateOf(1f) }
        var offset by remember(enabled) { mutableStateOf(Offset.Zero) }

        val boardModifier = Modifier
            .fillMaxSize()
            .pointerInput(key1 = enabled, key2 = board) {
                detectTapGestures(
                    onTap = {
                        if (enabled) {
                            val totalOffset = it / zoom + offset
                            val row =
                                floor((totalOffset.y) / cellSize)
                                    .toInt()
                                    .coerceIn(board.indices)
                            val column =
                                floor((totalOffset.x) / cellSize)
                                    .toInt()
                                    .coerceIn(board.indices)
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

        val zoomModifier = Modifier
            .pointerInput(enabled) {
                detectTransformGestures(
                    onGesture = { gestureCentroid, gesturePan, gestureZoom, _ ->
                        if (enabled) {
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
            .graphicsLayer {
                translationX = -offset.x * zoom
                translationY = -offset.y * zoom
                scaleX = zoom
                scaleY = zoom
                TransformOrigin(0f, 0f).also { transformOrigin = it }
            }
        Canvas(
            modifier = if (zoomable) boardModifier.then(zoomModifier) else boardModifier
        ) {
            val cornerRadius = CornerRadius(15f, 15f)

            if (selectedCell.row >= 0 && selectedCell.col >= 0) {
                // current cell
                drawRoundCell(
                    row = selectedCell.row,
                    col = selectedCell.col,
                    gameSize = size,
                    rect = Rect(
                        offset = Offset(
                            x = selectedCell.col * cellSize,
                            y = selectedCell.row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    ),
                    color = highlightColor.copy(alpha = 0.2f),
                    cornerRadius = cornerRadius
                )
                if (positionLines) {
                    drawPositionLines(
                        row = selectedCell.row,
                        col = selectedCell.col,
                        gameSize = size,
                        color = highlightColor.copy(alpha = 0.1f),
                        cellSize = cellSize,
                        lineLength = maxWidth,
                        cornerRadius = cornerRadius
                    )
                }
            }
            if (identicalNumbersHighlight) {
                for (i in 0 until size) {
                    for (j in 0 until size) {
                        if (board[i][j].value == selectedCell.value && board[i][j].value != 0) {
                            drawRoundCell(
                                row = board[i][j].row,
                                col = board[i][j].col,
                                gameSize = size,
                                rect = Rect(
                                    offset = Offset(
                                        x = board[i][j].col * cellSize,
                                        y = board[i][j].row * cellSize
                                    ),
                                    size = Size(cellSize, cellSize)
                                ),
                                color = highlightColor.copy(alpha = 0.2f),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                }
            }
            cellsToHighlight?.forEach {
                drawRoundCell(
                    row = it.row,
                    col = it.col,
                    gameSize = size,
                    color = highlightColor.copy(alpha = 0.3f),
                    rect = Rect(
                        Offset(
                            x = it.col * cellSize,
                            y = it.row * cellSize
                        ),
                        size = Size(cellSize, cellSize)
                    ),
                    cornerRadius = cornerRadius
                )
            }

            drawBoardFrame(
                thickLineColor = thickLineColor,
                thickLineWidth = thickLineWidth,
                maxWidth = maxWidth,
                cornerRadius = CornerRadius(15f, 15f)
            )

            // horizontal line
            for (i in 1 until size) {
                val isThickLine = i % horThick == 0
                drawLine(
                    color = if (isThickLine) thickLineColor else thinLineColor,
                    start = Offset(cellSize * i.toFloat(), 0f),
                    end = Offset(cellSize * i.toFloat(), maxWidth),
                    strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                )
            }
            // vertical line
            for (i in 1 until size) {
                val isThickLine = i % vertThick == 0
                if (maxWidth >= cellSize * i) {
                    drawLine(
                        color = if (isThickLine) thickLineColor else thinLineColor,
                        start = Offset(0f, cellSize * i.toFloat()),
                        end = Offset(maxWidth, cellSize * i.toFloat()),
                        strokeWidth = if (isThickLine) thickLineWidth else thinLineWidth
                    )
                }
            }

            drawNumbers(
                size = size,
                board = board,
                highlightErrors = errorsHighlight,
                errorTextPaint = errorTextPaint,
                lockedTextPaint = lockedTextPaint,
                textPaint = textPaint,
                questions = questions,
                cellSize = cellSize
            )

            if (!notes.isNullOrEmpty() && !questions && renderNotes) {
                drawNotes(
                    size = size,
                    paint = notePaint,
                    notes = notes,
                    cellSize = cellSize,
                    cellSizeDivWidth = cellSizeDivWidth,
                    cellSizeDivHeight = cellSizeDivHeight
                )
            }

            // doesn't look good on 6x6
            if (crossHighlight && size != 6) {
                val sectionHeight = getSectionHeightForSize(size)
                val sectionWidth = getSectionWidthForSize(size)
                for (i in 0 until size / sectionWidth) {
                    for (j in 0 until size / sectionHeight) {
                        if ((i % 2 == 0 && j % 2 != 0) || (i % 2 != 0 && j % 2 == 0)) {
                            drawRect(
                                color = highlightColor.copy(alpha = 0.1f),
                                topLeft = Offset(
                                    x = i * sectionWidth * cellSize,
                                    y = j * sectionHeight * cellSize
                                ),
                                size = Size(cellSize * sectionWidth, cellSize * sectionHeight)
                            )
                        }
                    }
                }
            }

            if (cages.isNotEmpty()) {
                cages.forEach { cage ->
                    val noteBounds = android.graphics.Rect()
                    val textToDraw = cage.sum.toString()
                    killerSumPaint.getTextBounds(textToDraw, 0, textToDraw.length, noteBounds)
                    // get top left cell
                    val cellWithSum = cage.cells.minWith(
                        compareBy<Cell> { it.row }.thenBy { it.col }
                    )
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            textToDraw,
                            cellWithSum.col * cellSize + (cellSize * 0.05f),
                            cellWithSum.row * cellSize + noteBounds.height() + (cellSize * 0.05f),
                            killerSumPaint
                        )
                    }

                    drawKillerCage(
                        cage = cage,
                        cellWithSum = cellWithSum,
                        cellSize = cellSize,
                        strokeWidth = thickLineWidth,
                        color = thinLineColor,
                        cornerTextPadding = Offset(noteBounds.width().toFloat(), noteBounds.height().toFloat())
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBoardFrame(
    thickLineColor: Color,
    thickLineWidth: Float,
    maxWidth: Float,
    cornerRadius: CornerRadius
) {
    drawRoundRect(
        color = thickLineColor,
        topLeft = Offset.Zero,
        size = Size(maxWidth, maxWidth),
        cornerRadius = cornerRadius,
        style = Stroke(width = thickLineWidth)
    )
}

private fun DrawScope.drawNumbers(
    size: Int,
    board: List<List<Cell>>,
    highlightErrors: Boolean,
    errorTextPaint: Paint,
    lockedTextPaint: Paint,
    textPaint: Paint,
    questions: Boolean,
    cellSize: Float
) {
    drawIntoCanvas { canvas ->
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (board[i][j].value != 0) {
                    val paint = when {
                        board[i][j].error && highlightErrors -> errorTextPaint
                        board[i][j].locked -> lockedTextPaint
                        else -> textPaint
                    }

                    val textToDraw =
                        if (questions) "?" else board[i][j].value.toString(16).uppercase()
                    val textBounds = android.graphics.Rect()
                    textPaint.getTextBounds(textToDraw, 0, 1, textBounds)
                    val textWidth = paint.measureText(textToDraw)

                    canvas.nativeCanvas.drawText(
                        textToDraw,
                        board[i][j].col * cellSize + (cellSize - textWidth) / 2f,
                        board[i][j].row * cellSize + (cellSize + textBounds.height()) / 2f,
                        paint
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawNotes(
    size: Int,
    paint: Paint,
    notes: List<Note>,
    cellSize: Float,
    cellSizeDivWidth: Float,
    cellSizeDivHeight: Float
) {
    val noteBounds = android.graphics.Rect()
    paint.getTextBounds("1", 0, 1, noteBounds)

    drawIntoCanvas { canvas ->
        notes.forEach { note ->
            val textToDraw = note.value.toString(16).uppercase()
            val noteTextMeasure = paint.measureText(textToDraw)
            canvas.nativeCanvas.drawText(
                textToDraw,
                note.col * cellSize + cellSizeDivWidth / 2f + (cellSizeDivWidth * getNoteRowNumber(
                    note.value,
                    size
                )) - noteTextMeasure / 2f,
                note.row * cellSize + cellSizeDivHeight / 2f + (cellSizeDivHeight * getNoteColumnNumber(
                    note.value,
                    size
                )) + noteBounds.height() / 2f,
                paint
            )
        }
    }
}

private fun DrawScope.drawRoundCell(
    row: Int,
    col: Int,
    gameSize: Int,
    rect: Rect,
    color: Color,
    cornerRadius: CornerRadius = CornerRadius.Zero
) {
    val path = Path().apply {
        addRoundRect(
            roundRectForCell(
                row = row,
                col = col,
                gameSize = gameSize,
                rect = rect,
                cornerRadius = cornerRadius
            )
        )
    }
    drawPath(
        path = path,
        color = color
    )
}

private fun roundRectForCell(
    row: Int,
    col: Int,
    gameSize: Int,
    rect: Rect,
    cornerRadius: CornerRadius
): RoundRect {
    val topLeft = if (row == 0 && col == 0) cornerRadius  else CornerRadius.Zero
    val topRight = if (row == 0 && col == gameSize - 1)cornerRadius else CornerRadius.Zero
    val bottomLeft = if (row == gameSize - 1 && col == 0) cornerRadius else CornerRadius.Zero
    val bottomRight = if (row == gameSize - 1 && col == gameSize - 1) cornerRadius else CornerRadius.Zero

    return RoundRect(
        rect = rect,
        topLeft = topLeft,
        topRight = topRight,
        bottomLeft = bottomLeft,
        bottomRight = bottomRight
    )
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
                onClick = { },
                boardColors = SudokuBoardColorsImpl(
                    foregroundColor = BoardColors.foregroundColor,
                    notesColor = BoardColors.notesColor,
                    altForegroundColor = BoardColors.altForegroundColor,
                    errorColor = BoardColors.errorColor,
                    highlightColor = BoardColors.highlightColor,
                    thickLineColor = BoardColors.thickLineColor,
                    thinLineColor = BoardColors.thinLineColor
                )
            )
        }
    }
}