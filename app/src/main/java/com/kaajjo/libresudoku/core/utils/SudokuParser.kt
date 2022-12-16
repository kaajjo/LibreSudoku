package com.kaajjo.libresudoku.core.utils

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.Note
import com.kaajjo.libresudoku.core.qqwing.GameType

class SudokuParser {
    private val emptySeparators = listOf('0', '.')

    fun parseBoard(
        board: String,
        gameType: GameType,
        locked: Boolean = false,
        emptySeparator: Char? = null
    ): MutableList<MutableList<Cell>> {
        if(board.isEmpty()) {
            throw BoardParseException(message = "Input string was empty")
        }

        val size = gameType.size
        val listBoard = MutableList(size) { row ->
            MutableList(size) { col ->
                Cell(row, col, 0)
            }
        }

        for(i in board.indices) {
            val value = if(emptySeparator != null) {
                if(board[i] == emptySeparator) 0 else board[i].digitToInt()
            } else {
                if(board[i] in emptySeparators) 0 else board[i].digitToInt()
            }

            listBoard[i / size][i % size].value = value
            listBoard[i / size][i % size].locked = locked
        }

        return listBoard
    }

    /**
     * Converts sudoku board to string
     * @param boardList Sudoku board
     * @return Sudoku in string
     */
    fun boardToString(boardList: List<List<Cell>>): String {
        var boardString = ""
        boardList.forEach { cells ->
            cells.forEach { cell ->
                boardString += cell.value.toString()
            }
        }
        return boardString
    }

    fun parseNotes(notesString: String): List<Note> {
        val notes = mutableListOf<Note>()
        var i = 0
        while(i < notesString.length) {
            println(i.toString())
            val index = notesString.indexOf(';', i)
            val toParse = notesString.substring(i..index)
            val row = toParse[0].digitToInt()
            val col = toParse[2].digitToInt()
            val value = toParse[4].digitToInt()
            notes.add(Note(row, col, value))
            i += index - i + 1
        }
        return notes
    }

    fun notesToString(notes: List<Note>): String {
        var notesString = ""
        // row,col,number;row,col,number....row,col,number;
        notes.forEach {
            notesString += "${it.row},${it.col},${it.value};"
        }
        return notesString
    }
}

class BoardParseException(message: String) : Exception(message)