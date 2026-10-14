package com.kaajjo.libresudoku.core.parser

interface FileImportParser {
    /**
     *  Extracts 9x9 initial boards
     */
    fun toBoards(content: String): FileImportResult
}

sealed interface FileImportResult {
    data class Success(val boards: List<String>) : FileImportResult {
        init {
            require(boards.isNotEmpty())
        }
    }

    data object Failure : FileImportResult

    companion object {
        internal fun fromBoards(boards: List<String>): FileImportResult =
            if (boards.isEmpty()) Failure else Success(boards.toList())
    }
}

/**
 * Normalizes supported empty cells and rejects characters the board reader cannot handle
 */
fun parseClassicBoard(data: String?): String? {
    val board = data?.trim() ?: return null
    return board.takeIf { it.length == 81 && it.all { char -> char in '0'..'9' || char == '.' } }
        ?.replace('.', '0')
}
