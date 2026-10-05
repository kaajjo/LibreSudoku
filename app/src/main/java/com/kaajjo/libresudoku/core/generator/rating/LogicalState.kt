package com.kaajjo.libresudoku.core.generator.rating

/**
 * Exclusively owned by one logical pass. Test injection never enters the public rating API.
 *
 * @param topology Immutable geometry, unit membership and peer relationships.
 * @param control Operation budget and cooperative cancellation checks shared by this evaluation.
 */
internal class LogicalState(val topology: Topology, val control: EvaluationControl) {
    val n = topology.n
    val board = IntArray(topology.cells)
    val masks = IntArray(topology.cells)

    /**
     * Initializes board values and legal candidates from givens.
     *
     * @param input Row-major givens, with zero for empty cells and 1..N for values. Length must match this topology.
     */
    fun initialize(input: IntArray): Boolean {
        if (input.size != board.size || input.any { it !in 0..n }) return false
        input.copyInto(board)
        for (c in board.indices) {
            if (board[c] != 0) {
                if (topology.peers[c].any { board[it] == board[c] }) return false
                masks[c] = 0
            } else {
                var used = 0
                for (p in topology.peers[c]) if (board[p] != 0) used = used or bit(board[p])
                masks[c] = topology.fullMask and used.inv()
            }
        }
        return true
    }

    fun has(c: Int, v: Int): Boolean = masks[c] and bit(v) != 0
    fun positions(unit: Int, value: Int): IntArray = topology.units[unit].filter { has(it, value) }.toIntArray()
    fun contradiction(): Boolean {
        for (c in board.indices) if (board[c] == 0 && masks[c] == 0) return true
        for (unit in topology.units) {
            control.tick()
            var seen = 0; var possible = 0
            for (c in unit) {
                if (board[c] != 0) {
                    val b = bit(board[c]); if (seen and b != 0) return true; seen = seen or b
                }
                possible = possible or masks[c]
            }
            if ((possible or seen) != topology.fullMask) return true
        }
        return false
    }

    /**
     * Builds a placement and its direct peer eliminations without applying them.
     *
     * @param technique Technique supporting the placement.
     * @param c Zero-based target cell index.
     * @param v One-based value currently present in the target candidate mask.
     * @param units Unit indices supporting the witness: rows, then columns, then boxes.
     */
    fun place(technique: Technique, c: Int, v: Int, units: List<Int> = emptyList()): LogicalStep {
        check(has(c, v))
        return LogicalStep(technique, n, placements = listOf(Candidate(c, v)),
            eliminations = topology.peers[c].filter { has(it, v) }.map { Candidate(it, v) },
            supportCells = listOf(c), symbols = listOf(v), units = units)
    }

    /**
     * Builds an elimination witness, or returns null for an empty change set.
     *
     * @param technique Technique supporting the eliminations.
     * @param changes Candidates to remove; each must exist in the current state.
     * @param support Ordered witness cell indices.
     * @param symbols One-based symbols participating in the deduction.
     * @param units Witness unit indices: rows, then columns, then boxes.
     * @param links Ordered inference links for a chain witness.
     * @param colors Candidate color assignments for a coloring witness.
     * @param split Number of cells in the first ALS; zero for other techniques.
     */
    fun eliminate(
        technique: Technique, changes: Collection<Candidate>, support: Collection<Int>, symbols: Collection<Int>,
        units: Collection<Int> = emptyList(), links: Collection<InferenceLink> = emptyList(),
        colors: Collection<ColoredCandidate> = emptyList(), split: Int = 0
    ): LogicalStep? {
        if (changes.isEmpty()) return null
        check(changes.all { has(it.cell, it.value) })
        return LogicalStep(technique, n, eliminations = changes, supportCells = support,
            symbols = symbols, units = units, links = links, colors = colors, supportSplit = split)
    }

    /**
     * Effects are validated before any mutation. No public API accepts an untrusted LogicalStep.
     *
     * @param step Trusted logical move whose effects are validated before the state is mutated.
     */
    fun apply(step: LogicalStep) {
        check(step.placements.isNotEmpty() || step.eliminations.isNotEmpty())
        check(step.placements.all { board[it.cell] == 0 && has(it.cell, it.value) })
        check(step.eliminations.all { has(it.cell, it.value) })
        for (p in step.placements) { board[p.cell] = p.value; masks[p.cell] = 0 }
        for (e in step.eliminations) masks[e.cell] = masks[e.cell] and bit(e.value).inv()
    }

    /**
     * Lists candidate removals in cells that see every witness cell.
     *
     * @param value One-based symbol to remove.
     * @param seenBy Witness cells that every target must see.
     * @param except Cell indices excluded from the targets; defaults to the witness cells.
     */
    fun commonEliminations(value: Int, seenBy: IntArray, except: IntArray = seenBy): List<Candidate> {
        if (seenBy.isEmpty()) return emptyList()
        var out: ArrayList<Candidate>? = null
        for (c in topology.peers[seenBy[0]]) {
            if (has(c, value) && c !in except && seenBy.all { topology.sees(c, it) }) {
                if (out == null) out = ArrayList()
                out.add(Candidate(c, value))
            }
        }
        return out ?: emptyList()
    }
}
