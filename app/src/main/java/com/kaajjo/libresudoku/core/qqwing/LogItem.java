package com.kaajjo.libresudoku.core.qqwing;

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
public class LogItem {

    /**
     * The recursion level at which this item was gathered. Used for backing out
     * log items solve branches that don't lead to a solution.
     */
    private int round;

    /**
     * The type of log message that will determine the message printed.
     */
    private LogType type;

    /**
     * Value that was set by the operation (or zero for no value)
     */
    private int value;

    /**
     * position on the board at which the value (if any) was set.
     */
    private int position;

    public LogItem(int r, LogType t) {
        init(r, t, 0, -1);
    }

    public LogItem(int r, LogType t, int v, int p) {
        init(r, t, v, p);
    }

    private void init(int r, LogType t, int v, int p) {
        round = r;
        type = t;
        value = v;
        position = p;
    }

    public int getRound() {
        return round;
    }

    /**
     * Get the type of this log item.
     */
    public LogType getType() {
        return type;
    }

    public void print() {
        System.out.print(this);
    }

    /**
     * Get the row (1 indexed), or -1 if no row
     */
    public int getRow() {
        if (position <= -1) return -1;
        return QQWing.cellToRow(position) + 1;
    }

    /**
     * Get the column (1 indexed), or -1 if no column
     */
    public int getColumn() {
        if (position <= -1) return -1;
        return QQWing.cellToColumn(position) + 1;
    }

    /**
     * Get the position (0-80) on the board or -1 if no position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Get the value, or -1 if no value
     */
    public int getValue() {
        if (value <= 0) return -1;
        return value;
    }

    /**
     * Print the current log item. The message used is determined by the type of
     * log item.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("Round: ").append(getRound());
        sb.append(" - ");
        sb.append(getType().getDescription());
        if (value > 0 || position > -1) {
            sb.append(" (");
            if (position > -1) {
                sb.append("Row: ").append(getRow()).append(" - Column: ").append(getColumn());
            }
            if (value > 0) {
                if (position > -1) sb.append(" - ");
                sb.append("Value: ").append(getValue());
            }
            sb.append(")");
        }
        return sb.toString();
    }

    public String toString() {
        return getDescription();
    }
}
