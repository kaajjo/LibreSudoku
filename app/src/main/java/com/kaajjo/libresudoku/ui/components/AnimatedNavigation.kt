package com.kaajjo.libresudoku.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.animatedComposable(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    deepLinks: List<NavDeepLink> = emptyList(),
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) = composable(
    route = route,
    arguments = arguments,
    deepLinks = deepLinks,
    enterTransition = {
        fadeIn(animationSpec = tween(230, delayMillis = 100)) +
                scaleIn(initialScale = 0.95f)
    },
    exitTransition = {
        fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.9f)
    },
    popEnterTransition = {
        fadeIn(animationSpec = tween(230, delayMillis = 100)) +
                scaleIn(initialScale = 0.95f)
    },
    popExitTransition = {
        fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.9f)
    },
    content = content
)