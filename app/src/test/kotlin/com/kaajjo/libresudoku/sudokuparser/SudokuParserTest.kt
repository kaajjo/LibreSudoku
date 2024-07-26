package com.kaajjo.libresudoku.sudokuparser

import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import org.junit.Test

class SudokuParserTest {
    @Test
    fun parse6x6_ReturnsTrue() {
        val board = listOf(
            listOf(
                Cell(0, 0, 0),
                Cell(0, 1, 2),
                Cell(0, 2, 3),
                Cell(0, 3, 0),
                Cell(0, 4, 0),
                Cell(0, 5, 6)
            ),
            listOf(
                Cell(1, 0, 0),
                Cell(1, 1, 0),
                Cell(1, 2, 5),
                Cell(1, 3, 0),
                Cell(1, 4, 0),
                Cell(1, 5, 0)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 5),
                Cell(2, 2, 0),
                Cell(2, 3, 0),
                Cell(2, 4, 1),
                Cell(2, 5, 0)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 6),
                Cell(3, 2, 0),
                Cell(3, 3, 0),
                Cell(3, 4, 2),
                Cell(3, 5, 0)
            ),
            listOf(
                Cell(4, 0, 0),
                Cell(4, 1, 0),
                Cell(4, 2, 0),
                Cell(4, 3, 5),
                Cell(4, 4, 0),
                Cell(4, 5, 0)
            ),
            listOf(
                Cell(5, 0, 5),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 1),
                Cell(5, 4, 6),
                Cell(5, 5, 0)
            ),
        )
        val boardString = "023006005000050010060020000500500160"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default6x6,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed == board)
    }

    @Test
    fun parse6x6_ReturnsFalse() {
        val board = listOf(
            listOf(
                Cell(0, 0, 0),
                Cell(0, 1, 2),
                Cell(0, 2, 3),
                Cell(0, 3, 0),
                Cell(0, 4, 0),
                Cell(0, 5, 6)
            ),
            listOf(
                Cell(1, 0, 0),
                Cell(1, 1, 0),
                Cell(1, 2, 1),
                Cell(1, 3, 2),
                Cell(1, 4, 0),
                Cell(1, 5, 0)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 3),
                Cell(2, 2, 3),
                Cell(2, 3, 0),
                Cell(2, 4, 1),
                Cell(2, 5, 0)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 6),
                Cell(3, 2, 0),
                Cell(3, 3, 0),
                Cell(3, 4, 2),
                Cell(3, 5, 0)
            ),
            listOf(
                Cell(4, 0, 0),
                Cell(4, 1, 0),
                Cell(4, 2, 0),
                Cell(4, 3, 5),
                Cell(4, 4, 0),
                Cell(4, 5, 0)
            ),
            listOf(
                Cell(5, 0, 5),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 1),
                Cell(5, 4, 6),
                Cell(5, 5, 0)
            ),
        )
        val boardString = "023006005000050010060020000500500160"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default6x6,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed != board)
    }


    @Test
    fun parse9x9_ReturnsTrue() {
        val board = listOf(
            listOf(
                Cell(0, 0, 0),
                Cell(0, 1, 0),
                Cell(0, 2, 0),
                Cell(0, 3, 6),
                Cell(0, 4, 0),
                Cell(0, 5, 0),
                Cell(0, 6, 0),
                Cell(0, 7, 0),
                Cell(0, 8, 0)
            ),
            listOf(
                Cell(1, 0, 8),
                Cell(1, 1, 2),
                Cell(1, 2, 4),
                Cell(1, 3, 7),
                Cell(1, 4, 5),
                Cell(1, 5, 3),
                Cell(1, 6, 1),
                Cell(1, 7, 6),
                Cell(1, 8, 9)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 0),
                Cell(2, 2, 0),
                Cell(2, 3, 2),
                Cell(2, 4, 0),
                Cell(2, 5, 0),
                Cell(2, 6, 0),
                Cell(2, 7, 0),
                Cell(2, 8, 0)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 0),
                Cell(3, 2, 0),
                Cell(3, 3, 5),
                Cell(3, 4, 0),
                Cell(3, 5, 0),
                Cell(3, 6, 4),
                Cell(3, 7, 7),
                Cell(3, 8, 1)
            ),
            listOf(
                Cell(4, 0, 0),
                Cell(4, 1, 0),
                Cell(4, 2, 0),
                Cell(4, 3, 1),
                Cell(4, 4, 0),
                Cell(4, 5, 0),
                Cell(4, 6, 3),
                Cell(4, 7, 8),
                Cell(4, 8, 6)
            ),
            listOf(
                Cell(5, 0, 0),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 4),
                Cell(5, 4, 0),
                Cell(5, 5, 0),
                Cell(5, 6, 9),
                Cell(5, 7, 2),
                Cell(5, 8, 5)
            ),
            listOf(
                Cell(6, 0, 0),
                Cell(6, 1, 0),
                Cell(6, 2, 0),
                Cell(6, 3, 3),
                Cell(6, 4, 0),
                Cell(6, 5, 0),
                Cell(6, 6, 0),
                Cell(6, 7, 0),
                Cell(6, 8, 0)
            ),
            listOf(
                Cell(7, 0, 0),
                Cell(7, 1, 0),
                Cell(7, 2, 0),
                Cell(7, 3, 9),
                Cell(7, 4, 0),
                Cell(7, 5, 0),
                Cell(7, 6, 0),
                Cell(7, 7, 0),
                Cell(7, 8, 0)
            ),
            listOf(
                Cell(8, 0, 0),
                Cell(8, 1, 0),
                Cell(8, 2, 0),
                Cell(8, 3, 8),
                Cell(8, 4, 0),
                Cell(8, 5, 0),
                Cell(8, 6, 0),
                Cell(8, 7, 0),
                Cell(8, 8, 0)
            ),
        )
        val boardString =
            "000600000824753169000200000000500471000100386000400925000300000000900000000800000"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default9x9,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed == board)
    }

    @Test
    fun parse9x9_ReturnsFalse() {
        val board = listOf(
            listOf(
                Cell(0, 0, 0),
                Cell(0, 1, 1),
                Cell(0, 2, 0),
                Cell(0, 3, 2),
                Cell(0, 4, 0),
                Cell(0, 5, 3),
                Cell(0, 6, 4),
                Cell(0, 7, 5),
                Cell(0, 8, 6)
            ),
            listOf(
                Cell(1, 0, 8),
                Cell(1, 1, 2),
                Cell(1, 2, 4),
                Cell(1, 3, 7),
                Cell(1, 4, 5),
                Cell(1, 5, 3),
                Cell(1, 6, 1),
                Cell(1, 7, 6),
                Cell(1, 8, 9)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 0),
                Cell(2, 2, 0),
                Cell(2, 3, 2),
                Cell(2, 4, 0),
                Cell(2, 5, 0),
                Cell(2, 6, 0),
                Cell(2, 7, 0),
                Cell(2, 8, 0)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 0),
                Cell(3, 2, 0),
                Cell(3, 3, 5),
                Cell(3, 4, 0),
                Cell(3, 5, 0),
                Cell(3, 6, 4),
                Cell(3, 7, 7),
                Cell(3, 8, 1)
            ),
            listOf(
                Cell(4, 0, 0),
                Cell(4, 1, 0),
                Cell(4, 2, 0),
                Cell(4, 3, 1),
                Cell(4, 4, 0),
                Cell(4, 5, 0),
                Cell(4, 6, 3),
                Cell(4, 7, 8),
                Cell(4, 8, 6)
            ),
            listOf(
                Cell(5, 0, 0),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 4),
                Cell(5, 4, 0),
                Cell(5, 5, 0),
                Cell(5, 6, 9),
                Cell(5, 7, 2),
                Cell(5, 8, 5)
            ),
            listOf(
                Cell(6, 0, 0),
                Cell(6, 1, 0),
                Cell(6, 2, 0),
                Cell(6, 3, 3),
                Cell(6, 4, 0),
                Cell(6, 5, 0),
                Cell(6, 6, 0),
                Cell(6, 7, 0),
                Cell(6, 8, 0)
            ),
            listOf(
                Cell(7, 0, 0),
                Cell(7, 1, 0),
                Cell(7, 2, 0),
                Cell(7, 3, 9),
                Cell(7, 4, 0),
                Cell(7, 5, 0),
                Cell(7, 6, 0),
                Cell(7, 7, 0),
                Cell(7, 8, 0)
            ),
            listOf(
                Cell(8, 0, 0),
                Cell(8, 1, 0),
                Cell(8, 2, 0),
                Cell(8, 3, 8),
                Cell(8, 4, 0),
                Cell(8, 5, 0),
                Cell(8, 6, 0),
                Cell(8, 7, 0),
                Cell(8, 8, 0)
            ),
        )
        val boardString =
            "000600000824753169000200000000500471000100386000400925000300000000900000000800000"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default9x9,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed != board)
    }

    @Test
    fun parse12x12_ReturnsTrue() {
        val board = listOf(
            listOf(
                Cell(0, 0, 12),
                Cell(0, 1, 0),
                Cell(0, 2, 0),
                Cell(0, 3, 11),
                Cell(0, 4, 6),
                Cell(0, 5, 5),
                Cell(0, 6, 0),
                Cell(0, 7, 4),
                Cell(0, 8, 7),
                Cell(0, 9, 1),
                Cell(0, 10, 0),
                Cell(0, 11, 0)
            ),
            listOf(
                Cell(1, 0, 8),
                Cell(1, 1, 0),
                Cell(1, 2, 0),
                Cell(1, 3, 0),
                Cell(1, 4, 0),
                Cell(1, 5, 1),
                Cell(1, 6, 0),
                Cell(1, 7, 0),
                Cell(1, 8, 0),
                Cell(1, 9, 0),
                Cell(1, 10, 0),
                Cell(1, 11, 2)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 0),
                Cell(2, 2, 5),
                Cell(2, 3, 0),
                Cell(2, 4, 2),
                Cell(2, 5, 3),
                Cell(2, 6, 0),
                Cell(2, 7, 0),
                Cell(2, 8, 0),
                Cell(2, 9, 8),
                Cell(2, 10, 9),
                Cell(2, 11, 11)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 0),
                Cell(3, 2, 7),
                Cell(3, 3, 0),
                Cell(3, 4, 0),
                Cell(3, 5, 0),
                Cell(3, 6, 0),
                Cell(3, 7, 0),
                Cell(3, 8, 8),
                Cell(3, 9, 0),
                Cell(3, 10, 11),
                Cell(3, 11, 0)
            ),
            listOf(
                Cell(4, 0, 4),
                Cell(4, 1, 0),
                Cell(4, 2, 6),
                Cell(4, 3, 0),
                Cell(4, 4, 0),
                Cell(4, 5, 0),
                Cell(4, 6, 9),
                Cell(4, 7, 8),
                Cell(4, 8, 1),
                Cell(4, 9, 0),
                Cell(4, 10, 5),
                Cell(4, 11, 0)
            ),
            listOf(
                Cell(5, 0, 0),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 0),
                Cell(5, 4, 0),
                Cell(5, 5, 0),
                Cell(5, 6, 0),
                Cell(5, 7, 0),
                Cell(5, 8, 0),
                Cell(5, 9, 9),
                Cell(5, 10, 0),
                Cell(5, 11, 0)
            ),
            listOf(
                Cell(6, 0, 0),
                Cell(6, 1, 0),
                Cell(6, 2, 11),
                Cell(6, 3, 0),
                Cell(6, 4, 0),
                Cell(6, 5, 0),
                Cell(6, 6, 0),
                Cell(6, 7, 0),
                Cell(6, 8, 4),
                Cell(6, 9, 7),
                Cell(6, 10, 8),
                Cell(6, 11, 3)
            ),
            listOf(
                Cell(7, 0, 0),
                Cell(7, 1, 0),
                Cell(7, 2, 0),
                Cell(7, 3, 0),
                Cell(7, 4, 0),
                Cell(7, 5, 0),
                Cell(7, 6, 0),
                Cell(7, 7, 0),
                Cell(7, 8, 11),
                Cell(7, 9, 12),
                Cell(7, 10, 0),
                Cell(7, 11, 0)
            ),
            listOf(
                Cell(8, 0, 0),
                Cell(8, 1, 0),
                Cell(8, 2, 0),
                Cell(8, 3, 0),
                Cell(8, 4, 0),
                Cell(8, 5, 0),
                Cell(8, 6, 0),
                Cell(8, 7, 5),
                Cell(8, 8, 0),
                Cell(8, 9, 0),
                Cell(8, 10, 6),
                Cell(8, 11, 0)
            ),
            listOf(
                Cell(9, 0, 0),
                Cell(9, 1, 8),
                Cell(9, 2, 0),
                Cell(9, 3, 0),
                Cell(9, 4, 3),
                Cell(9, 5, 9),
                Cell(9, 6, 1),
                Cell(9, 7, 0),
                Cell(9, 8, 6),
                Cell(9, 9, 2),
                Cell(9, 10, 0),
                Cell(9, 11, 0)
            ),
            listOf(
                Cell(10, 0, 0),
                Cell(10, 1, 0),
                Cell(10, 2, 2),
                Cell(10, 3, 7),
                Cell(10, 4, 5),
                Cell(10, 5, 0),
                Cell(10, 6, 0),
                Cell(10, 7, 6),
                Cell(10, 8, 3),
                Cell(10, 9, 0),
                Cell(10, 10, 0),
                Cell(10, 11, 0)
            ),
            listOf(
                Cell(11, 0, 9),
                Cell(11, 1, 6),
                Cell(11, 2, 1),
                Cell(11, 3, 0),
                Cell(11, 4, 0),
                Cell(11, 5, 4),
                Cell(11, 6, 0),
                Cell(11, 7, 11),
                Cell(11, 8, 0),
                Cell(11, 9, 0),
                Cell(11, 10, 0),
                Cell(11, 11, 0)
            )
        )
        val boardString = "C00B6504710080000100000200502300089B0070000080B040600098105000000000090000B00000478300000000BC000000000500600800391062000027500630009610040B0000"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default12x12,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed == board)
    }
    @Test
    fun parse12x12_ReturnsFalse() {
        val board = listOf(
            listOf(
                Cell(0, 0, 12),
                Cell(0, 1, 0),
                Cell(0, 2, 0),
                Cell(0, 3, 11),
                Cell(0, 4, 2),
                Cell(0, 5, 5),
                Cell(0, 6, 3),
                Cell(0, 7, 4),
                Cell(0, 8, 7),
                Cell(0, 9, 1),
                Cell(0, 10, 0),
                Cell(0, 11, 0)
            ),
            listOf(
                Cell(1, 0, 8),
                Cell(1, 1, 0),
                Cell(1, 2, 0),
                Cell(1, 3, 0),
                Cell(1, 4, 1),
                Cell(1, 5, 0),
                Cell(1, 6, 0),
                Cell(1, 7, 0),
                Cell(1, 8, 0),
                Cell(1, 9, 0),
                Cell(1, 10, 0),
                Cell(1, 11, 2)
            ),
            listOf(
                Cell(2, 0, 0),
                Cell(2, 1, 0),
                Cell(2, 2, 5),
                Cell(2, 3, 0),
                Cell(2, 4, 2),
                Cell(2, 5, 3),
                Cell(2, 6, 0),
                Cell(2, 7, 6),
                Cell(2, 8, 0),
                Cell(2, 9, 8),
                Cell(2, 10, 9),
                Cell(2, 11, 11)
            ),
            listOf(
                Cell(3, 0, 0),
                Cell(3, 1, 0),
                Cell(3, 2, 7),
                Cell(3, 3, 0),
                Cell(3, 4, 0),
                Cell(3, 5, 0),
                Cell(3, 6, 0),
                Cell(3, 7, 0),
                Cell(3, 8, 8),
                Cell(3, 9, 0),
                Cell(3, 10, 11),
                Cell(3, 11, 0)
            ),
            listOf(
                Cell(4, 0, 4),
                Cell(4, 1, 0),
                Cell(4, 2, 6),
                Cell(4, 3, 0),
                Cell(4, 4, 0),
                Cell(4, 5, 0),
                Cell(4, 6, 0),
                Cell(4, 7, 9),
                Cell(4, 8, 8),
                Cell(4, 9, 1),
                Cell(4, 10, 0),
                Cell(4, 11, 5)
            ),
            listOf(
                Cell(5, 0, 0),
                Cell(5, 1, 0),
                Cell(5, 2, 0),
                Cell(5, 3, 0),
                Cell(5, 4, 0),
                Cell(5, 5, 0),
                Cell(5, 6, 0),
                Cell(5, 7, 0),
                Cell(5, 8, 0),
                Cell(5, 9, 9),
                Cell(5, 10, 0),
                Cell(5, 11, 0)
            ),
            listOf(
                Cell(6, 0, 0),
                Cell(6, 1, 0),
                Cell(6, 2, 11),
                Cell(6, 3, 0),
                Cell(6, 4, 0),
                Cell(6, 5, 0),
                Cell(6, 6, 0),
                Cell(6, 7, 0),
                Cell(6, 8, 4),
                Cell(6, 9, 7),
                Cell(6, 10, 8),
                Cell(6, 11, 3)
            ),
            listOf(
                Cell(7, 0, 0),
                Cell(7, 1, 0),
                Cell(7, 2, 0),
                Cell(7, 3, 0),
                Cell(7, 4, 0),
                Cell(7, 5, 0),
                Cell(7, 6, 0),
                Cell(7, 7, 0),
                Cell(7, 8, 11),
                Cell(7, 9, 12),
                Cell(7, 10, 0),
                Cell(7, 11, 0)
            ),
            listOf(
                Cell(8, 0, 0),
                Cell(8, 1, 0),
                Cell(8, 2, 0),
                Cell(8, 3, 0),
                Cell(8, 4, 0),
                Cell(8, 5, 0),
                Cell(8, 6, 0),
                Cell(8, 7, 5),
                Cell(8, 8, 0),
                Cell(8, 9, 0),
                Cell(8, 10, 6),
                Cell(8, 11, 0)
            ),
            listOf(
                Cell(9, 0, 0),
                Cell(9, 1, 8),
                Cell(9, 2, 0),
                Cell(9, 3, 0),
                Cell(9, 4, 3),
                Cell(9, 5, 9),
                Cell(9, 6, 1),
                Cell(9, 7, 0),
                Cell(9, 8, 6),
                Cell(9, 9, 2),
                Cell(9, 10, 0),
                Cell(9, 11, 0)
            ),
            listOf(
                Cell(10, 0, 0),
                Cell(10, 1, 0),
                Cell(10, 2, 2),
                Cell(10, 3, 7),
                Cell(10, 4, 5),
                Cell(10, 5, 0),
                Cell(10, 6, 0),
                Cell(10, 7, 6),
                Cell(10, 8, 3),
                Cell(10, 9, 0),
                Cell(10, 10, 0),
                Cell(10, 11, 0)
            ),
            listOf(
                Cell(11, 0, 9),
                Cell(11, 1, 6),
                Cell(11, 2, 1),
                Cell(11, 3, 0),
                Cell(11, 4, 0),
                Cell(11, 5, 4),
                Cell(11, 6, 0),
                Cell(11, 7, 11),
                Cell(11, 8, 0),
                Cell(11, 9, 0),
                Cell(11, 10, 0),
                Cell(11, 11, 0)
            )
        )
        val boardString = "C00B6504710080000100000200502300089B0070000080B040600098105000000000090000B00000478300000000BC000000000500600800391062000027500630009610040B0000"
        val parsed = SudokuParser().parseBoard(
            board = boardString,
            gameType = GameType.Default12x12,
            locked = false,
            emptySeparator = '0'
        )
        assert(parsed != board)
    }

    /**
     * TODO:
     * Add test for:
     * Board to string
     * Notes parsing
     * Killer Cages parsing
     */
}