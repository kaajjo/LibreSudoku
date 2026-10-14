package com.kaajjo.libresudoku.core.parser

import com.kaajjo.libresudoku.data.database.model.SudokuBoard

/**
 * .sdm - is a very simple format.
 * Each line is a single puzzle.
 * Empty cells can be represented by a zero or a dot
 * Example: 000605000003020800045090270500000001062000540400000007098060450006040700000203000
 */
class SdmParser : FileImportParser {
    override fun toBoards(content: String): FileImportResult = FileImportResult.fromBoards(
        content.removePrefix("\uFEFF").lineSequence().mapNotNull(::parseClassicBoard).toList()
    )

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
