package com.kaajjo.libresudoku.core.utils

import com.kaajjo.libresudoku.data.database.model.SudokuBoard

class SdmParser {

    fun boardsToSdm(boards: List<SudokuBoard>): String {
        val stringBuilder = StringBuilder()
        boards.forEach { game ->
            stringBuilder.append(game.initialBoard + "\n")
        }
        // Remove the extra \n that was added in the loop
        stringBuilder.delete(stringBuilder.length - 1, stringBuilder.length)
        return stringBuilder.toString()
    }
}