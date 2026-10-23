package com.kaajjo.libresudoku.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Immutable
data class MetalPalette(
    val base: Color,
    val sheen: Color,
    val streakLight: Color,
    val streakDark: Color,
    val edge: Color,
    val screwLight: Color,
    val screwDark: Color,
    val content: Color
) {
    companion object {
        val Light = MetalPalette(
            base = Color(0xFFB4BBC1),
            sheen = Color(0xFFC8CDD2),
            streakLight = Color.White,
            streakDark = Color(0xFF6E767E),
            edge = Color(0xFF858C93),
            screwLight = Color(0xFFEDEFF1),
            screwDark = Color(0xFF7A828A),
            content = Color(0xFF1C2024)
        )
        val Dark = MetalPalette(
            base = Color(0xFF474D53),
            sheen = Color(0xFF575E65),
            streakLight = Color(0xFFB8C0C8),
            streakDark = Color.Black,
            edge = Color(0xFF26292D),
            screwLight = Color(0xFFADB5BC),
            screwDark = Color(0xFF363B40),
            content = Color(0xFFE6E9EC)
        )
    }
}

@Composable
fun rememberMetalPalette(): MetalPalette =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) MetalPalette.Dark else MetalPalette.Light

private class Streak(val y: Float, val start: Float, val length: Float, val color: Color, val width: Float)

/**
 * Brushed steel background: a silver base with fine horizontal streaks.
 * Streaks are generated with a fixed seed so the texture doesn't flicker between recompositions.
 */
fun Modifier.brushedMetal(palette: MetalPalette): Modifier = drawWithCache {
    val random = Random(42)
    val streaks = List((size.height / 1.2f).toInt().coerceIn(0, 4000)) {
        val light = random.nextFloat() < 0.55f
        Streak(
            y = random.nextFloat() * size.height,
            start = random.nextFloat() * size.width * 1.2f - size.width * 0.2f,
            length = size.width * (0.15f + random.nextFloat() * 0.9f),
            color = if (light) {
                palette.streakLight.copy(alpha = 0.12f + random.nextFloat() * 0.38f)
            } else {
                palette.streakDark.copy(alpha = 0.05f + random.nextFloat() * 0.15f)
            },
            width = 0.8f + random.nextFloat() * 1.6f
        )
    }
    val baseBrush = Brush.linearGradient(
        colors = listOf(palette.sheen, palette.base, palette.sheen, palette.base),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    onDrawBehind {
        drawRect(baseBrush)
        streaks.forEach {
            drawLine(
                color = it.color,
                start = Offset(it.start, it.y),
                end = Offset(it.start + it.length, it.y),
                strokeWidth = it.width
            )
        }
    }
}

/** Draws a screw in each corner, [inset] away from the edges. */
fun Modifier.metalScrews(
    palette: MetalPalette,
    inset: Dp = 14.dp,
    radius: Dp = 6.dp
): Modifier = drawBehind {
    val i = inset.toPx()
    val r = radius.toPx()
    listOf(
        Offset(i, i) to 20f,
        Offset(size.width - i, i) to 70f,
        Offset(i, size.height - i) to 125f,
        Offset(size.width - i, size.height - i) to 160f
    ).forEach { (center, angle) -> drawScrew(center, r, angle, palette) }
}

private fun DrawScope.drawScrew(center: Offset, radius: Float, angleDegrees: Float, palette: MetalPalette) {
    drawCircle(Color.Black.copy(alpha = 0.3f), radius, center + Offset(radius * 0.15f, radius * 0.2f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(palette.screwLight, palette.screwDark),
            center = center - Offset(radius * 0.35f, radius * 0.35f),
            radius = radius * 1.5f
        ),
        radius = radius,
        center = center
    )
    drawCircle(palette.edge, radius, center, style = Stroke(width = radius * 0.12f))
    val angle = Math.toRadians(angleDegrees.toDouble())
    val slot = Offset(cos(angle).toFloat(), sin(angle).toFloat()) * (radius * 0.7f)
    drawLine(
        color = palette.edge,
        start = center - slot,
        end = center + slot,
        strokeWidth = radius * 0.28f,
        cap = StrokeCap.Round
    )
}

/** A raised brushed steel plate with a screw in each corner, like the lid of a metal box. */
@Composable
fun MetalPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val palette = rememberMetalPalette()
    CompositionLocalProvider(LocalContentColor provides palette.content) {
        Box(
            modifier = modifier
                .shadow(8.dp, shape)
                .clip(shape)
                .brushedMetal(palette)
                .metalScrews(palette)
                .border(1.5.dp, palette.edge, shape)
                .padding(contentPadding),
            content = content
        )
    }
}
