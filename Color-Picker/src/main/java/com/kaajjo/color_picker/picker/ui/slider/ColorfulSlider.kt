package com.kaajjo.color_picker.picker.ui.slider

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.GestureCancellationException
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.kaajjo.color_picker.picker.model.MaterialSliderColors
import com.kaajjo.color_picker.picker.model.MaterialSliderDefaults
import com.kaajjo.color_picker.picker.util.calculateFraction
import com.kaajjo.color_picker.picker.util.lerp
import com.kaajjo.color_picker.picker.util.scale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 **
 * Use continuous sliders to allow users to make meaningful selections that don’t
 * require a specific value:
 **
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * Material Slider allows to choose height for track and thumb radius and selection between
 * [Color] or [Brush] using [SliderBrushColor]. If brush of [SliderBrushColor.brush] is
 * not null gradient
 * provided in this [Brush] is used for drawing otherwise solid color
 * [SliderBrushColor.solidColor] is used.
 *
 * @param value current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange lambda that returns value.
 * @param modifier modifiers for the Slider layout.
 * @param enabled whether or not component is enabled and can be interacted with or not
 * @param valueRange range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 * @param trackHeight height of the track that will be drawn on [Canvas]. half of [trackHeight]
 * is used as **stroke** width.
 * @param thumbRadius radius of thumb of the the slider
 * @param colors [MaterialSliderColors] that will be used to determine the color of the Slider parts in
 * different state. See [MaterialSliderDefaults.defaultColors],
 * [MaterialSliderDefaults.customColors] or other functions to customize.
 * @param borderStroke draws border around the track with given width in dp.
 * @param drawInactiveTrack flag to draw **InActive** track between active progress and track end.
 * @param coerceThumbInTrack when set to true track's start position is matched to thumbs left
 * on start and thumbs right at the end of the track. Use this when [trackHeight] is bigger than
 * [thumbRadius].
 */
@Composable
fun ColorfulSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = TrackHeight,
    thumbRadius: Dp = ThumbRadius,
    colors: MaterialSliderColors = MaterialSliderDefaults.defaultColors(),
    borderStroke: BorderStroke? = null,
    drawInactiveTrack: Boolean = true,
    coerceThumbInTrack: Boolean = false
) {
    ColorfulSlider(
        modifier = modifier,
        value = value,
        onValueChange = { progress, _ ->
            onValueChange(progress)
        },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        trackHeight = trackHeight,
        thumbRadius = thumbRadius,
        colors = colors,
        borderStroke = borderStroke,
        drawInactiveTrack = drawInactiveTrack,
        coerceThumbInTrack = coerceThumbInTrack
    )
}

/**
 * Sliders allow users to make selections from a range of values.
 *
 * Sliders reflect a range of values along a bar, from which users may select a single value.
 * They are ideal for adjusting settings such as volume, brightness, or applying image filters.
 **
 * Use continuous sliders to allow users to make meaningful selections that don’t
 * require a specific value:
 **
 * You can allow the user to choose only between predefined set of values by specifying the amount
 * of steps between min and max values:
 *
 * Material Slider allows to choose height for track and thumb radius and selection between
 * [Color] or [Brush] using [SliderBrushColor]. If brush of [SliderBrushColor.brush] is
 * not null gradient
 * provided in this [Brush] is used for drawing otherwise solid color
 * [SliderBrushColor.solidColor] is used.
 *
 * @param value current value of the Slider. If outside of [valueRange] provided, value will be
 * coerced to this range.
 * @param onValueChange lambda that returns value, position of **thumb** as [Offset], vertical
 * center is stored in y.
 * @param modifier modifiers for the Slider layout
 * @param enabled whether or not component is enabled and can be interacted with or not
 * @param valueRange range of values that Slider value can take. Passed [value] will be coerced to
 * this range
 * @param steps if greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, slider will behave as a continuous slider and allow
 * to choose any value from the range specified. Must not be negative.
 * @param trackHeight height of the track that will be drawn on [Canvas]. half of [trackHeight]
 * is used as **stroke** width.
 * @param thumbRadius radius of thumb of the the slider
 * @param colors [MaterialSliderColors] that will be used to determine the color of the Slider parts in
 * different state. See [MaterialSliderDefaults.defaultColors],
 * [MaterialSliderDefaults.customColors] or other functions to customize.
 * @param borderStroke draws border around the track with given width in dp.
 * @param drawInactiveTrack flag to draw **InActive** track between active progress and track end.
 * @param coerceThumbInTrack when set to true track's start position is matched to thumbs left
 * on start and thumbs right at the end of the track. Use this when [trackHeight] is bigger than
 * [thumbRadius].
 */
