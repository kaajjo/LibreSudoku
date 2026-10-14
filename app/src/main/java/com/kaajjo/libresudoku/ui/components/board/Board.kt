package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.Cage
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.CROSS
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.HINT
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.NOTE
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.POSITION_LINES
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.SAME_NUMBER
import com.kaajjo.libresudoku.ui.components.board.HighlightAlpha.SELECTED_CELL
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColors
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import com.kaajjo.libresudoku.ui.util.LightDarkPreview

/**
 * Interactive Sudoku board with animated digits, notes and highlights
 *
 * @param modifier layout, drawing and input modifiers applied to the board
 * @param board square matrix of cells with values, lock states and error flags
 * @param size number of rows and columns, supported values are 6, 9 and 12
 * @param notes pencil marks to display, or null when there are none
 * @param mainTextSize size of the main digits when [autoFontSize] is false
 * @param autoFontSize whether to derive the main digit size from the cell size instead of [mainTextSize]
 * @param selectedCell selected cell, or a cell with negative coordinates when no cell is selected
 * @param onClick callback receiving the tapped cell, including when [enabled] is false
 * @param onLongClick callback receiving the long-pressed cell when [enabled] is true
 * @param identicalNumbersHighlight whether to highlight filled cells matching [selectedCell]'s value
 * @param errorsHighlight whether to highlight cells marked as errors
 * @param positionLines whether to highlight the selected cell's row and column
 * @param enabled whether long presses and zoom gestures are accepted, while taps are always reported
 * @param questions whether to replace digits and cage sums with question marks and hide notes
 * @param renderNotes whether to display notes when [questions] is false
 * @param cellsToHighlight additional cells highlighted by a hint or tutorial
 * @param notesToHighlight visible pencil marks that should receive a circular highlight
 * @param zoomable whether to install zoom and pan handling and clip the board to its viewport
 * @param boardColors colors used for text, grid lines and highlights
 * @param crossHighlight whether to tint alternating boxes, ignored for 6x6 boards
 * @param cages Killer Sudoku cages whose outlines and sums should be drawn
 */
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
    autoFontSize: Boolean = false,
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
    notesToHighlight: List<Note> = emptyList(),
    zoomable: Boolean = false,
    boardColors: SudokuBoardColors = LocalBoardColors.current,
    crossHighlight: Boolean = false,
    cages: List<Cage> = emptyList()
) {
    val gameSize = size
    val content = boardContent(
        board = board,
        gameSize = gameSize,
        notes = if (renderNotes && !questions) notes.orEmpty() else emptyList(),
        selectedCell = selectedCell,
        highlightSameNumbers = identicalNumbersHighlight,
        highlightErrors = errorsHighlight,
        cellsToHighlight = cellsToHighlight.orEmpty(),
        notesToHighlight = notesToHighlight
    )

    val animations = rememberBoardAnimations()

    // Runs after composition but before the frame is drawn
    SideEffect { animations.update(content) }
    LaunchedEffect(content.selected) {
        animations.selection.animateTo(content.selected)
    }

    val zoomState = remember { BoardZoomState() }
    LaunchedEffect(enabled) {
        if (!enabled) zoomState.reset()
    }

    val crossHighlightAlpha = animateFloatAsState(
        // doesn't look good on 6x6
        targetValue = if (crossHighlight && gameSize != 6) 1f else 0f,
        label = "Cross highlight"
    )

    val currentBoard by rememberUpdatedState(board)
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (zoomable) Modifier.clipToBounds() else Modifier)
            .padding(4.dp)
            .then(if (zoomable) Modifier.boardZoom(zoomState, enabled) else Modifier)
            .pointerInput(gameSize, enabled) {
                fun cellAt(position: Offset): Cell? {
                    if (currentBoard.isEmpty()) return null
                    val cell = BoardGeometry(gameSize, this.size.width.toFloat()).cellAt(position)
                    return currentBoard[cell.row][cell.col]
                }
                detectTapGestures(
                    onTap = { position -> cellAt(position)?.let(currentOnClick) },
                    onLongPress = if (enabled) {
                        { position -> cellAt(position)?.let(currentOnLongClick) }
                    } else {
                        null
                    }
                )
            }
            .drawWithCache {
                val boardRect = this.size.toRect()
                val geometry = BoardGeometry(gameSize, boardRect.width)
                val digitGlyphs = GlyphPainter(
                    textSize = if (autoFontSize) geometry.cellSize * AutoFontScale else mainTextSize.toPx()
                )
                val noteTextSize = geometry.cellSize / geometry.boxWidth * NoteFontScale
                val noteGlyphs = GlyphPainter(noteTextSize)
                val cageSumGlyphs = GlyphPainter(noteTextSize * CageSumFontScale)
                val noteLayout = NoteLayout(
                    geometry = geometry,
                    topReserve = if (cages.isEmpty()) 0f else cageSumGlyphs.inkBounds("0").height
                )
                val cornerRadius = CornerRadius(BoardCornerRadius.toPx())
                val outline = Path().apply { addRoundRect(RoundRect(boardRect, cornerRadius)) }
                val lineWidth = BoardLineWidth.toPx()
                val highlight = boardColors.highlightColor

                onDrawBehind {
                    clipPath(outline) {
                        drawCrossHighlight(
                            geometry = geometry,
                            color = highlight.copy(alpha = HighlightAlpha.CROSS * crossHighlightAlpha.value)
                        )
                        drawSelection(
                            geometry = geometry,
                            selection = animations.selection,
                            cellColor = highlight.copy(alpha = HighlightAlpha.SELECTED_CELL),
                            lineColor = if (positionLines) highlight.copy(alpha = HighlightAlpha.POSITION_LINES) else null
                        )
                        drawCellHighlights(
                            geometry = geometry,
                            cells = animations.sameNumberCells,
                            color = highlight.copy(alpha = HighlightAlpha.SAME_NUMBER)
                        )
                        drawCellHighlights(
                            geometry = geometry,
                            cells = animations.hintCells,
                            color = highlight.copy(alpha = HighlightAlpha.HINT)
                        )
                    }
                    drawGrid(
                        geometry = geometry,
                        thinLineColor = boardColors.thinLineColor,
                        thickLineColor = boardColors.thickLineColor,
                        lineWidth = lineWidth,
                        cornerRadius = cornerRadius
                    )
                    drawDigits(
                        geometry = geometry,
                        animations = animations,
                        glyphs = digitGlyphs,
                        colors = boardColors,
                        questions = questions,
                        isLocked = { cell ->
                            board.getOrNull(cell.row)?.getOrNull(cell.col)?.locked == true
                        }
                    )
                    drawNotes(
                        animations = animations,
                        layout = noteLayout,
                        glyphs = noteGlyphs,
                        color = boardColors.notesColor,
                        highlightColor = highlight.copy(alpha = HighlightAlpha.NOTE)
                    )
                    drawCages(
                        cages = cages,
                        geometry = geometry,
                        sumGlyphs = cageSumGlyphs,
                        lineColor = boardColors.thinLineColor,
                        sumColor = boardColors.notesColor,
                        lineWidth = lineWidth,
                        questions = questions
                    )
                }
            }
    )
}

private val BoardCornerRadius = 5.dp
private val BoardLineWidth = 1.3.dp

// Text sizes, relative to the cell
private const val AutoFontScale = 0.9f
private const val NoteFontScale = 0.8f // of a note slot
private const val CageSumFontScale = 1.1f // of a note

/**
 * Alpha of the highlight color for each kind of highlight
 */
private object HighlightAlpha {
    const val SELECTED_CELL = 0.2f
    const val POSITION_LINES = 0.1f
    const val SAME_NUMBER = 0.2f
    const val HINT = 0.3f
    const val NOTE = 0.3f
    const val CROSS = 0.1f
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
