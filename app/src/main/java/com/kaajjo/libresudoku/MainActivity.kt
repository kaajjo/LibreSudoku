package com.kaajjo.libresudoku

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navOptions
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.ui.components.animatedComposable
import com.kaajjo.libresudoku.ui.customsudoku.CustomSudokuScreen
import com.kaajjo.libresudoku.ui.customsudoku.createsudoku.CreateSudokuScreen
import com.kaajjo.libresudoku.ui.folders.FoldersScreen
import com.kaajjo.libresudoku.ui.game.GameScreen
import com.kaajjo.libresudoku.ui.gameshistory.GamesHistoryScreen
import com.kaajjo.libresudoku.ui.gameshistory.savedgame.SavedGameScreen
import com.kaajjo.libresudoku.ui.home.HomeScreen
import com.kaajjo.libresudoku.ui.import_from_file.ImportFromFileScreen
import com.kaajjo.libresudoku.ui.learn.LearnScreen
import com.kaajjo.libresudoku.ui.more.MoreScreen
import com.kaajjo.libresudoku.ui.more.about.AboutLibrariesScreen
import com.kaajjo.libresudoku.ui.more.about.AboutScreen
import com.kaajjo.libresudoku.ui.onboarding.WelcomeScreen
import com.kaajjo.libresudoku.ui.settings.SettingsScreen
import com.kaajjo.libresudoku.ui.statistics.StatisticsScreen
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.util.Route
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var settings: AppSettingsManager

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val mainViewModel: MainActivityViewModel = hiltViewModel()

            val dynamicColors by mainViewModel.dc.collectAsState(isSystemInDarkTheme())
            val darkTheme by mainViewModel.darkTheme.collectAsState(PreferencesConstants.DEFAULT_DARK_THEME)
            val amoledBlack by mainViewModel.amoledBlack.collectAsState(PreferencesConstants.DEFAULT_AMOLED_BLACK)
            val firstLaunch by mainViewModel.firstLaunch.collectAsState(false)
            val currentTheme by mainViewModel.currentTheme.collectAsState(PreferencesConstants.DEFAULT_SELECTED_THEME)
            LibreSudokuTheme(
                darkTheme = when (darkTheme) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = dynamicColors,
                amoled = amoledBlack,
                appTheme = when (currentTheme) {
                    PreferencesConstants.GREEN_THEME_KEY -> AppTheme.Green
                    PreferencesConstants.PEACH_THEME_KEY -> AppTheme.Peach
                    PreferencesConstants.YELLOW_THEME_KEY -> AppTheme.Yellow
                    PreferencesConstants.LAVENDER_THEME_KEY -> AppTheme.Lavender
                    PreferencesConstants.BLACK_AND_WHITE_THEME_KEY -> AppTheme.BlackAndWhite
                    else -> AppTheme.Green
                }
            ) {
                val navController = rememberAnimatedNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()

                var bottomBarState by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(navBackStackEntry) {
                    bottomBarState = when (navBackStackEntry?.destination?.route) {
                        Route.STATISTICS, Route.HOME, Route.MORE -> true
                        else -> false
                    }
                }
                LaunchedEffect(firstLaunch) {
                    if (firstLaunch) {
                        navController.navigate(
                            route = Route.WELCOME_SCREEN,
                            navOptions = navOptions {
                                popUpTo(Route.HOME) {
                                    inclusive = true
                                }
                            }
                        )
                    }
                }
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            navController = navController,
                            bottomBarState = bottomBarState
                        )
                    },
                    contentWindowInsets = WindowInsets(0.dp)
                ) { paddingValues ->
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = Route.HOME,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        animatedComposable(Route.HOME) {
                            HomeScreen(
                                navigatePlayGame = {
                                    navController.navigate("game/${it.first}/${it.second}")
                                },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.MORE) {
                            MoreScreen(
                                navigateSettings = { navController.navigate("settings/?fromGame=false") },
                                navigateCustomSudoku = { navController.navigate(Route.CUSTOM_SUDOKU) },
                                navigateLearn = { navController.navigate(Route.LEARN) },
                                navigateAbout = { navController.navigate(Route.ABOUT) },
                                navigateImport = { navController.navigate(Route.FOLDERS) }
                            )
                        }

                        animatedComposable(Route.ABOUT) {
                            AboutScreen(
                                navigateBack = { navController.popBackStack() },
                                navigateOpenSourceLicenses = { navController.navigate(Route.OPEN_SOURCE_LICENSES) }
                            )
                        }

                        animatedComposable(Route.WELCOME_SCREEN) {
                            WelcomeScreen(
                                navigateToGame = {
                                    navController.popBackStack()
                                    navController.navigate(Route.HOME)
                                },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.STATISTICS) {
                            StatisticsScreen(
                                navigateHistory = { navController.navigate(Route.HISTORY) },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.HISTORY) {
                            GamesHistoryScreen(
                                navigateBack = { navController.popBackStack() },
                                navigateSavedGame = { uid ->
                                    navController.navigate(
                                        "saved_game/${uid}"
                                    )
                                },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.LEARN) {
                            LearnScreen { navController.popBackStack() }
                        }

                        animatedComposable(Route.OPEN_SOURCE_LICENSES) {
                            AboutLibrariesScreen { navController.popBackStack() }
                        }

                        animatedComposable(Route.CUSTOM_SUDOKU) {
                            CustomSudokuScreen(
                                navigateBack = { navController.popBackStack() },
                                navigateCreateSudoku = { navController.navigate(Route.CREATE_SUDOKU) },
                                navigatePlayGame = { uid -> navController.navigate("game/${uid}/true") },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.CREATE_SUDOKU) {
                            CreateSudokuScreen(
                                navigateBack = { navController.popBackStack() },
                                hiltViewModel()
                            )
                        }
                        animatedComposable(
                            route = Route.SETTINGS,
                            arguments = listOf(navArgument("fromGame") {
                                defaultValue = false
                                type = NavType.BoolType
                            })
                        ) {
                            SettingsScreen(
                                navigateBack = { navController.popBackStack() },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(
                            route = Route.GAME,
                            arguments = listOf(
                                navArgument(name = "uid") { type = NavType.LongType },
                                navArgument(name = "saved") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }
                            )
                        ) {
                            GameScreen(
                                navigateBack = { navController.popBackStack() },
                                navigateSettings = {
                                    navController.navigate("settings/?fromGame=true")
                                },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(
                            route = Route.SAVED_GAME,
                            arguments = listOf(navArgument("uid") { type = NavType.LongType })
                        ) {
                            SavedGameScreen(
                                navigateBack = { navController.popBackStack() },
                                navigatePlayGame = { uid ->
                                    navController.navigate(
                                        "game/${uid}/${true}"
                                    ) {
                                        popUpTo(Route.HISTORY)
                                    }
                                },
                                hiltViewModel()
                            )
                        }

                        animatedComposable(Route.FOLDERS) {
                            FoldersScreen(
                                viewModel = hiltViewModel(),
                                navigateBack = { navController.popBackStack() },
                                navigateExploreFolder = { },
                                navigateImportSudokuFile = { uri ->
                                    navController.navigate("import_sudoku_file?$uri?-1")
                                }
                            )
                        }

                        animatedComposable(
                            route = "import_sudoku_file?{uri}?{folder_uid}",
                            arguments = listOf(
                                navArgument("uri") { type = NavType.StringType },
                                navArgument("folder_uid") { type = NavType.LongType }
                            )
                        ) {
                            ImportFromFileScreen(
                                viewModel = hiltViewModel(),
                                navigateBack = { navController.navigateUp() }
                            )
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
    var selectedScreen by remember { mutableStateOf(Route.HOME) }
    val navBarScreens = listOf(
        Pair(Route.STATISTICS, R.string.nav_bar_statistics),
        Pair(Route.HOME, R.string.nav_bar_home),
        Pair(Route.MORE, R.string.nav_bar_more),
    )
    val navBarIcons = listOf(
        painterResource(R.drawable.ic_round_info_24),
        painterResource(R.drawable.ic_round_home_24),
        painterResource(R.drawable.ic_round_more_horiz_24)
    )
    AnimatedContent(
        targetState = bottomBarState
    ) { visible ->
        if (visible) {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            LaunchedEffect(currentDestination) {
                currentDestination?.let {
                    selectedScreen = it.route ?: ""
                }
            }

            NavigationBar {
                navBarScreens.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = navBarIcons[index],
                                contentDescription = null
                            )
                        },
                        selected = selectedScreen == item.first,
                        label = {
                            Text(
                                text = stringResource(item.second),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        onClick = {
                            navController.navigate(item.first) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@HiltViewModel
class MainActivityViewModel
@Inject constructor(
    themeSettingsManager: ThemeSettingsManager,
    appSettingsManager: AppSettingsManager
) : ViewModel() {
    val dc = themeSettingsManager.dynamicColors
    val darkTheme = themeSettingsManager.darkTheme
    val amoledBlack = themeSettingsManager.amoledBlack
    val firstLaunch = appSettingsManager.firstLaunch
    val currentTheme = themeSettingsManager.currentTheme
}
