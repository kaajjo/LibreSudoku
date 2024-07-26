package com.kaajjo.libresudoku.ui.theme

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicMaterialThemeState

@Composable
fun LibreSudokuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    amoled: Boolean = false,
    colorSeed: Color = Color.Green,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit,
) {
    var materialThemeState = rememberDynamicMaterialThemeState(
        seedColor = colorSeed,
        isDark = darkTheme,
        style = paletteStyle,
        isAmoled = amoled
    )

    var colorScheme = materialThemeState.colorScheme
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current

        colorScheme = when {
            darkTheme && amoled -> dynamicDarkColorScheme(context).copy(
                background = Color.Black,
                surface = Color.Black
            )

            darkTheme && !amoled -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }
    }
    val systemUiController = rememberSystemUiController()

    materialThemeState = rememberDynamicMaterialThemeState(
        seedColor = colorSeed,
        isDark = darkTheme,
        style = paletteStyle,
        isAmoled = amoled,
        modifyColorScheme = { colorScheme }
    )

    DynamicMaterialTheme(
        state = materialThemeState,
        animate = true,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
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