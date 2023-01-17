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

import java.util.Locale;

public enum Symmetry {
    NONE,
    ROTATE90,
    ROTATE180,
    MIRROR,
    FLIP,
    RANDOM;

    public static Symmetry get(String s) {
        if (s == null) return null;
        try {
            s = s.toUpperCase(Locale.ENGLISH);
            return valueOf(s);
        } catch (IllegalArgumentException aix) {
            return null;
        }
    }

    public String getName() {
        String name = toString();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ENGLISH);
    }
}

