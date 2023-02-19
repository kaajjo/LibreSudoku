package com.kaajjo.libresudoku.core.parser

import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * .1gsudoku - file type from "Sudoku 10'000" app.
 */
class GsudokuParser : FileImportParser {
    private val tag = "GsudokuParser"

    /**
     * @param content .1gsudoku file content
     * @return Pair with: First - parsing success. Second - strings of parsed boards
     */
    override fun toBoards(content: String): Pair<Boolean, List<String>> {
        val parsedBoards = mutableListOf<String>()

        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()

        val input = content.reader()
        parser.setInput(input)

        var eventType = parser.eventType
        try {
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.name == "sudoku") {
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "data") {
                            val boardString = parser.getAttributeValue(i)
                            if (boardString.length == 81 && boardString.all { char -> char.isDigit() }) {
                                parsedBoards.add(boardString)
                            } else {
                                Log.i(tag, "Unexpected line: $boardString")
                                return Pair(false, parsedBoards)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception while parsing!")
            e.printStackTrace()
            return Pair(false, parsedBoards)
        }

        input.close()

        return Pair(true, parsedBoards)
    }
}