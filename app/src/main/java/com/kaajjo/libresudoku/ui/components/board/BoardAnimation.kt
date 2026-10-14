package com.kaajjo.libresudoku.ui.components.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import com.kaajjo.libresudoku.core.Note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Shared animation specifications and motion constants for the sudoku board
 */
object BoardMotion {
    // Selection glides between cells, with a barely noticeable overshoot
    val SelectionMove = spring(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = Offset(0.005f, 0.005f)
    )
    val SelectionShow = tween<Float>(durationMillis = 150, easing = LinearOutSlowInEasing)
    val SelectionHide = tween<Float>(durationMillis = 120, easing = FastOutLinearInEasing)

    val DigitEnter = spring<Float>(dampingRatio = 0.6f, stiffness = 700f)
    val DigitExit = tween<Float>(durationMillis = 120, easing = FastOutLinearInEasing)
    const val DigitEnterFromScale = 0.5f
    const val DigitExitToScale = 0.8f

    val NoteEnter = tween<Float>(durationMillis = 180, easing = LinearOutSlowInEasing)
    val NoteExit = tween<Float>(durationMillis = 120, easing = FastOutLinearInEasing)
    const val NoteEnterFromScale = 0.6f

    val HighlightEnter = tween<Float>(durationMillis = 220, easing = LinearOutSlowInEasing)
    val HighlightExit = tween<Float>(durationMillis = 160, easing = FastOutLinearInEasing)
    val NoteHighlightEnter = spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

    val ErrorEnter = tween<Float>(durationMillis = 150)
    val ErrorExit = tween<Float>(durationMillis = 250, easing = FastOutLinearInEasing)

    val ErrorShake = spring<Float>(dampingRatio = 0.25f, stiffness = Spring.StiffnessMedium)
    const val ErrorShakeVelocity = 50f
    const val ErrorShakeAmplitude = 0.07f

    const val BulkChangeThreshold = 3
    const val WaveSpreadMillis = 250
    const val RippleSpreadMillis = 120

    val ZoomReset = spring<Float>(stiffness = Spring.StiffnessMediumLow)
}

/**
 * Delay provider that returns zero milliseconds for every key
 */
val NoDelay: (Any) -> Int = { 0 }

/**
 * A set of keys whose members animate in and out
 *
 * Keys removed by [update] remain available for drawing until their exit animation finishes
 *
 * @property scope coroutine scope that runs batch animations
 * @property enterSpec progress animation for added keys
 * @property exitSpec progress animation for removed keys
 */
@Stable
class AnimatedSet<K : Any>(
    private val scope: CoroutineScope,
    private val enterSpec: FiniteAnimationSpec<Float>,
    private val exitSpec: FiniteAnimationSpec<Float>
) {
    private val entries = mutableStateMapOf<K, Entry>()
    private var visibleKeys: Set<K> = emptySet()

    /**
     * Updates the target set and transitions keys whose membership has changed
     *
     * @param keys target set of visible keys
     * @param animate whether changed keys animate or are added and removed immediately
     * @param delayMillis nonnegative start delay in milliseconds for each changed key
     */
    fun update(keys: Set<K>, animate: Boolean = true, delayMillis: (K) -> Int = NoDelay) {
        if (keys == visibleKeys) return
        val entering = keys.filter { it !in visibleKeys }
        val exiting = visibleKeys.filter { it !in keys }
        visibleKeys = keys

        if (animate) {
            transition(entering, isEntering = true, delayMillis)
            transition(exiting, isEntering = false, delayMillis)
        } else {
            exiting.forEach { entries.remove(it) }
            entries.putAll(entering.associateWith { Entry.Shown })
        }
    }

    /**
     * Visits all entries, including keys whose exit animation is still running
     *
     * @param action callback receiving each key and its current animation entry
     */
    fun forEach(action: (key: K, entry: Entry) -> Unit) {
        entries.forEach { (key, entry) -> action(key, entry) }
    }

    /**
     * Returns the current visibility fraction of a key
     *
     * @param key item whose visibility is requested
     * @return 0 when absent, 1 when fully shown, with possible spring overshoot above 1
     */
    fun fractionOf(key: K): Float = entries[key]?.fraction ?: 0f

    private fun transition(keys: List<K>, isEntering: Boolean, delayMillis: (K) -> Int) {
        if (keys.isEmpty()) return
        val timeline = Timeline(
            spec = if (isEntering) enterSpec else exitSpec,
            maxDelayMillis = keys.maxOf(delayMillis)
        )
        entries.putAll(
            keys.associateWith { key ->
                Entry(
                    timeline = timeline,
                    delayMillis = delayMillis(key),
                    // Continue from where an interrupted animation left off, so nothing jumps
                    from = entries[key]?.fraction ?: 0f,
                    isEntering = isEntering
                )
            }
        )
        scope.launch {
            timeline.play()
            if (!isEntering) {
                keys.forEach { key -> if (entries[key]?.timeline === timeline) entries.remove(key) }
            }
        }
    }

    /**
     * Visibility state of one key within an animation batch
     *
     * @property timeline shared batch clock, or null for a settled entry
     * @property delayMillis start delay from the batch beginning in milliseconds
     * @property from visibility fraction captured when this transition starts
     * @property isEntering whether the fraction moves toward 1 instead of 0
     */
    class Entry constructor(
        val timeline: Timeline?,
        private val delayMillis: Int,
        private val from: Float,
        val isEntering: Boolean
    ) {
        /**
         * Current visibility: 0 is hidden and 1 is fully shown, with possible spring overshoot above 1
         */
        val fraction: Float
            get() {
                val progress = timeline?.progressAt(delayMillis) ?: 1f
                return if (isEntering) from + (1f - from) * progress else from * (1f - progress)
            }

        companion object {
            val Shown = Entry(timeline = null, delayMillis = 0, from = 1f, isEntering = true)
        }
    }
}

