package com.kaajjo.libresudoku.core.utils

import android.util.Log
import com.kaajjo.libresudoku.data.database.model.SudokuBoard

/**
 * .sdm - is a very simple format.
 * Each line is a single puzzle.
 * Empty cells can be represented by a zero or a dot
 * Example: 000605000003020800045090270500000001062000540400000007098060450006040700000203000
 */
class SdmParser {
    private val tag = "SDMParser"

    fun textToStringBoards(text: String): Pair<Boolean, List<String>> {
        val toImport = mutableListOf<String>()
        if (text.isEmpty()) return Pair(false, toImport)

        text.lines().forEach {
            val line = it.trim()
            Log.d(tag, "Current line: $line")
            if (line.length == 81) {
                toImport.add(line.replace(".", "0"))
            } else {
                Log.i(tag, "This line was skipped: $line")
            }
        }
        Log.d(tag, "About to return: ${toImport.size} boards")

        return Pair(true, toImport)
    }

    fun boardsToSdm(boards: List<SudokuBoard>): String {
        val stringBuilder = StringBuilder()
        boards.forEach { game ->
            stringBuilder.append(game.initialBoard + "\n")
        }
        // Remove the extra \n that was added in the loop above
        stringBuilder.delete(stringBuilder.length - 1, stringBuilder.length)
        return stringBuilder.toString()
    }
}