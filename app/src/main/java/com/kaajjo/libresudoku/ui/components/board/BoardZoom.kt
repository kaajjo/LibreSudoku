package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp

/**
 * Zoom and pan of the board
 */
@Stable
class BoardZoomState {
    var scale by mutableFloatStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /**
     * Applies a gesture while keeping the visible area within the board
     *
     * @param centroid gesture centre in viewport pixels before this gesture update
     * @param pan pointer translation in viewport pixels since the previous update
     * @param zoom multiplicative scale change since the previous update
     * @param boardSize unzoomed board dimensions in pixels
     */
    fun onGesture(centroid: Offset, pan: Offset, zoom: Float, boardSize: Size) {
        val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
        val newOffset = offset + centroid / scale - centroid / newScale - pan / scale
        scale = newScale
        offset = Offset(
            x = newOffset.x.coerceIn(0f, boardSize.width - boardSize.width / newScale),
            y = newOffset.y.coerceIn(0f, boardSize.height - boardSize.height / newScale)
        )
    }

    /**
     * Animates the scale back to 1 and the offset back to zero
     */
    suspend fun reset() {
        if (scale == 1f && offset == Offset.Zero) return
        val startScale = scale
        val startOffset = offset
        animate(0f, 1f, animationSpec = BoardMotion.ZoomReset) { fraction, _ ->
            scale = lerp(startScale, 1f, fraction)
            offset = lerp(startOffset, Offset.Zero, fraction)
        }
    }

    companion object {
        const val MAX_SCALE = 3f
    }
}

/**
 * Adds pinch zoom and drag pan while transforming subsequent drawing and pointer coordinates
 *
 * @param state zoom and pan values applied to the graphics layer
 * @param enabled whether gestures may update the state, without disabling its visual transform
 */
fun Modifier.boardZoom(state: BoardZoomState, enabled: Boolean): Modifier = this
    .pointerInput(state, enabled) {
        if (!enabled) return@pointerInput
        detectTransformGestures { centroid, pan, zoom, _ ->
            state.onGesture(centroid, pan, zoom, size.toSize())
        }
    }
    .graphicsLayer {
        transformOrigin = TransformOrigin(0f, 0f)
        scaleX = state.scale
        scaleY = state.scale
        translationX = -state.offset.x * state.scale
        translationY = -state.offset.y * state.scale
    }