@Composable
fun ColorfulSlider(
    value: Float,
    onValueChange: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = TrackHeight,
    thumbRadius: Dp = ThumbRadius,
    colors: MaterialSliderColors = MaterialSliderDefaults.defaultColors(),
    borderStroke: BorderStroke? = null,
    drawInactiveTrack: Boolean = true,
    coerceThumbInTrack: Boolean = false,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {

    require(steps >= 0) { "steps should be >= 0" }
    val onValueChangeState = rememberUpdatedState(onValueChange)
    val onValueChangeFinishedState by rememberUpdatedState(onValueChangeFinished)
    val tickFractions = remember(steps) {
        stepsToTickFractions(steps)
    }
    BoxWithConstraints(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .requiredSizeIn(
                minWidth = ThumbRadius * 2,
                minHeight = ThumbRadius * 2
            ),
        contentAlignment = Alignment.CenterStart
    ) {

        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

        val width = constraints.maxWidth.toFloat()
        val thumbRadiusInPx: Float

        // Start of the track used for measuring progress,
        // it's line + radius of cap which is half of height of track
        // to draw this on canvas starting point of line
        // should be at trackStart + trackHeightInPx / 2 while drawing
        val trackStart: Float
        // End of the track that is used for measuring progress
        val trackEnd: Float
        val strokeRadius: Float
        with(LocalDensity.current) {
            thumbRadiusInPx = thumbRadius.toPx()
            strokeRadius = trackHeight.toPx() / 2
            trackStart = thumbRadiusInPx.coerceAtLeast(strokeRadius)
            trackEnd = width - trackStart
        }

        // Sales and interpolates from offset from dragging to user value in valueRange
        fun scaleToUserValue(offset: Float) =
            scale(trackStart, trackEnd, offset, valueRange.start, valueRange.endInclusive)

        // Scales user value using valueRange to position on x axis on screen
        fun scaleToOffset(userValue: Float) =
            scale(valueRange.start, valueRange.endInclusive, userValue, trackStart, trackEnd)

        val scope = rememberCoroutineScope()
        val rawOffset = remember { mutableStateOf(scaleToOffset(value)) }
        val pressOffset = remember { mutableStateOf(0f) }

        val draggableState = remember(trackStart, trackEnd, valueRange) {
            SliderDraggableState {
                rawOffset.value = (rawOffset.value + it + pressOffset.value)
                pressOffset.value = 0f
                val offsetInTrack = rawOffset.value.coerceIn(trackStart, trackEnd)
                onValueChangeState.value.invoke(
                    scaleToUserValue(offsetInTrack),
                    Offset(rawOffset.value.coerceIn(trackStart, trackEnd), strokeRadius)
                )
            }
        }
        CorrectValueSideEffect(
            ::scaleToOffset,
            valueRange,
            trackStart..trackEnd,
            rawOffset,
            value
        )

        LaunchedEffect(valueRange) {
            onValueChangeState.value(
                value,
                Offset(x = rawOffset.value.coerceIn(trackStart, trackEnd), y = strokeRadius)
            )
        }

        val coerced = value.coerceIn(valueRange.start, valueRange.endInclusive)
        val fraction = calculateFraction(valueRange.start, valueRange.endInclusive, coerced)

        val gestureEndAction = rememberUpdatedState<(Float) -> Unit> { velocity: Float ->
            val current = rawOffset.value
            val target = snapValueToTick(current, tickFractions, trackStart, trackEnd)
            if (current != target) {
                scope.launch {
                    animateToTarget(draggableState, current, target, velocity)
                    onValueChangeFinished?.invoke()
                }
            } else if (!draggableState.isDragging) {
                // check ifDragging in case the change is still in progress (touch -> drag case)
                onValueChangeFinished?.invoke()
            }
        }
        val press = Modifier.sliderTapModifier(
            draggableState = draggableState,
            interactionSource = interactionSource,
            maxPx = constraints.maxWidth.toFloat(),
            isRtl = isRtl,
            rawOffset = rawOffset,
            gestureEndAction = gestureEndAction,
            pressOffset = pressOffset,
            enabled = enabled
        )

        val drag = Modifier.draggable(
            orientation = Orientation.Horizontal,
            reverseDirection = isRtl,
            enabled = enabled,
            interactionSource = interactionSource,
            onDragStopped = { _ ->
                if (enabled) {
                    onValueChangeFinishedState?.invoke()
                }
            },
            startDragImmediately = draggableState.isDragging,
            state = draggableState
        )

        SliderImpl(
            enabled = enabled,
            fraction = fraction,
            trackStart = trackStart,
            trackEnd = trackEnd,
            tickFractions = tickFractions,
            colors = colors,
            trackHeight = trackHeight,
            thumbRadius = thumbRadiusInPx,
            coerceThumbInTrack = coerceThumbInTrack,
            drawInactiveTrack = drawInactiveTrack,
            borderStroke = borderStroke,
            modifier = press.then(drag)
        )
    }
}

@Composable
private fun SliderImpl(
    enabled: Boolean,
    fraction: Float,
    trackStart: Float,
    trackEnd: Float,
    tickFractions: List<Float>,
    colors: MaterialSliderColors,
    trackHeight: Dp,
    thumbRadius: Float,
    coerceThumbInTrack: Boolean,
    drawInactiveTrack: Boolean,
    borderStroke: BorderStroke? = null,
    modifier: Modifier,
) {

    val trackStrokeWidth: Float
    val thumbSize: Dp

    var borderWidth = 0f
    val borderBrush: Brush? = borderStroke?.brush

    with(LocalDensity.current) {
        trackStrokeWidth = trackHeight.toPx()
        thumbSize = (2 * thumbRadius).toDp()

        if (borderStroke != null) {
            borderWidth = borderStroke.width.toPx()
        }
    }

    Box(
        // Constraint max height of Slider to max of thumb or track or minimum touch 48.dp
        modifier
            .heightIn(
                max = trackHeight
                    .coerceAtLeast(thumbSize)
                    .coerceAtLeast(TrackHeight)
            )
    ) {

        // Position that corresponds to center of this slider's thumb
        val thumbCenterPos = (trackStart + (trackEnd - trackStart) * fraction)

        Track(
            modifier = Modifier.fillMaxSize(),
            fraction = fraction,
            tickFractions = tickFractions,
            thumbRadius = thumbRadius,
            trackStart = trackStart,
            trackHeight = trackStrokeWidth,
            coerceThumbInTrack = coerceThumbInTrack,
            colors = colors,
            enabled = enabled,
            borderBrush = borderBrush,
            borderWidth = borderWidth,
            drawInactiveTrack = drawInactiveTrack
        )

        Thumb(
            modifier = Modifier.align(Alignment.CenterStart),
            offset = thumbCenterPos - thumbRadius,
            thumbSize = thumbSize,
            colors = colors,
            enabled = enabled
        )
    }
}

/**
 * Draws active and if [drawInactiveTrack] is set to true inactive tracks on Canvas.
 * If inactive track is to be drawn it's drawn between start and end of canvas. Active track
 * is drawn between start and current value.
 *
 * Drawing both tracks use [SliderBrushColor] to draw a nullable [Brush] first. If it's not then
 * [SliderBrushColor.solidColor] is used to draw with solid colors provided by [MaterialSliderColors]
 */
@Composable
private fun Track(
    modifier: Modifier,
    fraction: Float,
    tickFractions: List<Float>,
    thumbRadius: Float,
    trackStart: Float,
    trackHeight: Float,
    coerceThumbInTrack: Boolean,
    colors: MaterialSliderColors,
    enabled: Boolean,
    borderBrush: Brush?,
    borderWidth: Float,
    drawInactiveTrack: Boolean,
) {

    val debug = false

    // Colors for drawing track and/or ticks
    val activeTrackColor: Brush =
        colors.trackColor(enabled = enabled, active = true).value
    val inactiveTrackColor: Brush =
        colors.trackColor(enabled = enabled, active = false).value
    val inactiveTickColor = colors.tickColor(enabled, active = false).value
    val activeTickColor = colors.tickColor(enabled, active = true).value

    // stroke radius is used for drawing length it adds this radius to both sides of the line
    val strokeRadius = trackHeight / 2

    // Start of drawing in Canvas
    // when not coerced set start of drawing line at trackStart + strokeRadius
    // to limit drawing start edge at track start end edge at track end

    // When coerced move edges of drawing by thumb radius to cover thumb edges in drawing
    // it needs to move to right as stroke radius minus thumb radius to match track start
    val drawStart =
        if (coerceThumbInTrack) trackStart - thumbRadius + strokeRadius else trackStart

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val isRtl = layoutDirection == LayoutDirection.Rtl

        val centerY = center.y

        // left side of the slider that is drawn on canvas, left tip of stroke radius on left side
        val sliderLeft = Offset(drawStart, centerY)
        // right side of the slider that is drawn on canvas, right tip of stroke radius on left side
        val sliderRight = Offset((width - drawStart).coerceAtLeast(drawStart), centerY)

        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight

        val sliderValue = Offset(
            sliderStart.x + (sliderEnd.x - sliderStart.x) * fraction,
            center.y
        )

        // InActive Track
        if (drawInactiveTrack) {
            drawLine(
                brush = inactiveTrackColor,
                start = sliderStart,
                end = sliderEnd,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }

        // Active Track
        drawLine(
            brush = activeTrackColor,
            start = sliderStart,
            end = if (drawInactiveTrack) sliderValue else sliderEnd,
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        if (debug) {
            drawLine(
                color = Color.Yellow,
                start = sliderStart,
                end = sliderEnd,
                strokeWidth = strokeRadius / 4
            )
        }

        borderBrush?.let { brush ->
            drawRoundRect(
                brush = brush,
                topLeft = Offset(sliderStart.x - strokeRadius, (height - trackHeight) / 2),
                size = Size(width = sliderEnd.x - sliderStart.x + trackHeight, trackHeight),
                cornerRadius = CornerRadius(strokeRadius, strokeRadius),
                style = Stroke(width = borderWidth)
            )
        }

        if (drawInactiveTrack) {
            tickFractions.groupBy { it > fraction }
                .forEach { (outsideFraction, list) ->
                    drawPoints(
                        points = list.map {
                            Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                        },
                        pointMode = PointMode.Points,
                        brush = if (outsideFraction) inactiveTickColor
                        else activeTickColor,
                        strokeWidth = strokeRadius.coerceAtMost(thumbRadius / 2),
                        cap = StrokeCap.Round
                    )
                }
        }
    }
}

@Composable
private fun Thumb(
    modifier: Modifier,
    offset: Float,
    thumbSize: Dp,
    colors: MaterialSliderColors,
    enabled: Boolean
) {

    val thumbColor: Brush = colors.thumbColor(enabled).value

    Spacer(
        modifier = modifier
            .offset { IntOffset(offset.toInt(), 0) }
            .shadow(1.dp, shape = CircleShape)
            .size(thumbSize)
            .then(
                Modifier.background(thumbColor)
            )
    )
}

@Composable
internal fun CorrectValueSideEffect(
    scaleToOffset: (Float) -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    trackRange: ClosedFloatingPointRange<Float>,
    valueState: MutableState<Float>,
    value: Float
) {
    SideEffect {
        val error = (valueRange.endInclusive - valueRange.start) / 1000
        val newOffset = scaleToOffset(value)
        if (abs(newOffset - valueState.value) > error) {
            if (valueState.value in trackRange) {
                valueState.value = newOffset
            }
        }
    }
}

internal fun stepsToTickFractions(steps: Int): List<Float> {
    return if (steps == 0) emptyList() else List(steps + 2) { it.toFloat() / (steps + 1) }
}

internal val ThumbRadius = 10.dp
internal val TrackHeight = 4.dp
internal val SliderHeight = 48.dp

private val SliderToTickAnimation = TweenSpec<Float>(durationMillis = 100)

internal fun Modifier.sliderTapModifier(
    draggableState: DraggableState,
    interactionSource: MutableInteractionSource,
    maxPx: Float,
    isRtl: Boolean,
    rawOffset: State<Float>,
    gestureEndAction: State<(Float) -> Unit>,
    pressOffset: MutableState<Float>,
    enabled: Boolean
) = composed(
    factory = {
        if (enabled) {
            val scope = rememberCoroutineScope()
            pointerInput(draggableState, interactionSource, maxPx, isRtl) {
                detectTapGestures(
                    onPress = { pos ->
                        val to = if (isRtl) maxPx - pos.x else pos.x
                        pressOffset.value = to - rawOffset.value
                        try {
                            awaitRelease()
                        } catch (_: GestureCancellationException) {
                            pressOffset.value = 0f
                        }
                    },
                    onTap = {
                        scope.launch {
                            draggableState.drag(MutatePriority.UserInput) {
                                // just trigger animation, press offset will be applied
                                dragBy(0f)
                            }
                            gestureEndAction.value.invoke(0f)
                        }
                    }
                )
            }
        } else {
            this
        }
    },
    inspectorInfo = debugInspectorInfo {
        name = "sliderTapModifier"
        properties["draggableState"] = draggableState
        properties["interactionSource"] = interactionSource
        properties["maxPx"] = maxPx
        properties["isRtl"] = isRtl
        properties["rawOffset"] = rawOffset
        properties["gestureEndAction"] = gestureEndAction
        properties["pressOffset"] = pressOffset
        properties["enabled"] = enabled
    }
)

internal suspend fun animateToTarget(
    draggableState: DraggableState,
    current: Float,
    target: Float,
    velocity: Float
) {
    draggableState.drag {
        var latestValue = current
        Animatable(initialValue = current).animateTo(target, SliderToTickAnimation, velocity) {
            dragBy(this.value - latestValue)
            latestValue = this.value
        }
    }
}

internal fun snapValueToTick(
    current: Float,
    tickFractions: List<Float>,
    minPx: Float,
    maxPx: Float
): Float {
    // target is a closest anchor to the `current`, if exists
    return tickFractions
        .minByOrNull { abs(lerp(minPx, maxPx, it) - current) }
        ?.run { lerp(minPx, maxPx, this) }
        ?: current
}

internal class SliderDraggableState(
    val onDelta: (Float) -> Unit
) : DraggableState {

    var isDragging by mutableStateOf(false)
        private set

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}