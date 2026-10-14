package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.util.lerp
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColors

/**
 * Draws a checkerboard of tinted boxes to make them easier to tell apart
 *
 * @param geometry board dimensions and box layout in pixels
 * @param color box fill color including its opacity
 */
fun DrawScope.drawCrossHighlight(geometry: BoardGeometry, color: Color) {
    if (color.alpha == 0f) return
    val boxSize = Size(geometry.boxWidth * geometry.cellSize, geometry.boxHeight * geometry.cellSize)
    for (boxRow in 0 until geometry.gameSize / geometry.boxHeight) {
        for (boxCol in 0 until geometry.gameSize / geometry.boxWidth) {
            if ((boxRow + boxCol) % 2 == 1) {
                drawRect(
                    color = color,
                    topLeft = Offset(boxCol * boxSize.width, boxRow * boxSize.height),
                    size = boxSize
                )
            }
        }
    }
}

/**
 * Draws the selected cell and optionally its row and column
 *
 * Row and column fills overlap the selected cell, making its background more saturated
 *
 * @param geometry board and cell dimensions in pixels
 * @param selection animated cell position and visibility
 * @param cellColor selected cell fill color before applying animated visibility
 * @param lineColor row and column fill color, or null to omit those highlights
 */
fun DrawScope.drawSelection(
    geometry: BoardGeometry,
    selection: SelectionAnimation,
    cellColor: Color,
    lineColor: Color?
) {
    val visibility = selection.visibility.value
    if (visibility <= 0f) return
    val cellSize = geometry.cellSize
    val topLeft = selection.position.value * cellSize

    if (lineColor != null) {
        val color = lineColor.copy(alpha = lineColor.alpha * visibility)
        drawRect(color, topLeft = Offset(topLeft.x, 0f), size = Size(cellSize, geometry.boardSize))
        drawRect(color, topLeft = Offset(0f, topLeft.y), size = Size(geometry.boardSize, cellSize))
    }
    drawRect(
        color = cellColor.copy(alpha = cellColor.alpha * visibility),
        topLeft = topLeft,
        size = Size(cellSize, cellSize)
    )
}

/**
 * Draws cell backgrounds that fade in and out with their set
 *
 * @param geometry board and cell dimensions in pixels
 * @param cells cell positions with their current visibility fractions
 * @param color cell fill color before applying animated visibility
 */
fun DrawScope.drawCellHighlights(
    geometry: BoardGeometry,
    cells: AnimatedSet<CellPosition>,
    color: Color
) {
    cells.forEach { cell, entry ->
        drawRect(
            color = color.copy(alpha = color.alpha * entry.fraction.coerceIn(0f, 1f)),
            topLeft = geometry.cellTopLeft(cell),
            size = Size(geometry.cellSize, geometry.cellSize)
        )
    }
}

/**
 * Draws cell and box borders and the rounded frame around the board
 *
 * @param geometry board dimensions and box layout in pixels
 * @param thinLineColor color of borders between cells within a box
 * @param thickLineColor color of box borders and the outer frame
 * @param lineWidth stroke width in pixels for all grid lines and the frame
 * @param cornerRadius outer frame corner radii in pixels
 */
fun DrawScope.drawGrid(
    geometry: BoardGeometry,
    thinLineColor: Color,
    thickLineColor: Color,
    lineWidth: Float,
    cornerRadius: CornerRadius
) {
    // Thin lines go first, so box borders run uninterrupted on top of them
    drawGridLines(geometry, thinLineColor, lineWidth, boxBorders = false)
    drawGridLines(geometry, thickLineColor, lineWidth, boxBorders = true)
    drawRoundRect(
        color = thickLineColor,
        size = Size(geometry.boardSize, geometry.boardSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = lineWidth)
    )
}

