package com.kaajjo.libresudoku.core.qqwing.advanced_hint

/**
 * With this, you can enable or disable techniques for advanced hint
 *
 * @constructor Everything is enabled by default
 */
data class AdvancedHintSettings(
    val fullHouse: Boolean = true,
    val nakedSingle: Boolean = true,
    val hiddenSingle: Boolean = true,
    val checkWrongValue: Boolean = true
)
