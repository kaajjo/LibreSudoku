package com.kaajjo.libresudoku.core.generator.rating

/**
 * Disjoint ALS-XZ, one selected restricted common candidate (RCC), ALS sizes <= configured cap
 */
internal object AlsTechniques {

    private data class Als(val cells: IntArray, val symbols: Int, val unit: Int)

    /**
     * Finds eliminations from two disjoint almost-locked sets.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     */
    fun xz(s: LogicalState): LogicalStep? {
        val t = s.topology;
        val sets = ArrayList<Als>();
        val seen = HashSet<Long>()
        for (u in t.units.indices) {
            s.control.check()
            for (k in 1..s.control.limits.maxAlsSize) {
                val pool = t.units[u].filter { population(s.masks[it]) in 2..k + 1 }.toIntArray()
                combinations(pool.size, k, s.control) { indices ->
                    var union = 0
                    for (i in indices) union = union or s.masks[pool[i]]
                    if (population(union) == k + 1) {
                        val cells = indices.map { pool[it] }.sorted().toIntArray()
                        var key = k.toLong()
                        for (c in cells) key = (key shl 10) or (c + 1).toLong()
                        if (seen.add(key)) sets += Als(cells, union, u)
                    }
                    false
                }
            }
        }

        for (i in sets.indices) for (j in i + 1 until sets.size) {
            s.control.tick()
            val a = sets[i];
            val b = sets[j];
            val common = a.symbols and b.symbols
            if (population(common) < 2 || a.cells.any { it in b.cells }) continue
            for (x in values(common)) {
                val ax = a.cells.filter { s.has(it, x) };
                val bx = b.cells.filter { s.has(it, x) }
                if (!ax.all { ac -> bx.all { bc -> t.sees(ac, bc) } }) continue
                for (z in values(common and bit(x).inv())) {
                    val az = a.cells.filter { s.has(it, z) };
                    val bz = b.cells.filter { s.has(it, z) }
                    val support = a.cells + b.cells
                    val changes = s.commonEliminations(z, (az + bz).toIntArray(), support)
                    // If z were excluded from both ALS, both would become locked and require x.
                    // The RCC prevents both x occurrences being true, so z occurs in at least one ALS.
                    s.eliminate(
                        Technique.ALS_XZ,
                        changes,
                        support.toList(),
                        listOf(x, z),
                        listOf(a.unit, b.unit),
                        split = a.cells.size
                    )?.let { return it }
                }
            }
        }

        return null
    }
}