private fun DrawScope.drawGridLines(
    geometry: BoardGeometry,
    color: Color,
    lineWidth: Float,
    boxBorders: Boolean
) {
    for (i in 1 until geometry.gameSize) {
        val position = i * geometry.cellSize
        if ((i % geometry.boxWidth == 0) == boxBorders) {
            drawLine(
                color = color,
                start = Offset(position, 0f),
                end = Offset(position, geometry.boardSize),
                strokeWidth = lineWidth
            )
        }
        if ((i % geometry.boxHeight == 0) == boxBorders) {
            drawLine(
                color = color,
                start = Offset(0f, position),
                end = Offset(geometry.boardSize, position),
                strokeWidth = lineWidth
            )
        }
    }
}

/**
 * Draws digits with animated entry, removal, error color and horizontal shake
 *
 * @param geometry board and cell dimensions in pixels
 * @param animations digit visibility, error highlighting and shake state
 * @param glyphs painter configured for the main digit text size
 * @param colors foreground, locked-digit and error colors
 * @param questions whether to draw a question mark instead of each digit
 * @param isLocked lookup indicating whether the cell at a position contains a locked clue
 */
fun DrawScope.drawDigits(
    geometry: BoardGeometry,
    animations: BoardAnimations,
    glyphs: GlyphPainter,
    colors: SudokuBoardColors,
    questions: Boolean,
    isLocked: (CellPosition) -> Boolean
) {
    animations.digits.forEach { digit, entry ->
        val cell = digit.position
        val fraction = entry.fraction
        val digitScale: Float
        val alpha: Float
        if (entry.isEntering) {
            digitScale = lerp(BoardMotion.DigitEnterFromScale, 1f, fraction)
            // Opaque halfway through, so the bounce reads as a solid digit, not a blinking one
            alpha = (fraction * 2f).coerceIn(0f, 1f)
        } else {
            digitScale = lerp(BoardMotion.DigitExitToScale, 1f, fraction)
            alpha = fraction.coerceIn(0f, 1f)
        }

        val baseColor = if (isLocked(cell)) colors.altForegroundColor else colors.foregroundColor
        val color = lerp(baseColor, colors.errorColor, animations.errors.fractionOf(cell).coerceIn(0f, 1f))
        val shift = animations.shakeOf(cell) * BoardMotion.ErrorShakeAmplitude * geometry.cellSize
        val position = geometry.cellCenter(cell) + Offset(shift, 0f)

        scale(digitScale, pivot = position) {
            drawGlyph(
                painter = glyphs,
                text = if (questions) "?" else digitLabel(digit.value),
                center = position,
                color = color.copy(alpha = color.alpha * alpha)
            )
        }
    }
}

/**
 * Draws pencil marks with a circle behind the highlighted ones
 *
 * @param animations note and highlight visibility state
 * @param layout note slot positions and highlight radius in pixels
 * @param glyphs painter configured for the note text size
 * @param color note text color before applying animated visibility
 * @param highlightColor circle fill color before applying animated visibility
 */
fun DrawScope.drawNotes(
    animations: BoardAnimations,
    layout: NoteLayout,
    glyphs: GlyphPainter,
    color: Color,
    highlightColor: Color
) {
    // Circles go first, so a circle never covers a neighboring note
    animations.highlightedNotes.forEach { note, entry ->
        val fraction = entry.fraction
        drawCircle(
            color = highlightColor,
            radius = layout.highlightRadius * fraction.coerceAtLeast(0f),
            center = layout.center(note),
            alpha = fraction.coerceIn(0f, 1f)
        )
    }
    animations.notes.forEach { note, entry ->
        val fraction = entry.fraction.coerceIn(0f, 1f)
        val position = layout.center(note)
        scale(lerp(BoardMotion.NoteEnterFromScale, 1f, fraction), pivot = position) {
            drawGlyph(
                painter = glyphs,
                text = digitLabel(note.value),
                center = position,
                color = color.copy(alpha = color.alpha * fraction)
            )
        }
    }
}
