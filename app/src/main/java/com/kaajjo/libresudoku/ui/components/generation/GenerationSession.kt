package com.kaajjo.libresudoku.ui.components.generation

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GamePuzzleGenerator
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.NoRatedPuzzleException
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GenerationProgress(
    val type: GameType,
    val difficulty: GameDifficulty,
    val total: Int,
    val completed: Int = 0
)

sealed interface GenerationState {
    data object Idle : GenerationState
    data class Running(val progress: GenerationProgress) : GenerationState
    data class SaveFailed(val progress: GenerationProgress) : GenerationState
    data object Completed : GenerationState
}

/**
 * Owns a generation request across dialog recompositions. Call actions on the scope's dispatcher
 * (the main thread in ViewModels). Only fully rated puzzles matching the request can be saved;
 * Killer ratings describe the classical base, as recorded in the metadata version.
 * Generation retries automatically until cancelled. Save retries keep the same puzzle and batch prefix.
 *
 * @param scope Owner scope and dispatcher for state changes; cancelling it stops the session.
 * @param generate Suspending generator; results must be unique, logically rated and match the requested category.
 * @param onError Reports unexpected generation or save errors; search exhaustion and cancellation are excluded.
 */
class GenerationSession(
    private val scope: CoroutineScope,
    private val generate: suspend (GameType, GameDifficulty) -> QQWingResult =
        GamePuzzleGenerator()::generate,
    private val onError: (Exception) -> Unit = {}
) {
    private val mutableState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val state = mutableState.asStateFlow()

    private var job: Job? = null
    private var runId: Any? = null
    private var retrySignal: CompletableDeferred<Unit>? = null

    /**
     * Starts a batch, automatically retrying generation until success or cancellation.
     *
     * @param type Concrete variant shared by every puzzle in the batch.
     * @param difficulty Required category; Unspecified allows any completed rating.
     * @param count Positive number of puzzles to save.
     * @param save Persists one matching result. A failed save can be retried with the same result; keep side effects retry-safe.
     * @return Immediately; an already active request is left unchanged.
     */
    fun start(
        type: GameType,
        difficulty: GameDifficulty,
        count: Int = 1,
        save: suspend (QQWingResult) -> Unit
    ) {
        if (job?.isActive == true) return
        require(count > 0)
        val id = Any()
        runId = id
        var progress = GenerationProgress(type, difficulty, count)
        mutableState.value = GenerationState.Running(progress)
        job = scope.launch {
            var pendingSave: QQWingResult? = null
            try {
                while (progress.completed < progress.total) {
                    currentCoroutineContext().ensureActive()
                    mutableState.value = GenerationState.Running(progress)
                    try {
                        if (pendingSave == null) {
                            val candidate = generate(type, progress.difficulty)
                            currentCoroutineContext().ensureActive()
                            if (candidate.difficulty == GameDifficulty.Unspecified ||
                                candidate.difficulty == GameDifficulty.Custom ||
                                candidate.ratingMetadata?.logicallySolved != true ||
                                candidate.solutionCount != 1 ||
                                progress.difficulty != GameDifficulty.Unspecified &&
                                candidate.difficulty != progress.difficulty
                            ) {
                                throw NoRatedPuzzleException("Generator did not return a unique, fully rated puzzle of the requested difficulty")
                            }
                            pendingSave = candidate
                        }

                        mutableState.value = GenerationState.Running(progress)
                        save(checkNotNull(pendingSave))
                        currentCoroutineContext().ensureActive()
                        pendingSave = null
                        progress = progress.copy(completed = progress.completed + 1)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        currentCoroutineContext().ensureActive()
                        if (pendingSave == null) {
                            if (exception !is NoRatedPuzzleException) onError(exception)
                            // Suspend even after immediate failures so cancellation and UI work can run.
                            delay(100)
                        } else {
                            onError(exception)
                            awaitRetry(GenerationState.SaveFailed(progress))
                        }
                    }
                }
                mutableState.value = GenerationState.Completed
            } finally {
                // A cancelled worker may finish after a new request has already started.
                if (runId === id) {
                    retrySignal = null
                    if (mutableState.value != GenerationState.Completed) {
                        mutableState.value = GenerationState.Idle
                    }
                }
            }
        }
    }

    fun retry() {
        if (state.value is GenerationState.SaveFailed) retrySignal?.complete(Unit)
    }

    fun cancel() {
        runId = null
        job?.cancel()
        retrySignal?.cancel()
        retrySignal = null
        mutableState.value = GenerationState.Idle
    }

    private suspend fun awaitRetry(state: GenerationState.SaveFailed) {
        val pending = CompletableDeferred<Unit>()
        retrySignal = pending
        mutableState.value = state
        return try {
            pending.await()
        } finally {
            if (retrySignal === pending) retrySignal = null
        }
    }

}
