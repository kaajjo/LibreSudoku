package com.kaajjo.libresudoku.core.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

/** Imports original puzzles from OpenSudoku XML exports (document versions 1–3). */
class OpenSudokuParser : FileImportParser {
    override fun toBoards(content: String): FileImportResult {
        return try {
            content.reader().use { reader ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(reader)
                if (parser.nextTag() != XmlPullParser.START_TAG || parser.name != "opensudoku") {
                    return FileImportResult.Failure
                }
                val version = parser.getAttributeValue(null, "version")
                if (version != null && version !in setOf("1", "2", "3")) {
                    return FileImportResult.Failure
                }

                val boards = mutableListOf<String>()
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "game") {
                        readBoard(parser)?.let(boards::add)
                    }
                }
                FileImportResult.fromBoards(boards)
            }
        } catch (_: XmlPullParserException) {
            FileImportResult.Failure
        } catch (_: IOException) {
            FileImportResult.Failure
        }
    }

    private fun readBoard(parser: XmlPullParser): String? {
        parseClassicBoard(parser.getAttributeValue(null, "original_values"))?.let { return it }
        val data = (parser.getAttributeValue(null, "cells_data")
            ?: parser.getAttributeValue(null, "data"))?.trim() ?: return null
        parseClassicBoard(data)?.let { return it }

        // XML normalizes literal newlines in attributes to spaces. Character references
        // (&#10;) preserve them, so accept either separator after the version header.
        val header = DATA_VERSION.find(data) ?: return null
        val cellPattern = CELL_PATTERNS[header.groupValues[1].toInt()] ?: return null
        var offset = header.range.last + 1
        val board = StringBuilder(81)
        repeat(81) {
            val cell = cellPattern.matchAt(data, offset) ?: return null
            val value = cell.groupValues[1]
            val editable = cell.groupValues[2] == "1"
            // User-entered values must not become locked clues in the imported puzzle.
            board.append(if (editable) "0" else value)
            offset = cell.range.last + 1
        }
        return board.toString().takeIf { offset == data.length }
    }

    private companion object {
        val DATA_VERSION = Regex("""^version:\s*([1-5])\s+""")

        // Cell.serialize / SudokuBoard.DATA_PATTERN_VERSION_* in OpenSudoku:
        // https://gitlab.com/opensudoku/opensudoku/-/tree/develop/shared/src/commonMain/kotlin/org/moire/opensudoku/game
        // v1–3: value, notes, editable; v4 adds secondary notes; v5 adds color.
        // v1 notes are comma-separated; v2 can have an extra separator after notes.
        val CELL_PATTERNS = mapOf(
            1 to Regex("""([0-9])\|(?:[0-9],|-)+\|([01])\|"""),
            2 to Regex("""([0-9])\|[0-9]{1,3}\|{1,2}([01])\|"""),
            3 to Regex("""([0-9])\|[0-9]{1,3}\|([01])\|"""),
            4 to Regex("""([0-9])\|[0-9]{1,3}\|[0-9]{1,3}\|([01])\|"""),
            5 to Regex("""([0-9])\|[0-9]{1,3}\|[0-9]{1,3}\|[0-9]\|([01])\|""")
        )
    }
}
