package com.kaajjo.libresudoku.core.qqwing

import com.kaajjo.libresudoku.core.qqwing.QQWing.Companion.cellToColumn
import com.kaajjo.libresudoku.core.qqwing.QQWing.Companion.cellToRow

// @formatter:off
/*
 * qqwing - Sudoku solver and generator
 * Copyright (C) 2014 Stephen Ostermiller
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
// @formatter:on
/**
 * While solving the puzzle, log steps taken in a log item. This is useful for
 * later printing out the solve history or gathering statistics about how hard
 * the puzzle was to solve.
 */
class LogItem {
    /**
     * The recursion level at which this item was gathered. Used for backing out
     * log items solve branches that don't lead to a solution.
     */
    var round = 0
        private set
    /**
     * Get the type of this log item.
     */
    /**
     * The type of log message that will determine the message printed.
     */
    var type: LogType? = null
        private set

    /**
     * Value that was set by the operation (or zero for no value)
     */
    private var value = 0
    /**
     * Get the position (0-80) on the board or -1 if no position
     */
    /**
     * position on the board at which the value (if any) was set.
     */
    var position = 0
        private set

    constructor(r: Int, t: LogType) {
        init(r, t, 0, -1)
    }

    constructor(r: Int, t: LogType, v: Int, p: Int) {
        init(r, t, v, p)
    }

    private fun init(r: Int, t: LogType, v: Int, p: Int) {
        round = r
        type = t
        value = v
        position = p
    }

    fun print() {
        print(this)
    }

    /**
     * Get the row (1 indexed), or -1 if no row
     */
    val row: Int
        get() = if (position <= -1) -1 else cellToRow(position) + 1

    /**
     * Get the column (1 indexed), or -1 if no column
     */
    val column: Int
        get() = if (position <= -1) -1 else cellToColumn(position) + 1

    /**
     * Get the value, or -1 if no value
     */
    fun getValue(): Int {
        return if (value <= 0) -1 else value
    }

    /**
     * Print the current log item. The message used is determined by the type of
     * log item.
     */
    val description: String
        get() {
            val sb = StringBuilder()
            sb.append("Round: ").append(round)
            sb.append(" - ")
            sb.append(type!!.description)
            if (value > 0 || position > -1) {
                sb.append(" (")
                if (position > -1) {
                    sb.append("Row: ").append(row).append(" - Column: ").append(column)
                }
                if (value > 0) {
                    if (position > -1) sb.append(" - ")
                    sb.append("Value: ").append(getValue())
                }
                sb.append(")")
            }
            return sb.toString()
        }

    override fun toString(): String {
        return description
    }
}