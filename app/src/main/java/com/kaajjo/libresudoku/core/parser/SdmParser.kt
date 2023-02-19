package com.kaajjo.libresudoku.core.parser

import android.util.Log
import com.kaajjo.libresudoku.data.database.model.SudokuBoard

/**
 * .sdm - is a very simple format.
 * Each line is a single puzzle.
 * Empty cells can be represented by a zero or a dot
 * Example: 000605000003020800045090270500000001062000540400000007098060450006040700000203000
 */
class SdmParser : FileImportParser {
    private val tag = "SDMParser"

    /**
     * @param content .sdm file content
     * @return Pair with: First - parsing success. Second - strings of parsed boards
     */
    override fun toBoards(content: String): Pair<Boolean, List<String>> {
        val toImport = mutableListOf<String>()
        if (content.isEmpty()) return Pair(false, toImport)

        try {
            content.lines().forEach {
                val line = it.trim()

                if (line.length == 81 && line.all { char -> char.isDigit() }) {
                    toImport.add(line.replace(".", "0"))
                } else {
                    Log.i(tag, "This line was skipped: $line")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception while parsing!")
            e.printStackTrace()
            return Pair(false, toImport)
        }

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