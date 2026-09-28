package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.Technique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Each detector is forced directly
 * an unrelated easier technique cannot mask missing coverage
 */
@RunWith(Parameterized::class)
class AdvancedTechniqueSoundnessTest(
    private val fixtureName: String,
    private val transpose: Boolean
) {
    @Test
    fun everyConsequenceIsCertifiedAgainstThePreStepCandidateState() {
        val fixture = fixtures().single { it.name == fixtureName }
        val state = fresh()
        for (cell in fixture.state.masks.indices) {
            state.masks[if (transpose) (cell % 9) * 9 + cell / 9 else cell] =
                fixture.state.masks[cell]
        }
        val step = checkNotNull(findTechnique(state, fixture.technique)) {
            "No detection for $fixtureName, transpose=$transpose"
        }
        assertEquals(fixture.technique, step.technique)
        val before = state.masks.copyOf()

        // A satisfiable pre-state is required; forcing every eliminated candidate must be UNSAT.
        assertTrue(certify(state, step) > 0)
        verifyLinks(state, step)

        // Preserve the supplied synthetic fixture and evidence as a reviewed regression baseline.
        val fixtureId = "$fixtureName-${if (transpose) "T" else "I"}"
        val recorded = fixtureLines("technique-states.csv").single { it.startsWith("$fixtureId,") }
        val recordedParts = recorded.split('"')
        assertEquals(before.joinToString(" ") { it.toString(16) }, recordedParts[1])
        assertEquals(step.signature(), recordedParts[3])

        state.apply(step)
        assertFalse(before.contentEquals(state.masks))
        assertFalse(state.contradiction())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}, transpose={1}")
        fun cases(): List<Array<Any>> {
            val fixtures = fixtures()
            assertEquals(20, fixtures.size)
            assertEquals(20, fixtures.map { it.technique }.toSet().size)
            // These fixtures cover the original twenty complex detectors; v3 Full House has
            // separate coverage and must not be pulled in merely because its ID was appended.
            val advanced = Technique.values()
                .slice(Technique.NAKED_TRIPLE.ordinal..Technique.ALS_XZ.ordinal).toSet()
            assertEquals(advanced, fixtures.map { it.technique }.toSet())
            return fixtures.flatMap { fixture ->
                listOf(false, true).map { transpose -> arrayOf<Any>(fixture.name, transpose) }
            }
        }
    }
}
