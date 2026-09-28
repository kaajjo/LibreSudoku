package com.kaajjo.libresudoku.core.generator.rating

/** Classical row/column fish only; no mutant, Franken, endofin or grouped variants. */
internal object FishTechniques {
    /**
     * Finds a classical row/column fish elimination.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param k Fish size: two for X-Wing, three for Swordfish, four for Jellyfish.
     */
    fun basic(s: LogicalState, k: Int): LogicalStep? {
        val n = s.n
        for (orientation in 0..1) for (digit in 1..n) {
            s.control.check()
            val pos = IntArray(n)
            for (base in 0 until n) for (cover in 0 until n) {
                val c = if (orientation == 0) base * n + cover else cover * n + base
                if (s.has(c, digit)) pos[base] = pos[base] or (1 shl cover)
            }
            val bases = (0 until n).filter { population(pos[it]) in 2..k }.toIntArray()
            var result: LogicalStep? = null
            combinations(bases.size, k, s.control) { combo ->
                var covers = 0; var baseMask = 0
                for (i in combo) { covers = covers or pos[bases[i]]; baseMask = baseMask or (1 shl bases[i]) }
                if (population(covers) != k) return@combinations false
                val changes = ArrayList<Candidate>()
                for (b in 0 until n) if (baseMask and (1 shl b) == 0) {
                    for (c in 0 until n) if (covers and (1 shl c) != 0) {
                        val cell = if (orientation == 0) b*n+c else c*n+b
                        if (s.has(cell,digit)) changes += Candidate(cell,digit)
                    }
                }
                if (changes.isEmpty()) return@combinations false
                val support = ArrayList<Int>()
                for (i in combo) for (c in 0 until n) if (pos[bases[i]] and (1 shl c) != 0) {
                    support += if (orientation == 0) bases[i]*n+c else c*n+bases[i]
                }
                val units = combo.map { orientation*n + bases[it] } +
                    (0 until n).filter { covers and (1 shl it) != 0 }.map { (1-orientation)*n+it }
                result = s.eliminate(when(k) { 2 -> Technique.X_WING; 3 -> Technique.SWORDFISH; else -> Technique.JELLYFISH }, changes, support, listOf(digit), units)
                true
            }
            if (result != null) return result
        }
        return null
    }

    /**
     * Genuine 2x2 core + nonempty fins all in ONE box. Sashimi deliberately excluded.
     *
     * @param s Current candidate state and evaluation control; detection does not apply the returned move.
     */
    fun finnedXWing(s: LogicalState): LogicalStep? {
        val n = s.n; val t = s.topology
        for (orientation in 0..1) for (digit in 1..n) {
            val pos = IntArray(n)
            for (b in 0 until n) for (c in 0 until n) {
                val cell = if (orientation == 0) b*n+c else c*n+b
                if (s.has(cell,digit)) pos[b] = pos[b] or (1 shl c)
            }
            for (b1 in 0 until n) for (b2 in b1+1 until n) {
                val common = (0 until n).filter { pos[b1] and pos[b2] and (1 shl it) != 0 }.toIntArray()
                var result: LogicalStep? = null
                combinations(common.size, 2, s.control) { combo ->
                    val coverMask = (1 shl common[combo[0]]) or (1 shl common[combo[1]])
                    val fins = ArrayList<Int>(); val support = ArrayList<Int>()
                    for (b in intArrayOf(b1,b2)) for (c in 0 until n) if (pos[b] and (1 shl c) != 0) {
                        val cell = if (orientation == 0) b*n+c else c*n+b
                        support += cell
                        if (coverMask and (1 shl c) == 0) fins += cell
                    }
                    if (fins.isEmpty() || fins.any { t.box[it] != t.box[fins[0]] }) return@combinations false
                    val changes = ArrayList<Candidate>()
                    for (b in 0 until n) if (b != b1 && b != b2) for (j in combo) {
                        val c = common[j]; val cell = if (orientation == 0) b*n+c else c*n+b
                        if (s.has(cell,digit) && fins.all { t.sees(cell,it) }) changes += Candidate(cell,digit)
                    }
                    result = s.eliminate(Technique.FINNED_X_WING, changes, support, listOf(digit),
                        listOf(orientation*n+b1, orientation*n+b2, (1-orientation)*n+common[combo[0]],
                            (1-orientation)*n+common[combo[1]], 2*n+t.box[fins[0]]))
                    result != null
                }
                if (result != null) return result
            }
        }
        return null
    }
}

internal object WingTechniques {
    /**
     * Finds a wing elimination with a bivalue or trivalue pivot.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param xyz Use a trivalue XYZ-Wing pivot when true; use a bivalue XY-Wing pivot otherwise.
     */
    fun xyOrXyz(s: LogicalState, xyz: Boolean): LogicalStep? {
        val t = s.topology
        for (pivot in s.board.indices) {
            s.control.check()
            if (population(s.masks[pivot]) != if (xyz) 3 else 2) continue
            val wings = t.peers[pivot].filter { population(s.masks[it]) == 2 }.toIntArray()
            for (i in wings.indices) for (j in i+1 until wings.size) {
                s.control.tick()
                val a = wings[i]; val b = wings[j]; val pm = s.masks[pivot]
                val am = s.masks[a]; val bm = s.masks[b]; val common = am and bm
                if (population(common) != 1 || population(am or bm or pm) != 3) continue
                if (xyz) {
                    if ((am or bm) != pm) continue
                } else {
                    if (pm and common != 0 || population(pm and am) != 1 || population(pm and bm) != 1) continue
                }
                val digit = firstValue(common)
                val seenBy = if (xyz) intArrayOf(pivot,a,b) else intArrayOf(a,b)
                val changes = s.commonEliminations(digit, seenBy, intArrayOf(pivot,a,b))
                s.eliminate(if (xyz) Technique.XYZ_WING else Technique.XY_WING, changes,
                    listOf(pivot,a,b), values(pm or common))?.let { return it }
            }
        }
        return null
    }

    /**
     * Finds a W-Wing connected by a conjugate pair.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     */
    fun wWing(s: LogicalState): LogicalStep? {
        val t = s.topology
        val bivalue = s.board.indices.filter { population(s.masks[it]) == 2 }.toIntArray()
        for (i in bivalue.indices) for (j in i+1 until bivalue.size) {
            s.control.check()
            val a = bivalue[i]; val b = bivalue[j]
            if (s.masks[a] != s.masks[b] || t.sees(a,b)) continue
            for (x in values(s.masks[a])) {
                val z = firstValue(s.masks[a] and bit(x).inv())
                for (u in t.units.indices) {
                    s.control.tick()
                    val pair = s.positions(u,x)
                    if (pair.size != 2 || a in pair || b in pair) continue
                    for (o in 0..1) {
                        val p = pair[o]; val q = pair[1-o]
                        if (!t.sees(a,p) || !t.sees(b,q)) continue
                        val changes = s.commonEliminations(z, intArrayOf(a,b), intArrayOf(a,b,p,q))
                        s.eliminate(Technique.W_WING, changes, listOf(a,p,q,b), listOf(x,z), listOf(u),
                            listOf(InferenceLink(Candidate(p,x),Candidate(q,x),LinkKind.STRONG,u)))?.let { return it }
                    }
                }
            }
        }
        return null
    }
}
