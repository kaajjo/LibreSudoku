package com.kaajjo.libresudoku.ui.components.navigation_bar

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import com.kaajjo.libresudoku.destinations.MoreScreenDestination
import com.kaajjo.libresudoku.startAppDestination
import com.ramcosta.composedestinations.utils.toDestinationsNavigator

@Composable
fun NavigationBarComponent(
    navController: NavController,
    isVisible: Boolean,
    updateAvailable: Boolean = false,
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
                        if (destination.direction.route == MoreScreenDestination.route
                            && updateAvailable
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge()
                                }
                            ) {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null
                                )
                            }
                        } else {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null
                            )
                        }
                    },
                    selected = currentDestination == destination.direction,
                    label = {
                        Text(
                            text = stringResource(destination.label),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        navController.toDestinationsNavigator().navigate(destination.direction) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}