/**
 * Shared clock driven by one coroutine for a batch of keys that change together
 *
 * @param spec finite progress animation from 0 to 1, shared by all entries in the batch
 * @param maxDelayMillis largest entry start delay in milliseconds, added to the playback duration
 */
class Timeline(spec: FiniteAnimationSpec<Float>, maxDelayMillis: Int) {
    private val animation = TargetBasedAnimation(spec, Float.VectorConverter, 0f, 1f)
    private val totalMillis = (animation.durationNanos / 1_000_000 + maxDelayMillis).toInt()
    private val playTimeMillis = Animatable(0f)

    /**
     * Samples progress for an entry with a delayed start
     *
     * @param delayMillis entry start delay from the batch beginning in milliseconds
     * @return progress from 0 to 1, with possible overshoot according to the animation specification
     */
    fun progressAt(delayMillis: Int): Float {
        val playTimeNanos = ((playTimeMillis.value - delayMillis) * 1_000_000).toLong()
        return if (playTimeNanos <= 0) 0f else animation.getValueFromNanos(playTimeNanos)
    }

    /**
     * Advances the batch clock while respecting the system animation duration scale
     */
    suspend fun play() {
        playTimeMillis.animateTo(
            targetValue = totalMillis.toFloat(),
            animationSpec = tween(durationMillis = totalMillis, easing = LinearEasing)
        )
    }
}

/**
 * Animated selection shared by the cell, row and column highlights
 */
@Stable
class SelectionAnimation {
    val position = Animatable(Offset.Zero, Offset.VectorConverter)

    val visibility = Animatable(0f)

    /**
     * Moves the visible selection or fades it out when cleared
     *
     * @param cell target cell position, or null to hide the selection
     */
    suspend fun animateTo(cell: CellPosition?) {
        if (cell == null) {
            visibility.animateTo(0f, BoardMotion.SelectionHide)
            return
        }
        val target = Offset(cell.col.toFloat(), cell.row.toFloat())
        if (visibility.value < 0.5f) position.snapTo(target)
        coroutineScope {
            launch { visibility.animateTo(1f, BoardMotion.SelectionShow) }
            position.animateTo(target, BoardMotion.SelectionMove)
        }
    }
}

/**
 * Animated board state updated from content snapshots and sampled during drawing
 *
 * Reading animation values in the draw phase redraws the board without recomposing it
 *
 * @property scope coroutine scope that runs visibility transitions and error shakes
 */
@Stable
class BoardAnimations(private val scope: CoroutineScope) {
    val selection = SelectionAnimation()
    val digits = AnimatedSet<Digit>(scope, BoardMotion.DigitEnter, BoardMotion.DigitExit)
    val notes = AnimatedSet<Note>(scope, BoardMotion.NoteEnter, BoardMotion.NoteExit)
    val errors = AnimatedSet<CellPosition>(scope, BoardMotion.ErrorEnter, BoardMotion.ErrorExit)
    val sameNumberCells = AnimatedSet<CellPosition>(scope, BoardMotion.HighlightEnter, BoardMotion.HighlightExit)
    val hintCells = AnimatedSet<CellPosition>(scope, BoardMotion.HighlightEnter, BoardMotion.HighlightExit)
    val highlightedNotes = AnimatedSet<Note>(scope, BoardMotion.NoteHighlightEnter, BoardMotion.HighlightExit)

