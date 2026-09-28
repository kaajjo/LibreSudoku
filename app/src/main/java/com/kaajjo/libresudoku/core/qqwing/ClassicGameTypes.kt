package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.generator.rating.SudokuGeometry

/** Single app-level registry. Add a future CLASSICAL type here and an explicit search profile. */
object ClassicGameTypes {
    /**
     * Checks whether the type has classical constraints supported by this pipeline.
     *
     * @param type Variant to check; Killer and Unspecified are not classical entries.
     */
    fun isSupported(type: GameType): Boolean = when (type) {
        GameType.Default6x6, GameType.Default9x9, GameType.Default12x12 -> true
        else -> false
    }

    /**
     * Resolves the dimensions of a supported classical variant.
     *
     * @param type Explicit classical variant defining the grid and rectangular box dimensions.
     */
    fun geometryOf(type: GameType): SudokuGeometry {
        require(isSupported(type)) { "No classical rules registered for $type; Killer requires cage rules" }
        return SudokuGeometry(type.size, type.sectionHeight, type.sectionWidth)
    }
}
