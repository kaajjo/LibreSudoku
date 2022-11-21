package com.kaajjo.libresudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.ui.home.HomeScreen
import com.kaajjo.libresudoku.ui.theme.AppThemes
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
                appThemes = when(currentTheme.value) {
                    "green" -> AppThemes.Green
                    "pink" -> AppThemes.Pink
                    "yellow" -> AppThemes.Yellow
                    "lavender" -> AppThemes.Lavender
                    "black_and_white" -> AppThemes.BlackAndWhite
                    else -> AppThemes.Green
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
                        //navController.navigate("welcome_screen")
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
    val screens by remember {
        mutableStateOf(
            listOf(
                "statistics", "home", "more"
            )
        )
    }

    var selectedScreen by remember { mutableStateOf("home") }
    AnimatedContent(
        targetState = bottomBarState
    ) { visible ->
        if (visible) {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                NavigationBarItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_info_24),
                            contentDescription = null
                        )
                    },
                    selected = selectedScreen == "statistics",
                    onClick = {
                        selectedScreen = "statistics"
                        navController.navigate("statistics")
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
                        selectedScreen = "homessssssssssss"
                        navController.navigate("home")
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
                        selectedScreen = "more"
                        navController.navigate("more")
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
