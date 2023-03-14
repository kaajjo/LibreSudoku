package com.kaajjo.libresudoku.core.utils

import kotlin.time.Duration


fun Duration.toFormattedString(): String {
    return this.toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }
}