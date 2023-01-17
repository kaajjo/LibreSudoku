package com.kaajjo.libresudoku.core.qqwing

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
enum class LogType(val description: String) {
    GIVEN("Mark given"),
    SINGLE("Mark only possibility for cell"),
    HIDDEN_SINGLE_ROW("Mark single possibility for value in row"),
    HIDDEN_SINGLE_COLUMN("Mark single possibility for value in column"),
    HIDDEN_SINGLE_SECTION("Mark single possibility for value in section"),
    GUESS("Mark guess (start round)"),
    ROLLBACK("Roll back round"),
    NAKED_PAIR_ROW("Remove possibilities for naked pair in row"),
    NAKED_PAIR_COLUMN("Remove possibilities for naked pair in column"),
    NAKED_PAIR_SECTION("Remove possibilities for naked pair in section"),
    POINTING_PAIR_TRIPLE_ROW("Remove possibilities for row because all values are in one section"),
    POINTING_PAIR_TRIPLE_COLUMN("Remove possibilities for column because all values are in one section"),
    ROW_BOX("Remove possibilities for section because all values are in one row"),
    COLUMN_BOX("Remove possibilities for section because all values are in one column"),
    HIDDEN_PAIR_ROW("Remove possibilities from hidden pair in row"),
    HIDDEN_PAIR_COLUMN("Remove possibilities from hidden pair in column"),
    HIDDEN_PAIR_SECTION("Remove possibilities from hidden pair in section")
}