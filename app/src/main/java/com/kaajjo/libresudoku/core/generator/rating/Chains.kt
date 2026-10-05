package com.kaajjo.libresudoku.core.generator.rating

/**
 * Bounded, STATIC, ungrouped implication graph. No hypothetical board mutation, no recursive
 * solver, no branch merging. Strong links exist only in a bivalue cell or conjugate house pair.
 * false -> true uses a strong link; true -> false uses a weak link.
 */
internal object ChainTechniques {
    private enum class Mode { X, XY, AIC }

    /**
     * Searches a bounded static implication graph for a chain deduction.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param technique Chain family to search: X_CHAIN, XY_CHAIN or AIC.
     */
    fun find(s: LogicalState, technique: Technique): LogicalStep? {
        val mode = when (technique) { Technique.X_CHAIN -> Mode.X; Technique.XY_CHAIN -> Mode.XY; else -> Mode.AIC }
        val t = s.topology; val n = s.n
        val candidates = t.cells*n; val literals = candidates*2
        val positionMasks = Array(3*n) { IntArray(n) }
        for (u in t.units.indices) for (p in 0 until n) {
            var m = s.masks[t.units[u][p]]
            while (m != 0) {
                val v = firstValue(m)-1; m = m and (m-1)
                positionMasks[u][v] = positionMasks[u][v] or (1 shl p)
            }
        }
        val stamp = IntArray(literals); val parent = IntArray(literals); val parentUnit = IntArray(literals)
        val depth = IntArray(literals); val queue = IntArray(literals)
        var iteration = 0
        fun cand(id: Int) = Candidate(id/n,id%n+1)
        fun path(end: Int): List<InferenceLink> {
            val links = ArrayList<InferenceLink>()
            var c = end
            while (parent[c] != -1) {
                val p = parent[c]
                links += InferenceLink(cand(p/2),cand(c/2),if (p and 1 == 0) LinkKind.STRONG else LinkKind.WEAK,parentUnit[c])
                c=p
            }
            links.reverse(); return links
        }
        for (cell in s.board.indices) {
            if (mode == Mode.XY && population(s.masks[cell]) != 2) continue
            for (digit in values(s.masks[cell])) for (truth in 0..if (mode == Mode.AIC) 1 else 0) {
                s.control.check(); iteration++
                val startId = cell*n+digit-1; val root = startId*2+truth
                var head=0; var tail=1; queue[0]=root; stamp[root]=iteration; parent[root]=-1; depth[root]=0
                fun enqueue(from: Int, next: Int, unit: Int) {
                    s.control.tick()
                    if (stamp[next] == iteration) return
                    stamp[next]=iteration; parent[next]=from; parentUnit[next]=unit; depth[next]=depth[from]+1
                    queue[tail++]=next
                }
                while (head < tail) {
                    val lit = queue[head++]; val id=lit/2; val c=id/n; val v=id%n+1; val isTrue=lit and 1 != 0
                    val length=depth[lit]
                    if (length >= 3) {
                        // Discontinuous AIC: premise implies its negation.
                        if (mode == Mode.AIC && lit == (root xor 1)) {
                            val links=path(lit)
                            val support=links.flatMap { listOf(it.from.cell,it.to.cell) }.distinct()
                            if (truth == 1) return s.eliminate(technique,listOf(Candidate(cell,digit)),support,
                                links.flatMap { listOf(it.from.value,it.to.value) }.distinct(),links=links)
                            val place=s.place(technique,cell,digit)
                            return LogicalStep(technique,n,place.placements,place.eliminations,support,
                                links.flatMap { listOf(it.from.value,it.to.value) }.distinct(),links=links)
                        }
                        // If A is false then B is true: A or B. Same-digit endpoints eliminate
                        // that digit from all cells seeing both. This is Type-1 endpoint logic.
                        if (truth == 0 && isTrue && v == digit && c != cell) {
                            val changes=s.commonEliminations(digit,intArrayOf(cell,c))
                            if (changes.isNotEmpty()) {
                                val links=path(lit)
                                return s.eliminate(technique,changes,links.flatMap { listOf(it.from.cell,it.to.cell) }.distinct(),
                                    links.flatMap { listOf(it.from.value,it.to.value) }.distinct(),links=links)
                            }
                        }
                    }
                    if (length >= s.control.limits.maxChainLinks) continue
                    if (!isTrue) {
                        // Strong link inside a bivalue cell.
                        if (mode != Mode.X && population(s.masks[c]) == 2) {
                            val other=firstValue(s.masks[c] and bit(v).inv())
                            enqueue(lit,(c*n+other-1)*2+1,-1)
                        }
                        // Strong link in a house containing exactly two positions for v.
                        if (mode != Mode.XY) for (u in t.cellUnits[c]) {
                            val pMask=positionMasks[u][v-1]
                            if (population(pMask) != 2) continue
                            var pm=pMask
                            while (pm != 0) {
                                val p=Integer.numberOfTrailingZeros(pm); pm=pm and (pm-1)
                                val other=t.units[u][p]
                                if (other != c) enqueue(lit,(other*n+v-1)*2+1,u)
                            }
                        }
                    } else {
                        if (mode == Mode.AIC) for (other in values(s.masks[c] and bit(v).inv())) {
                            enqueue(lit,(c*n+other-1)*2,-1)
                        }
                        for (p in t.peers[c]) if (s.has(p,v) && (mode != Mode.XY || population(s.masks[p]) == 2)) {
                            enqueue(lit,(p*n+v-1)*2,t.sharedUnit(c,p))
                        }
                    }
                }
            }
        }
        return null
    }
}
