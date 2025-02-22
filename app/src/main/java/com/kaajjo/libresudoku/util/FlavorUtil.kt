package com.kaajjo.libresudoku.util

import com.kaajjo.libresudoku.BuildConfig

object FlavorUtil {
    fun isFoss(): Boolean  = BuildConfig.FLAVOR == "foss"
}