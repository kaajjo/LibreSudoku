package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.generator.dlx.DlxEngine
import com.kaajjo.libresudoku.core.generator.dlx.DlxGenerator
import com.kaajjo.libresudoku.core.generator.dlx.DlxSearchBudgetExceeded
import com.kaajjo.libresudoku.core.generator.rating.*
import com.kaajjo.libresudoku.core.qqwing.models.QQWingResult
import com.kaajjo.libresudoku.core.qqwing.models.RatingMetadata
import com.kaajjo.libresudoku.core.qqwing.models.SolveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.random.Random

class NoRatedPuzzleException(message: String): RuntimeException(message)
class GenerationTimeBudgetExceeded: RuntimeException("Generation elapsed-time budget exhausted")

/**
 * Returns a fully rated, unique puzzle of the requested difficulty, or fails after a bounded search.
 * Unspecified requests accept any completed rating. Other difficulties are never substituted.
 * Each request owns its RNG. A seeded controller is repeatable for the same sequential call order,
 * but neither concurrent scheduling nor elapsed-time cutoffs are guaranteed replay-stable.
 *
 * @param random Source used to obtain one independent random seed per generation request.
 * @param maxGenerationAttempts Optional positive attempt cap overriding the size/difficulty profile.
 * @param exposeSimple Whether naked-single-only puzzles may use the separate Simple category.
 * @param evaluationLimits Per-candidate logical evaluation limits; an exhausted candidate is discarded.
 * @param maxSearchNodesPerCheck Positive node budget for each DLX search or uniqueness check.
 * @param maxGenerationMillis Positive elapsed-time budget in milliseconds for one generation call.
 * @param profileFor Supplies clue-removal ranges and attempt limits for a supported classical type.
 */