    private val shakes = mutableStateMapOf<CellPosition, Animatable<Float, AnimationVector1D>>()
    private var current: BoardContent? = null

    /**
     * Returns the current horizontal shake displacement before applying the amplitude multiplier
     *
     * @param cell zero-based cell position
     * @return animation displacement, or zero when the cell is not shaking
     */
    fun shakeOf(cell: CellPosition): Float = shakes[cell]?.value ?: 0f

    /**
     * Updates content animations, showing the first snapshot immediately and staggering bulk changes
     *
     * Selection movement is driven separately through [SelectionAnimation.animateTo]
     *
     * @param content target board content and requested highlights
     */
    fun update(content: BoardContent) {
        val previous = current
        if (content == previous) return
        current = content

        if (previous == null) {
            // A board that just appeared on screen shouldn't put on a show
            show(content, animate = false)
            return
        }
        val before = if (previous.gameSize == content.gameSize) {
            previous
        } else {
            // Positions point to different cells on a board of another size: start from a clean slate
            BoardContent.empty(content.gameSize).also { show(it, animate = false) }
        }

        val wave = waveFrom(
            origin = content.selected ?: CellPosition(0, 0),
            gameSize = content.gameSize,
            spreadMillis = BoardMotion.WaveSpreadMillis
        )
        val isBulkDigitChange = isBulk(before.digits, content.digits)
        val isBulkNoteChange = isBulk(before.notes, content.notes)

        digits.update(
            content.digits,
            delayMillis = { digit -> if (isBulkDigitChange) wave(digit.position) else 0 }
        )
        notes.update(
            content.notes,
            delayMillis = { note -> if (isBulkNoteChange) wave(note.position) else 0 }
        )
        errors.update(content.errors)
        sameNumberCells.update(
            content.sameNumberCells,
            delayMillis = content.selected
                ?.let { waveFrom(it, content.gameSize, BoardMotion.RippleSpreadMillis) }
                ?: NoDelay
        )
        hintCells.update(content.hintCells)
        highlightedNotes.update(content.highlightedNotes)

        if (!isBulkDigitChange) {
            content.digits
                .filter { it !in before.digits && it.position in content.errors }
                .forEach { shake(it.position) }
        }
    }

    private fun show(content: BoardContent, animate: Boolean) {
        digits.update(content.digits, animate)
        notes.update(content.notes, animate)
        errors.update(content.errors, animate)
        sameNumberCells.update(content.sameNumberCells, animate)
        hintCells.update(content.hintCells, animate)
        highlightedNotes.update(content.highlightedNotes, animate)
    }

    private fun shake(cell: CellPosition) {
        val shake = Animatable(0f)
        shakes[cell] = shake
        scope.launch {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = BoardMotion.ErrorShake,
                initialVelocity = BoardMotion.ErrorShakeVelocity
            )
            if (shakes[cell] === shake) shakes.remove(cell)
        }
    }
}

/**
 * Remembers the board animations with a coroutine scope tied to this composition
 */
@Composable
fun rememberBoardAnimations(): BoardAnimations {
    val scope = rememberCoroutineScope()
    return remember(scope) { BoardAnimations(scope) }
}

private fun <K> isBulk(before: Set<K>, after: Set<K>): Boolean {
    val changed = before.count { it !in after } + after.count { it !in before }
    return changed > BoardMotion.BulkChangeThreshold
}

/**
 * Creates start delays proportional to each cell's distance from the wave origin
 *
 * @param origin cell from which the wave spreads with zero delay
 * @param gameSize number of rows and columns in the board
 * @param spreadMillis delay in milliseconds assigned to the farthest corner
 * @return delay provider for cell positions, measured in milliseconds
 */
private fun waveFrom(origin: CellPosition, gameSize: Int, spreadMillis: Int): (CellPosition) -> Int {
    val last = gameSize - 1
    val farthest = hypot(
        max(origin.row, last - origin.row).toFloat(),
        max(origin.col, last - origin.col).toFloat()
    )
    if (farthest == 0f) return NoDelay
    return { cell ->
        val distance = hypot((cell.row - origin.row).toFloat(), (cell.col - origin.col).toFloat())
        (distance / farthest * spreadMillis).roundToInt()
    }
}
