package com.kaajjo.libresudoku.core.generator.rating

internal object BasicTechniques {

    /**
     * Finds a Full House or single-candidate placement.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param hidden Whether hidden singles are allowed after Full House and naked singles.
     */
    fun singles(s: LogicalState, hidden: Boolean): LogicalStep? {
        // Classify a last empty cell in a unit before generic singles. This matches the
        // scoring convention of Full House and keeps the witness replayable.
        for (u in s.topology.units.indices) {
            s.control.tick()
            var empty = -1
            for (c in s.topology.units[u]) if (s.board[c] == 0) {
                if (empty >= 0) { empty = -1; break }
                empty = c
            }
            if (empty >= 0 && population(s.masks[empty]) == 1) return s.place(
                Technique.FULL_HOUSE, empty, firstValue(s.masks[empty]), listOf(u)
            )
        }
        for (c in s.board.indices) if (population(s.masks[c]) == 1) return s.place(
            Technique.SINGLE,
            c,
            firstValue(s.masks[c])
        )
        if (!hidden) return null
        val n = s.n

        for (u in (2 * n until 3 * n) + (0 until 2 * n)) {
            s.control.tick()
            for (v in 1..n) {
                var count = 0;
                var cell = -1
                for (c in s.topology.units[u]) if (s.has(c, v)) {
                    count++; cell = c
                }
                if (count == 1) {
                    val tech =
                        if (u < n) Technique.HIDDEN_SINGLE_ROW else if (u < 2 * n) Technique.HIDDEN_SINGLE_COLUMN else Technique.HIDDEN_SINGLE_SECTION
                    return s.place(tech, cell, v, listOf(u))
                }
            }
        }
        return null
    }

    /**
     * Finds a naked or hidden subset elimination in one unit.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param k Subset size: two for pairs, three for triples, four for quads.
     * @param hidden Search symbol-to-cell subsets when true, cell-to-symbol subsets otherwise.
     */
    fun subset(s: LogicalState, k: Int, hidden: Boolean): LogicalStep? {
        val n = s.n
        for (u in s.topology.units.indices) {
            s.control.check()
            val unit = s.topology.units[u]
            val pool = if (!hidden) unit.filter { population(s.masks[it]) in 2..k }.toIntArray()
            else (1..n).filter { v -> unit.count { s.has(it, v) } in 1..k }.toIntArray()
            var result: LogicalStep? = null
            combinations(pool.size, k, s.control) { combo ->
                var union = 0
                if (hidden) {
                    for (i in combo) for (p in unit.indices) if (s.has(unit[p], pool[i])) union =
                        union or (1 shl p)
                } else for (i in combo) union = union or s.masks[pool[i]]
                if (population(union) != k) return@combinations false
                val chosenCells: List<Int>
                val symbols: Int
                if (hidden) {
                    chosenCells = unit.indices.filter { union and (1 shl it) != 0 }.map { unit[it] }
                    symbols = combo.fold(0) { m, i -> m or bit(pool[i]) }
                } else {
                    chosenCells = combo.map { pool[it] }; symbols = union
                }
                val changes = ArrayList<Candidate>()
                for (c in unit) {
                    val remove = if (hidden && c in chosenCells) s.masks[c] and symbols.inv()
                    else if (!hidden && c !in chosenCells) s.masks[c] and symbols else 0
                    for (v in values(remove)) changes += Candidate(c, v)
                }
                val tech = when (k) {
                    2 -> if (hidden) when {
                        u < n -> Technique.HIDDEN_PAIR_ROW; u < 2 * n -> Technique.HIDDEN_PAIR_COLUMN; else -> Technique.HIDDEN_PAIR_SECTION
                    }
                    else when {
                        u < n -> Technique.NAKED_PAIR_ROW; u < 2 * n -> Technique.NAKED_PAIR_COLUMN; else -> Technique.NAKED_PAIR_SECTION
                    }

                    3 -> if (hidden) Technique.HIDDEN_TRIPLE else Technique.NAKED_TRIPLE
                    else -> if (hidden) Technique.HIDDEN_QUAD else Technique.NAKED_QUAD
                }
                result = s.eliminate(tech, changes, chosenCells, values(symbols), listOf(u))
                result != null
            }
            if (result != null) return result
        }
        return null
    }

    /**
     * Finds a pointing or claiming elimination across intersecting units.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     */
    fun locked(s: LogicalState): LogicalStep? {
        val t = s.topology;
        val n = s.n
        // A digit confined to U intersect V cannot occur in V outside U.
        for (u in (2 * n until 3 * n) + (0 until 2 * n)) {
            s.control.check()
            for (digit in 1..n) {
                val cells = s.positions(u, digit)
                if (cells.size < 2) continue
                for (v in t.cellUnits[cells[0]]) {
                    if (v == u || (u < 2 * n && v < 2 * n)) continue
                    if (!cells.all { v in t.cellUnits[it] }) continue
                    val changes = t.units[v].filter { u !in t.cellUnits[it] && s.has(it, digit) }
                        .map { Candidate(it, digit) }
                    val technique = when {
                        u >= 2 * n && v < n -> Technique.POINTING_PAIR_TRIPLE_ROW
                        u >= 2 * n -> Technique.POINTING_PAIR_TRIPLE_COLUMN
                        u < n -> Technique.ROW_BOX
                        else -> Technique.COLUMN_BOX
                    }
                    s.eliminate(technique, changes, cells.toList(), listOf(digit), listOf(u, v))
                        ?.let { return it }
                }
            }
        }
        return null
    }
}
