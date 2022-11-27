package com.kaajjo.libresudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.ui.game.GameScreen
import com.kaajjo.libresudoku.ui.gameshistory.GamesHistoryScreen
import com.kaajjo.libresudoku.ui.gameshistory.savedgame.SavedGameScreen
import com.kaajjo.libresudoku.ui.home.HomeScreen
import com.kaajjo.libresudoku.ui.learn.LearnScreen
import com.kaajjo.libresudoku.ui.more.MoreScreen
import com.kaajjo.libresudoku.ui.more.about.AboutScreen
import com.kaajjo.libresudoku.ui.onboarding.WelcomeScreen
import com.kaajjo.libresudoku.ui.settings.SettingsScreen
import com.kaajjo.libresudoku.ui.statistics.StatisticsScreen
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settings: AppSettingsManager
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mainViewModel: MainActivityViewModel = hiltViewModel()

            val dynamicColors = mainViewModel.dc.collectAsState(initial = isSystemInDarkTheme())
            val darkTheme = mainViewModel.darkTheme.collectAsState(initial = 0)
            val amoledBlack = mainViewModel.amoledBlack.collectAsState(initial = false)
            val firstLaunch by mainViewModel.firstLaunch.collectAsState(initial = false)
            val currentTheme = mainViewModel.currentTheme.collectAsState(initial = "green")
            LibreSudokuTheme(
                darkTheme = when(darkTheme.value) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = dynamicColors.value,
                amoled = amoledBlack.value,
                appTheme = when(currentTheme.value) {
                    "green" -> AppTheme.Green
                    "pink" -> AppTheme.Pink
                    "yellow" -> AppTheme.Yellow
                    "lavender" -> AppTheme.Lavender
                    "black_and_white" -> AppTheme.BlackAndWhite
                    else -> AppTheme.Green
                }
            ) {
                val systemUiController = rememberSystemUiController()
                systemUiController.setSystemBarsColor(MaterialTheme.colorScheme.surface)

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                var bottomBarState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry) {
                    bottomBarState = when(navBackStackEntry?.destination?.route) {
                        "statistics", "home", "more" -> true
                        else -> false
                    }
                }
                LaunchedEffect(firstLaunch) {
                    if(firstLaunch) {
                        navController.navigate("welcome_screen")
                    }
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            navController = navController,
                            bottomBarState = bottomBarState
                        )
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable("home") { HomeScreen(navController, hiltViewModel()) }
                        composable("more") { MoreScreen(navController) }
                        composable("about") { AboutScreen(navController)}
                        composable("welcome_screen") { WelcomeScreen(navController, hiltViewModel()) }
                        composable("statistics") { StatisticsScreen(navController, hiltViewModel()) }
                        composable("history") { GamesHistoryScreen(navController, hiltViewModel()) }
                        composable("learn") { LearnScreen(navController) }
                        composable(
                            route = "settings/?fromGame={fromGame}",
                            arguments = listOf(navArgument("fromGame") {
                                defaultValue = false
                                type = NavType.BoolType
                            })
                        ) {
                            SettingsScreen(navController, hiltViewModel())
                        }

                        composable(
                            route = "game/{uid}/{saved}",
                            arguments = listOf(
                                navArgument(name = "uid") { type = NavType.LongType},
                                navArgument(name = "saved") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) {
                            GameScreen(navController, hiltViewModel())
                        }

                        composable(
                            route = "saved_game/{uid}",
                            arguments = listOf(navArgument("uid") { type = NavType.LongType } )
                        ) {
                            SavedGameScreen(navController, hiltViewModel())
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationBar(
    navController: NavController,
    bottomBarState: Boolean
) {
    var selectedScreen by remember { mutableStateOf("home") }
    AnimatedContent(
        targetState = bottomBarState
    ) { visible ->
        if (visible) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            LaunchedEffect(key1 = currentDestination) {
                currentDestination?.let {
                    selectedScreen = it.route ?: ""
                }
            }

            NavigationBar {
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_info_24),
                            contentDescription = null
                        )
                    },
                    selected = selectedScreen == "statistics",
                    onClick = {
                        navController.navigate("statistics") {
                            launchSingleTop = true
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.nav_bar_statistics),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_home_24),
                            contentDescription = null
                        )
                    },
                    selected = selectedScreen == "home",
                    onClick = {
                        navController.navigate("home") {
                            launchSingleTop = true
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.nav_bar_home),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_more_horiz_24),
                            contentDescription = null
                        )
                    },
                    selected = selectedScreen == "more",
                    onClick = {
                        navController.navigate("more") {
                            launchSingleTop = true
                        }
                    },
                    label = {
                        Text(
                            text = stringResource(id = R.string.nav_bar_more),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }
    }
}

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    themeSettingsManager: ThemeSettingsManager,
    val appSettingsManager: AppSettingsManager
) : ViewModel()
{
    val dc = themeSettingsManager.dynamicColors
    val darkTheme = themeSettingsManager.darkTheme
    val amoledBlack = themeSettingsManager.amoledBlack
    val firstLaunch = appSettingsManager.firstLaunch
    val currentTheme = themeSettingsManager.currentTheme
}
