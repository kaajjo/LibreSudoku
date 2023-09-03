package com.kaajjo.libresudoku.ui.game

data class GameScreenNavArgs(
    val gameUid: Long,
    val playedBefore: Boolean = false
)
