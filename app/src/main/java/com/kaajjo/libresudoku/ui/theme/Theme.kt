package com.kaajjo.libresudoku.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun LibreSudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    appTheme: AppTheme = AppTheme.Green,
    content: @Composable () -> Unit,
) {
    val appColorScheme = AppColorScheme()
    val currentTheme = appColorScheme.getTheme(appTheme, darkTheme)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            when {
                darkTheme && amoled -> dynamicDarkColorScheme(context).copy(
                    background = Color.Black,
                    surface = Color.Black
                )

                darkTheme && !amoled -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
        }

        darkTheme && amoled -> currentTheme.copy(background = Color.Black, surface = Color.Black)
        darkTheme -> currentTheme
        else -> currentTheme
    }
    val systemUiController = rememberSystemUiController()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            SideEffect {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = !darkTheme
                )
            }

            content()
        }
    )
}