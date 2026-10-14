package com.kaajjo.libresudoku.fileimport

import com.kaajjo.libresudoku.core.parser.FileImportResult
import com.kaajjo.libresudoku.core.parser.GsudokuParser
import com.kaajjo.libresudoku.core.parser.SdmParser
import org.junit.Assert.assertEquals
import org.junit.Test

class FileImportParserTest {
    private val puzzle = "000605000003020800045090270500000001062000540400000007098060450006040700000203000"

    @Test
    fun sdmAcceptsDotsWhitespaceBomAndBothLineEndings() {
        val content = "\uFEFF  ${puzzle.replace('0', '.')}  \r\n\n$puzzle\n"
        assertEquals(FileImportResult.Success(listOf(puzzle, puzzle)), SdmParser().toBoards(content))
    }

    @Test
    fun sdmSkipsInvalidLinesButRejectsFilesWithoutBoards() {
        for (invalid in listOf("", " \r\n ", "x".repeat(81), "١".repeat(81), puzzle + "0")) {
            assertEquals(FileImportResult.Failure, SdmParser().toBoards(invalid))
            assertEquals(
                FileImportResult.Success(listOf(puzzle)),
                SdmParser().toBoards("$invalid\n$puzzle")
            )
        }
    }

    @Test
    fun gsudokuImportsMultipleBoardsAndSkipsInvalidRecords() {
        val xml = """<onegravitysudoku><sudokus>
            <sudoku data="$puzzle"/><sudoku/><sudoku data="bad"/>
            <sudoku data="${puzzle.replace('0', '.')}"/>
            </sudokus></onegravitysudoku>"""
        assertEquals(FileImportResult.Success(listOf(puzzle, puzzle)), GsudokuParser().toBoards(xml))
    }

    @Test
    fun gsudokuRejectsEmptyWrongAndMalformedDocuments() {
        for (xml in listOf(
            "", "<onegravitysudoku/>", """<other><sudoku data="$puzzle"/></other>""",
            """<onegravitysudoku><sudoku data="$puzzle"/><broken""",
            """<onegravitysudoku><sudoku data="${"١".repeat(81)}"/></onegravitysudoku>"""
        )) {
            assertEquals(FileImportResult.Failure, GsudokuParser().toBoards(xml))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyImportCannotBeReportedAsSuccessful() {
        FileImportResult.Success(emptyList())
    }
}
