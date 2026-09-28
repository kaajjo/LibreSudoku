package com.kaajjo.libresudoku.core.generator.rating

internal object SingleDigitPatterns {
    /**
     * Finds a short single-digit chain pattern.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param kite Search Two-String Kite when true; search Skyscraper otherwise.
     */
    fun shortChain(s: LogicalState, kite: Boolean): LogicalStep? {
        val t = s.topology; val n = s.n
        for (digit in 1..n) {
            s.control.check()
            for (u in 0 until 2*n) {
                val first = s.positions(u,digit)
                if (first.size != 2) continue
                for (v in u+1 until 2*n) {
                    s.control.tick()
                    val sameOrientation = u/n == v/n
                    if (kite == sameOrientation) continue
                    val second = s.positions(v,digit)
                    if (second.size != 2 || second.any { it in first }) continue
                    for (i in 0..1) for (j in 0..1) {
                        val a = first[1-i]; val p = first[i]; val q = second[j]; val b = second[1-j]
                        val connected = if (kite) t.box[p] == t.box[q]
                            else if (u < n) t.col[p] == t.col[q] else t.row[p] == t.row[q]
                        if (!connected || (!kite && (if (u<n) t.col[a] == t.col[b] else t.row[a] == t.row[b]))) continue
                        val changes = s.commonEliminations(digit,intArrayOf(a,b),intArrayOf(a,p,q,b))
                        s.eliminate(if (kite) Technique.TWO_STRING_KITE else Technique.SKYSCRAPER,
                            changes,listOf(a,p,q,b),listOf(digit),listOf(u,v),listOf(
                                InferenceLink(Candidate(a,digit),Candidate(p,digit),LinkKind.STRONG,u),
                                InferenceLink(Candidate(p,digit),Candidate(q,digit),LinkKind.WEAK,t.sharedUnit(p,q)),
                                InferenceLink(Candidate(q,digit),Candidate(b,digit),LinkKind.STRONG,v)
                            ))?.let { return it }
                    }
                }
            }
        }
        return null
    }

    /**
     * Standard box row/column ER with >=3 candidates, not the two-candidate turbot form.
     *
     * @param s Current candidate state and evaluation control; detection does not apply the returned move.
     */
    fun emptyRectangle(s: LogicalState): LogicalStep? {
        val t = s.topology; val n = s.n
        for (box in 0 until n) for (digit in 1..n) {
            s.control.check()
            val support = s.positions(2*n+box,digit)
            if (support.size < 3) continue
            val rows = support.map { t.row[it] }.distinct()
            val cols = support.map { t.col[it] }.distinct()
            for (row in rows) for (col in cols) {
                s.control.tick()
                if (support.any { t.row[it] != row && t.col[it] != col }) continue
                if (support.none { t.row[it] != row } || support.none { t.col[it] != col }) continue
                for (orientation in 0..1) for (line in 0 until n) {
                    val u = if (orientation == 0) n+line else line
                    val pair = s.positions(u,digit)
                    if (pair.size != 2 || pair.any { t.box[it] == box }) continue
                    for (o in 0..1) {
                        val p = pair[o]; val q = pair[1-o]
                        if (orientation == 0 && t.row[p] != row || orientation == 1 && t.col[p] != col) continue
                        val target = if (orientation == 0) t.row[q]*n+col else row*n+t.col[q]
                        if (!s.has(target,digit) || target in support || target in pair) continue
                        // If q is true, target is false. Otherwise p is true; all remaining box
                        // candidates must lie in the other arm and all see target.
                        val remaining = support.filter { !t.sees(p,it) }
                        if (remaining.isEmpty() || !t.sees(q,target) || remaining.any { !t.sees(target,it) }) continue
                        return s.eliminate(Technique.EMPTY_RECTANGLE,listOf(Candidate(target,digit)),
                            support.toList()+listOf(p,q),listOf(digit),listOf(2*n+box,row,n+col,u),
                            listOf(InferenceLink(Candidate(p,digit),Candidate(q,digit),LinkKind.STRONG,u)))
                    }
                }
            }
        }
        return null
    }

    /**
     * Finds eliminations supported by a two-color conjugate component.
     *
     * @param s Current candidate state and evaluation control; the returned move is not applied.
     * @param wrapOnly True restricts to color wraps, false to color traps, null permits both.
     */
    fun coloring(s: LogicalState, wrapOnly: Boolean? = null): LogicalStep? {
        val t = s.topology
        for (digit in 1..s.n) {
            s.control.check()
            val adjacency = Array(t.cells) { ArrayList<Pair<Int,Int>>(3) }
            for (u in t.units.indices) {
                val pair = s.positions(u,digit)
                if (pair.size == 2) {
                    adjacency[pair[0]].add(pair[1] to u); adjacency[pair[1]].add(pair[0] to u)
                }
            }
            val visited = BooleanArray(t.cells)
            for (start in s.board.indices) {
                if (visited[start] || adjacency[start].isEmpty()) continue
                val color = IntArray(t.cells) { -1 }; val queue = IntArray(t.cells)
                var head = 0; var tail = 1; queue[0] = start; color[start]=0; visited[start]=true
                val links = ArrayList<InferenceLink>(); var bipartite = true
                while (head < tail) {
                    s.control.tick()
                    val c = queue[head++]
                    for ((other,u) in adjacency[c]) {
                        if (color[other] == -1) {
                            color[other] = color[c] xor 1; visited[other]=true; queue[tail++]=other
                            links += InferenceLink(Candidate(c,digit),Candidate(other,digit),LinkKind.STRONG,u)
                        } else if (color[other] == color[c]) bipartite=false
                    }
                }
                // An odd conjugate cycle means inconsistent candidates. Do not invent a coloring.
                // The exact solver at the boundary diagnoses invalid puzzles; this detector skips it.
                if (!bipartite) continue
                val members = queue.copyOf(tail)
                if (wrapOnly != false) for (i in members.indices) for (j in i+1 until members.size) {
                    val a = members[i]; val b = members[j]
                    if (color[a] == color[b] && t.sees(a,b)) {
                        val changes = members.filter { color[it] == color[a] }.map { Candidate(it,digit) }
                        return s.eliminate(Technique.COLOR_WRAP,changes,members.toList(),listOf(digit),
                            listOf(t.sharedUnit(a,b)),links,members.map { ColoredCandidate(Candidate(it,digit),color[it]) })
                    }
                }
                if (wrapOnly != true) {
                    val changes = ArrayList<Candidate>()
                    for (c in s.board.indices) if (color[c] == -1 && s.has(c,digit)) {
                        var sees0 = false; var sees1 = false
                        for (p in members) if (t.sees(c,p)) { if (color[p] == 0) sees0=true else sees1=true }
                        if (sees0 && sees1) changes += Candidate(c,digit)
                    }
                    s.eliminate(Technique.COLOR_TRAP,changes,members.toList(),listOf(digit),links=links,
                        colors=members.map { ColoredCandidate(Candidate(it,digit),color[it]) })?.let { return it }
                }
            }
        }
        return null
    }
}
