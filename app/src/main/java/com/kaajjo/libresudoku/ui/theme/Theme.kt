package com.kaajjo.libresudoku.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat

@Composable
fun LibreSudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    appThemes: AppThemes = AppThemes.Green,
    content: @Composable () -> Unit,
) {
    val appTheme = AppTheme()
    val currentTheme = appTheme.getTheme(appThemes, darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            // standard implementation: if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            // имплементация с амолед цветами, банально заменяем на черный
            when {
                darkTheme && amoled -> dynamicDarkColorScheme(context).copy(background = Color.Black, surface = Color.Black)
                darkTheme && !amoled -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }

        darkTheme && amoled -> currentTheme.copy(background = Color.Black, surface = Color.Black)
        darkTheme -> currentTheme
        else -> currentTheme
    }
    //val view = LocalView.current
    //if (!view.isInEditMode) {
    //    SideEffect {
    //        (view.context as Activity).window.statusBarColor = if(darkTheme) Color.Black.toArgb() else Color.White.toArgb()
    //        ViewCompat.getWindowInsetsController(view)?.isAppearanceLightStatusBars = !darkTheme
    //    }
    //}

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}