package com.kaajjo.libresudoku.fileimport

import com.kaajjo.libresudoku.core.parser.FileImportResult
import com.kaajjo.libresudoku.core.parser.OpenSudokuParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSudokuParserTest {
    private val parser = OpenSudokuParser()
    private val puzzle = "000605000003020800045090270500000001062000540400000007098060450006040700000203000"

    @Test
    fun importsRealSingleGameExport() {
        val expected = resource("opensudoku-v3-game.sdm").trim()
        assertBoards(listOf(expected), resource("opensudoku-v3-game.opensudoku"))
    }

    @Test
    fun supportsUnversionedAndExplicitV1V2AndV3Documents() {
        for (version in listOf(null, "1", "2", "3")) {
            assertBoards(listOf(puzzle), document("""<game data="$puzzle"/>""", version))
        }
    }

    @Test
    fun importsGamesFromMultipleFoldersAndDirectChildrenInOrder() {
        val second = puzzle.reversed()
        assertBoards(
            listOf(puzzle, second, puzzle),
            document(
                """<folder name="a"><game data="$puzzle"/></folder>
                    <folder name="b"><game data="$second"/></folder><game data="$puzzle"/>"""
            )
        )
    }

    @Test
    fun readsAllKnownCellVersionsFromBothAttributeNames() {
        val payloads = listOf(
            "version: 1&#10;8|-|0|7|1,3,|1|" + "0|-|1|".repeat(79),
            "version: 2&#10;8|0|0|7|257||1|" + "0|0|1|".repeat(79),
            "version: 3&#10;8|0|0|7|257|1|" + "0|0|1|".repeat(79),
            "version: 4&#10;8|0|0|0|7|257|16|1|" + "0|0|0|1|".repeat(79),
            "version: 5&#10;8|0|0|0|0|7|257|16|3|1|" + "0|0|0|0|1|".repeat(79)
        )
        for (payload in payloads) {
            for (attribute in listOf("data", "cells_data")) {
                assertBoards(listOf("8" + "0".repeat(80)), document("""<game $attribute="$payload"/>"""))
            }
        }
    }

    @Test
    fun finishedGameImportsOnlyOriginalClues() {
        val cells = puzzle.map { value ->
            if (value == '0') "9|0|0|0|1|" else "$value|0|0|0|0|"
        }.joinToString("")
        assertBoards(listOf(puzzle), document("""<game state="2" cells_data="version: 5&#10;$cells"/>"""))
    }

    @Test
    fun acceptsLiteralAndEscapedAttributeNewlines() {
        for (separator in listOf("&#10;", "\n", "\r\n", " ")) {
            assertBoards(
                listOf("8" + "0".repeat(80)),
                document("""<game cells_data="version: 5${separator}8|0|0|0|0|${"0|0|0|0|1|".repeat(80)}"/>""")
            )
        }
    }

    @Test
    fun prefersExplicitOriginalValuesOverPlayedCells() {
        assertBoards(
            listOf(puzzle),
            document("""<game original_values="$puzzle" cells_data="unsupported future data"/>""")
        )
    }

    @Test
    fun invalidOriginalValuesFallBackToCellData() {
        assertBoards(listOf(puzzle), document("""<game original_values="broken" data="$puzzle"/>"""))
    }

    @Test
    fun cellsDataTakesPrecedenceOverLegacyData() {
        assertBoards(
            listOf(puzzle),
            document("""<game cells_data="$puzzle" data="${puzzle.reversed()}"/>""")
        )
    }

    @Test
    fun skipsMissingAndInvalidGamesWithoutLosingValidOnes() {
        assertBoards(
            listOf(puzzle, puzzle),
            document("""<game/><game data="broken"/><game data="$puzzle"/>
                <game cells_data="version: 9&#10;broken"/><game data="$puzzle"/>""")
        )
    }

    @Test
    fun rejectsEmptyFilesAndFilesWithoutImportableGames() {
        for (content in listOf("", " ", "<opensudoku/>", document("<folder/>"), document("<game/>"))) {
            assertEquals(FileImportResult.Failure, parser.toBoards(content))
        }
    }

    @Test
    fun rejectsUnsupportedDocumentVersionAndWrongRoot() {
        for (content in listOf(
            document("""<game data="$puzzle"/>""", "4"),
            """<other><game data="$puzzle"/></other>""",
            """<other>${document("""<game data="$puzzle"/>""")}</other>"""
        )) {
            assertEquals(FileImportResult.Failure, parser.toBoards(content))
        }
    }

    @Test
    fun malformedXmlDoesNotReturnPartialSuccess() {
        for (content in listOf(
            """<opensudoku><game data="$puzzle"/><game""",
            document("""<game data="$puzzle"/>""") + "<broken",
            """<opensudoku><game data="$puzzle"/></folder>"""
        )) {
            assertEquals(FileImportResult.Failure, parser.toBoards(content))
        }
    }

    @Test
    fun rejectsTruncatedExtraAndInvalidSerializedCells() {
        val emptyCell = "0|0|0|0|1|"
        val invalidPayloads = listOf(
            emptyCell.repeat(80),
            emptyCell.repeat(82),
            "10|0|0|0|0|" + emptyCell.repeat(80),
            "8|0|0|0|2|" + emptyCell.repeat(80),
            "8||0|0|0|" + emptyCell.repeat(80),
            "8|0|0|0|0|" + emptyCell.repeat(80) + "garbage",
            emptyCell.repeat(81).dropLast(1)
        )
        for (payload in invalidPayloads) {
            assertEquals(
                payload,
                FileImportResult.Failure,
                parser.toBoards(document("""<game cells_data="version: 5&#10;$payload"/>"""))
            )
        }
    }

    @Test
    fun rejectsInvalidPlainBoardCharactersAndNormalizesDots() {
        assertBoards(listOf(puzzle), document("""<game data="${puzzle.replace('0', '.')}"/>"""))
        for (board in listOf("x".repeat(81), "١".repeat(81), puzzle.dropLast(1), puzzle + "0")) {
            assertEquals(FileImportResult.Failure, parser.toBoards(document("""<game data="$board"/>""")))
        }
    }

    @Test
    fun readsXmlDeclarationCommentsAndEscapedMetadata() {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
            <!-- export -->
            <opensudoku version="3"><folder name="A &amp; B">
            <game data="$puzzle" user_note="&lt;game&gt; &quot;note&quot;"/>
            </folder></opensudoku>"""
        assertBoards(listOf(puzzle), xml)
    }

    private fun assertBoards(expected: List<String>, xml: String) {
        val result = parser.toBoards(xml)
        assertTrue("Expected imported boards, got $result", result is FileImportResult.Success)
        assertEquals(expected, (result as FileImportResult.Success).boards)
    }

    private fun document(games: String, version: String? = "3"): String {
        val attribute = version?.let { """ version="$it"""" }.orEmpty()
        return "<opensudoku$attribute>$games</opensudoku>"
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.getResource("/file-import/$name")).readText()
}
