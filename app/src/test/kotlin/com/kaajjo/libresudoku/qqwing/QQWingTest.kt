package com.kaajjo.libresudoku.qqwing

import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.qqwing.QQWing
import com.kaajjo.libresudoku.core.utils.SudokuParser
import org.junit.Test

class QQWingTest {

    @Test fun solve6x6_ReturnsTrue() {
        val board = SudokuParser().parseBoard(
            board = "500600000020053001100350040000001005",
            gameType = GameType.Default6x6,
            emptySeparator = '0'
        )

        val solvedBoard = SudokuParser().parseBoard(
            board = "532614416523653241124356345162261435",
            gameType = GameType.Default6x6,
            emptySeparator = '0'
        )

        val qqwing = QQWing(GameType.Default6x6, GameDifficulty.Unspecified)
        qqwing.setPuzzle(board.flatten().map { it.value }.toIntArray())
        qqwing.solve()

        assert(solvedBoard.flatten().map { it.value }.toIntArray().contentEquals(qqwing.solution))
    }

    @Test fun solve6x6_NoSolution_ReturnsFalse() {
        val board = SudokuParser().parseBoard(
            board = "106020205001010602623100001250562010",
            gameType = GameType.Default6x6,
            emptySeparator = '0'
        )
        val qqwing = QQWing(GameType.Default6x6, GameDifficulty.Unspecified)
        qqwing.setPuzzle(board.flatten().map { it.value }.toIntArray())
        qqwing.solve()

        assert(!qqwing.isSolved())
    }


    @Test fun solve6x6_UniqueSolution_ReturnsTrue() {
        val board = SudokuParser().parseBoard(
            board = "500600000020053001100350040000001005",
            gameType = GameType.Default6x6,
            emptySeparator = '0'
        )
        val qqwing = QQWing(GameType.Default6x6, GameDifficulty.Unspecified)
        qqwing.setPuzzle(board.flatten().map { it.value }.toIntArray())
        qqwing.solve()
        assert(qqwing.hasUniqueSolution())
    }

    @Test fun solve6x6_UniqueSolution_ReturnsFalse() {
        val board = SudokuParser().parseBoard(
            board = "000000000020053001100350040000001005",
            gameType = GameType.Default6x6,
            emptySeparator = '0'
        )
        val qqwing = QQWing(GameType.Default6x6, GameDifficulty.Unspecified)
        qqwing.setPuzzle(board.flatten().map { it.value }.toIntArray())
        qqwing.solve()

        assert(!qqwing.hasUniqueSolution())
    }

    /**
     * TODO:
     * Add test for:
     * 9x9 and 12x12
     * Generating
     * Solutions count
     * .......
     */
}