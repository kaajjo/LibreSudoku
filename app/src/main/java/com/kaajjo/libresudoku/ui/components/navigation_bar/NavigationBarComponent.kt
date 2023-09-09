package com.kaajjo.libresudoku.ui.components.navigation_bar

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import com.kaajjo.libresudoku.NavGraphs
import com.kaajjo.libresudoku.appCurrentDestinationAsState
import com.kaajjo.libresudoku.startAppDestination
import com.ramcosta.composedestinations.navigation.navigate

@Composable
fun NavigationBarComponent(
    navController: NavController,
    isVisible: Boolean
) {
    val directions = listOf(
        NavigationBarDestination.Statistics,
        NavigationBarDestination.Home,
        NavigationBarDestination.More
    )

    val currentDestination = navController.appCurrentDestinationAsState().value
        ?: NavGraphs.root.startAppDestination

    if (isVisible) {
        NavigationBar {
            directions.forEach { destination ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null
                        )
                    },
                    selected = currentDestination == destination.direction,
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        navController.navigate(destination.direction) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}