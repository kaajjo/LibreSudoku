package com.kaajjo.libresudoku.generation.validation

import com.kaajjo.libresudoku.core.generator.rating.*

internal data class Fixture(val name: String, val technique: Technique, val state: LogicalState)
internal fun fresh(g: SudokuGeometry=SudokuGeometry(9,3,3))=LogicalState(Topology.of(g),EvaluationControl(EvaluationLimits(maxOperations=100_000_000),{})).also {
    check(it.initialize(IntArray(g.cellCount)))
}
internal fun restrictDigit(s: LogicalState,u: Int,v: Int,keep: IntArray) {
    for (c in s.topology.units[u]) if (c !in keep) s.masks[c]=s.masks[c] and bit(v).inv()
}
internal fun set(s: LogicalState,c: Int,vararg v: Int) { s.masks[c]=v.fold(0) { m,d->m or bit(d) } }
internal fun fixtures(): List<Fixture> {
    val out=ArrayList<Fixture>()
    fun add(name:String,tech:Technique,build:(LogicalState)->Unit) { val s=fresh(); build(s); out+=Fixture(name,tech,s) }
    add("naked-triple",Technique.NAKED_TRIPLE) { s -> set(s,0,1,2); set(s,3,2,3); set(s,6,1,3) }
    add("hidden-triple",Technique.HIDDEN_TRIPLE) { s ->
        restrictDigit(s,0,1,intArrayOf(0,3)); restrictDigit(s,0,2,intArrayOf(3,6)); restrictDigit(s,0,3,intArrayOf(0,6))
    }
    add("naked-quad",Technique.NAKED_QUAD) { s -> set(s,0,1,2);set(s,2,2,3);set(s,4,3,4);set(s,6,1,4) }
    add("hidden-quad",Technique.HIDDEN_QUAD) { s ->
        restrictDigit(s,0,1,intArrayOf(0,6));restrictDigit(s,0,2,intArrayOf(0,2));restrictDigit(s,0,3,intArrayOf(2,4));restrictDigit(s,0,4,intArrayOf(4,6))
    }
    add("x-wing",Technique.X_WING) { s -> restrictDigit(s,0,1,intArrayOf(0,4));restrictDigit(s,3,1,intArrayOf(27,31)) }
    add("swordfish",Technique.SWORDFISH) { s -> restrictDigit(s,0,1,intArrayOf(0,3));restrictDigit(s,3,1,intArrayOf(30,33));restrictDigit(s,6,1,intArrayOf(54,60)) }
    add("jellyfish",Technique.JELLYFISH) { s ->
        restrictDigit(s,0,1,intArrayOf(0,2));restrictDigit(s,2,1,intArrayOf(20,22));
        restrictDigit(s,4,1,intArrayOf(40,42));restrictDigit(s,6,1,intArrayOf(54,60))
    }
    add("xy-wing",Technique.XY_WING) { s -> set(s,0,1,2);set(s,4,1,3);set(s,36,2,3) }
    add("xyz-wing",Technique.XYZ_WING) { s -> set(s,0,1,2,3);set(s,4,1,3);set(s,9,2,3) }
    add("w-wing",Technique.W_WING) { s -> set(s,0,1,2);set(s,40,1,2);restrictDigit(s,11,1,intArrayOf(2,38)) }
    add("skyscraper",Technique.SKYSCRAPER) { s -> restrictDigit(s,0,1,intArrayOf(0,3));restrictDigit(s,3,1,intArrayOf(27,31)) }
    add("two-string-kite",Technique.TWO_STRING_KITE) { s -> restrictDigit(s,0,1,intArrayOf(0,4));restrictDigit(s,10,1,intArrayOf(10,37)) }
    add("finned-x-wing",Technique.FINNED_X_WING) { s -> restrictDigit(s,0,1,intArrayOf(0,3));restrictDigit(s,3,1,intArrayOf(27,30,31)) }
    add("empty-rectangle",Technique.EMPTY_RECTANGLE) { s -> restrictDigit(s,18,1,intArrayOf(0,1,9));restrictDigit(s,13,1,intArrayOf(4,40)) }
    add("color-trap",Technique.COLOR_TRAP) { s -> restrictDigit(s,0,1,intArrayOf(0,4));restrictDigit(s,13,1,intArrayOf(4,40));restrictDigit(s,4,1,intArrayOf(37,40)) }
    add("color-wrap",Technique.COLOR_WRAP) { s -> restrictDigit(s,0,1,intArrayOf(0,4));restrictDigit(s,13,1,intArrayOf(4,40));restrictDigit(s,4,1,intArrayOf(37,40));restrictDigit(s,10,1,intArrayOf(10,37)) }
    add("x-chain",Technique.X_CHAIN) { s -> restrictDigit(s,0,1,intArrayOf(0,4));restrictDigit(s,10,1,intArrayOf(10,37)) }
    add("xy-chain-four-cells",Technique.XY_CHAIN) { s -> set(s,0,1,2);set(s,4,2,3);set(s,40,3,4);set(s,37,4,1) }
    add("aic-mixed-strong-links",Technique.AIC) { s -> set(s,0,1,2);set(s,40,1,2);restrictDigit(s,13,2,intArrayOf(4,40));restrictDigit(s,4,1,intArrayOf(37,40)) }
    add("als-xz-two-plus-two",Technique.ALS_XZ) { s -> set(s,0,1,2);set(s,4,2,3);set(s,9,1,4);set(s,13,3,4) }
    return out
}
internal fun findTechnique(s: LogicalState,technique: Technique): LogicalStep? = when(technique) {
    Technique.NAKED_TRIPLE -> BasicTechniques.subset(s,3,false)
    Technique.HIDDEN_TRIPLE -> BasicTechniques.subset(s,3,true)
    Technique.NAKED_QUAD -> BasicTechniques.subset(s,4,false)
    Technique.HIDDEN_QUAD -> BasicTechniques.subset(s,4,true)
    Technique.X_WING -> FishTechniques.basic(s,2)
    Technique.SWORDFISH -> FishTechniques.basic(s,3)
    Technique.JELLYFISH -> FishTechniques.basic(s,4)
    Technique.XY_WING -> WingTechniques.xyOrXyz(s,false)
    Technique.XYZ_WING -> WingTechniques.xyOrXyz(s,true)
    Technique.W_WING -> WingTechniques.wWing(s)
    Technique.SKYSCRAPER -> SingleDigitPatterns.shortChain(s,false)
    Technique.TWO_STRING_KITE -> SingleDigitPatterns.shortChain(s,true)
    Technique.FINNED_X_WING -> FishTechniques.finnedXWing(s)
    Technique.EMPTY_RECTANGLE -> SingleDigitPatterns.emptyRectangle(s)
    Technique.COLOR_WRAP -> SingleDigitPatterns.coloring(s,true)
    Technique.COLOR_TRAP -> SingleDigitPatterns.coloring(s,false)
    Technique.X_CHAIN,Technique.XY_CHAIN,Technique.AIC -> ChainTechniques.find(s,technique)
    Technique.ALS_XZ -> AlsTechniques.xz(s)
    else -> error("Unexpected test technique $technique")
}

/** Every elimination must be UNSAT when forced in the PRE-step candidate state. */
internal fun certify(s: LogicalState,step: LogicalStep,allEffects: Boolean=true): Int {
    val oracle=IndependentOracle(s.topology.geometry)
    check(oracle.solve(s.board,s.masks).isNotEmpty()) { "Vacuous test: pre-state has no completion (${step.technique})" }
    var checked=0
    for (e in if (allEffects) step.eliminations else step.eliminations.take(1)) {
        val masks=s.masks.copyOf(); masks[e.cell]=bit(e.value)
        check(oracle.solve(s.board,masks).isEmpty()) { "UNSOUND ${step.technique}: $e has a counterexample" }
        checked++
    }
    for (p in step.placements) {
        val masks=s.masks.copyOf(); masks[p.cell]=masks[p.cell] and bit(p.value).inv()
        check(oracle.solve(s.board,masks).isEmpty()) { "UNSOUND placement ${step.technique}: $p" }
        checked++
    }
    return checked
}
