package com.kaajjo.libresudoku.core.parser

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException

/** .1gsudoku files exported by Sudoku 10'000 */
class GsudokuParser : FileImportParser {
    override fun toBoards(content: String): FileImportResult {
        return try {
            content.reader().use { reader ->
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(reader)
                if (parser.nextTag() != XmlPullParser.START_TAG || parser.name != "onegravitysudoku") {
                    return FileImportResult.Failure
                }

                val boards = mutableListOf<String>()
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "sudoku") {
                        parseClassicBoard(parser.getAttributeValue(null, "data"))?.let(boards::add)
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
}
