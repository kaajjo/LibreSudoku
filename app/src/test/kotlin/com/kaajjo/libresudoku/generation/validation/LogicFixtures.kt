package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry

/** Checked-in fixtures are classpath resources, so tests do not depend on the working directory. */
internal fun fixtureLines(name: String): List<String> =
    checkNotNull(IndependentOracle::class.java.getResourceAsStream("/logical-rating/$name")) {
        "Missing logical-rating fixture: $name"
    }.bufferedReader().use { it.readLines().drop(1).filter(String::isNotBlank) }

internal data class CorpusCase(
    val geometry: SudokuGeometry,
    val seed: Int,
    val expectedTier: String,
    val expectedScore: Int?,
    val expectedMoves: Int,
    val expectedAllPassMoves: Long,
    val expectedAllPassEffects: Long,
    val puzzle: IntArray
)

internal fun corpusCases(): List<CorpusCase> {
    val cases = fixtureLines("corpus.csv").map { line ->
        val fields = line.split(',')
        val size = fields[0].toInt()
        val seed = fields[1].toInt()
        val geometry = when (size) {
            6 -> SudokuGeometry(6, 2, 3)
            9 -> SudokuGeometry(9, 3, 3)
            12 -> SudokuGeometry(12, 3, 4)
            else -> error("Unexpected corpus geometry: $size")
        }
        CorpusCase(geometry, seed, fields[2], fields[3].toIntOrNull(),
            fields[4].toInt(), fields[5].toLong(), fields[6].toLong(),
            fields[7].map { Character.digit(it, 36) }.toIntArray())
    }
    check(cases.size == 420) { "Incomplete regression corpus" }
    check(cases.map { it.geometry.size to it.seed }.toSet().size == cases.size) {
        "Duplicate regression corpus case"
    }
    return cases
}