class QQWingControllerV2(
    private val random: Random=Random.Default,
    /** Null uses the per-size/difficulty profile. An explicit cap always takes precedence. */
    private val maxGenerationAttempts: Int?=null,
    private val exposeSimple: Boolean=false,
    private val evaluationLimits: EvaluationLimits=EvaluationLimits(),
    private val maxSearchNodesPerCheck: Long=2_000_000L,
    private val maxGenerationMillis: Long=10_000L,
    private val profileFor: (GameType)->GenerationProfile=ClassicGenerationProfiles::forType
) {
    init {
        require((maxGenerationAttempts==null || maxGenerationAttempts>=1) && maxSearchNodesPerCheck>0)
        require(maxGenerationMillis in 1..(Long.MAX_VALUE/1_000_000L))
    }
    private val evaluator=ClassicDifficultyAdapter(exposeSimple=exposeSimple,limits=evaluationLimits)

    /**
     * Searches for a unique puzzle with a completed rating matching the request.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     * @param targetDifficulty Required category; Unspecified accepts any completed rating. Simple requires exposeSimple.
     * @throws NoRatedPuzzleException if this call exhausts its time or attempt budget.
     * @throws IllegalArgumentException if the type or difficulty is unsupported.
     */
    suspend fun generate(type: GameType,targetDifficulty: GameDifficulty): QQWingResult {
        require(ClassicGameTypes.isSupported(type)) { "Concrete classical type required" }
        require(targetDifficulty in listOf(GameDifficulty.Unspecified,GameDifficulty.Easy,GameDifficulty.Moderate,
            GameDifficulty.Hard,GameDifficulty.Challenge) || exposeSimple && targetDifficulty==GameDifficulty.Simple)
        // Serialize only seed acquisition, not the CPU work. Avoid a shared seeded RNG on Default.
        val requestSeed=synchronized(random) { random.nextLong() }
        return withContext(Dispatchers.Default) {
            val context=currentCoroutineContext(); context.ensureActive()
            val started=System.nanoTime(); val allowedNanos=maxGenerationMillis*1_000_000L
            val checkpoint: ()->Unit={
                context.ensureActive()
                if (System.nanoTime()-started >= allowedNanos) throw GenerationTimeBudgetExceeded()
            }
            val rng=Random(requestSeed); val profile=profileFor(type)
            val range=profile.range(targetDifficulty)
            val attemptLimit=maxGenerationAttempts ?: profile.attemptLimit(targetDifficulty)
            require(range.first>=1 && range.last<=type.size*type.size) { "Generation profile must request 1..boardSize holes for $type" }
            var attempted=0; var budgeted=0; var beyond=0; var mismatched=0
            try {
                repeat(attemptLimit) {
                    checkpoint(); attempted++
                    val target=if (range.first==range.last) range.first else rng.nextInt(range.first,range.last+1)
                    try {
                        val generated=DlxGenerator.generateWithSolution(type,target,rng,profile.topK,maxSearchNodesPerCheck,checkpoint)
                        val board=generated.puzzle
                        val assessment=evaluator.evaluateKnownUnique(type,board,checkpoint)
                        when (val rating=assessment.rating) {
                            is LogicalRating.BeyondSupported -> beyond++
                            is LogicalRating.Rated -> {
                                val actual=checkNotNull(assessment.difficulty)
                                if (targetDifficulty==GameDifficulty.Unspecified || actual==targetDifficulty) {
                                    return@withContext QQWingResult(board,generated.solution,actual,1,
                                        RatingMetadata(assessment.ratingVersion,rating.report.tier,rating.report.counts(),
                                            effortScore=assessment.effortScore,scorePolicy=assessment.scorePolicy))
                                }
                                mismatched++
                            }
                            is LogicalRating.AlreadySolved -> error("Nonempty generation profile produced a solved board")
                            is LogicalRating.Contradiction, is LogicalRating.InvalidInput -> error("DLX/rater invariant violation: $rating")
                        }
                    } catch (_: EvaluationBudgetExceeded) {
                        budgeted++ // discard this attempt; it is NOT a rated challenge
                    } catch (_: DlxSearchBudgetExceeded) {
                        budgeted++ // discard whole trajectory, never cache this clue as impossible
                    }
                }
            } catch (_: GenerationTimeBudgetExceeded) {
                context.ensureActive()
                throw NoRatedPuzzleException("Time budget exhausted for $targetDifficulty; attempts=$attempted, mismatched=$mismatched, beyond=$beyond, budgeted=$budgeted")
            }
            context.ensureActive()
            throw NoRatedPuzzleException("No rated puzzle for $targetDifficulty; attempts=$attempted, mismatched=$mismatched, beyond=$beyond, budgeted=$budgeted")
        }
    }

    /**
     * Finds a solution and counts up to two classical completions.
     *
     * @param gameBoard Row-major givens, with zero for empty cells and 1..N for values. Copied before dispatch to the CPU worker.
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     * @return Impossible for malformed or unsatisfiable boards; otherwise the first solution and capped count.
     * @throws DlxSearchBudgetExceeded if uniqueness cannot be determined within the node budget.
     */
    suspend fun solve(gameBoard: IntArray,type: GameType): SolveResult {
        require(ClassicGameTypes.isSupported(type)) { "This solver supports only explicit classical variants" }
        // Take the snapshot before dispatch; caller must not mutate during this copy.
        val input=gameBoard.copyOf()
        return withContext(Dispatchers.Default) {
            if (input.size!=type.size*type.size || input.any { it !in 0..type.size }) return@withContext SolveResult.Impossible
            val context=currentCoroutineContext(); val checkpoint={ context.ensureActive() }
            try {
                val engine=DlxEngine(type.size,type.sectionHeight,type.sectionWidth,Random(0),checkpoint)
                val count=engine.solve(input,2,checkpoint,maxSearchNodesPerCheck)
                if (count==0) SolveResult.Impossible else SolveResult.Success(engine.solvedBoard,count)
            } catch (_: IllegalArgumentException) {
                SolveResult.Impossible
            }
            // Cancellation and budget exceptions deliberately propagate, not Impossible.
        }
    }

}
