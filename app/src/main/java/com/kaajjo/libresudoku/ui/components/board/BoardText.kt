package com.kaajjo.libresudoku.ui.components.board

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeRect

/**
 * Draws short labels such as digits, notes and cage sums at a fixed text size
 *
 * Labels are positioned by their ink bounds rather than by font metrics, so a digit sits optically
 * in the middle of its cell using bounds measured by the platform [Paint]
 *
 * @param textSize text size in pixels, shared by all labels drawn by this painter
 */
class GlyphPainter(textSize: Float) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).also {
        it.textSize = textSize
    }
    private val inkBoundsCache = HashMap<String, Rect>()

    /**
     * Ink bounds of [text], relative to the start of its baseline
     *
     * @param text label to measure using this painter's fixed text size
     * @return cached bounds of the visible glyphs in pixels
     */
    fun inkBounds(text: String): Rect = inkBoundsCache.getOrPut(text) {
        android.graphics.Rect()
            .also { paint.getTextBounds(text, 0, text.length, it) }
            .toComposeRect()
    }

    /**
     * Draws a label at the given baseline origin
     *
     * @param scope drawing scope whose native canvas receives the text
     * @param text label to draw
     * @param baselineStart baseline origin in local canvas pixels
     * @param color text color including its opacity
     */
    fun draw(scope: DrawScope, text: String, baselineStart: Offset, color: Color) {
        paint.color = color.toArgb()
        scope.drawContext.canvas.nativeCanvas.drawText(text, baselineStart.x, baselineStart.y, paint)
    }
}

/**
 * Draws [text] so that the center of its visible bounds lands on [center]
 *
 * @param painter painter that supplies the text size and cached glyph bounds
 * @param text label to draw
 * @param center desired center of the visible glyphs in local canvas pixels
 * @param color text color including its opacity
 */
fun DrawScope.drawGlyph(painter: GlyphPainter, text: String, center: Offset, color: Color) {
    painter.draw(this, text, center - painter.inkBounds(text).center, color)
}

/**
 * Draws [text] with the top-left corner of its visible bounds at [topLeft]
 *
 * @param painter painter that supplies the text size and cached glyph bounds
 * @param text label to draw
 * @param topLeft desired top-left corner of the visible glyphs in local canvas pixels
 * @param color text color including its opacity
 */
fun DrawScope.drawGlyphAt(painter: GlyphPainter, text: String, topLeft: Offset, color: Color) {
    painter.draw(this, text, topLeft - painter.inkBounds(text).topLeft, color)
}

private val digitLabels = Array(17) { it.toString(16).uppercase() }

/**
 * Formats a cell value as an uppercase hexadecimal label
 *
 * @param value cell value, normally 1 through the board size
 * @return digit label, with A, B and C representing 10, 11 and 12 on 12x12 boards
 */
fun digitLabel(value: Int): String = digitLabels.getOrElse(value) { value.toString(16).uppercase() }
