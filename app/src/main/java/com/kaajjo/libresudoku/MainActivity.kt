package com.kaajjo.libresudoku

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.navOptions
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.data.datastore.ThemeSettingsManager
import com.kaajjo.libresudoku.ui.components.animatedComposable
import com.kaajjo.libresudoku.ui.create_edit_sudoku.CreateSudokuScreen
import com.kaajjo.libresudoku.ui.explore_folder.ExploreFolderScreen
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
import com.kaajjo.libresudoku.ui.settings.boardtheme.SettingsBoardTheme
import com.kaajjo.libresudoku.ui.statistics.StatisticsScreen
import com.kaajjo.libresudoku.ui.theme.AppTheme
import com.kaajjo.libresudoku.ui.theme.BoardColors
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.SudokuBoardColorsImpl
import com.kaajjo.libresudoku.ui.util.Route
import com.kaajjo.libresudoku.ui.util.findActivity
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

val LocalBoardColors = staticCompositionLocalOf { SudokuBoardColorsImpl() }

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
                    PreferencesConstants.BLUE_THEME_KEY -> AppTheme.Blue
                    PreferencesConstants.PEACH_THEME_KEY -> AppTheme.Peach
                    PreferencesConstants.YELLOW_THEME_KEY -> AppTheme.Yellow
                    PreferencesConstants.LAVENDER_THEME_KEY -> AppTheme.Lavender
                    PreferencesConstants.BLACK_AND_WHITE_THEME_KEY -> AppTheme.BlackAndWhite
                    else -> AppTheme.Green
                }
            ) {
                val context = LocalContext.current
                val navController = rememberNavController()
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

                val monetSudokuBoard by mainViewModel.monetSudokuBoard.collectAsStateWithLifecycle(
                    initialValue = PreferencesConstants.DEFAULT_MONET_SUDOKU_BOARD
                )

                val boardColors =
                    if (monetSudokuBoard) {
                        SudokuBoardColorsImpl(
                            foregroundColor = BoardColors.foregroundColor,
                            notesColor = BoardColors.notesColor,
                            altForegroundColor = BoardColors.altForegroundColor,
                            errorColor = BoardColors.errorColor,
                            highlightColor = BoardColors.highlightColor,
                            thickLineColor = BoardColors.thickLineColor,
                            thinLineColor = BoardColors.thinLineColor
                        )
                    } else {
                        SudokuBoardColorsImpl(
                            foregroundColor = MaterialTheme.colorScheme.onSurface,
                            notesColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            altForegroundColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            errorColor = BoardColors.errorColor,
                            highlightColor = MaterialTheme.colorScheme.outline,
                            thickLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f),
                            thinLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.25f)
                        )
                    }

                CompositionLocalProvider(LocalBoardColors provides boardColors) {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                navController = navController,
                                bottomBarState = bottomBarState
                            )
                        },
                        contentWindowInsets = WindowInsets(0.dp)
                    ) { paddingValues ->
                        NavHost(
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
                                    navigateSavedGame = { navController.navigate("saved_game/${it}") },
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

                            animatedComposable(
                                route = "create_edit_sudoku/{game_uid}/{folder_uid}",
                                arguments = listOf(
                                    navArgument("game_uid") {
                                        type = NavType.LongType
                                    }, // used for editing
                                    navArgument("folder_uid") {
                                        type = NavType.LongType
                                    } // folder where to save
                                )
                            ) {
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
                                    hiltViewModel(),
                                    navigateBoardSettings = { navController.navigate("settings_board_theme") }
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
                                    navigateToFolder = { uid ->
                                        navController.navigate("explore_folder/$uid") {
                                            popUpTo("history")
                                        }
                                    },
                                    hiltViewModel()
                                )
                            }

                            animatedComposable(Route.FOLDERS) {
                                FoldersScreen(
                                    viewModel = hiltViewModel(),
                                    navigateBack = { navController.popBackStack() },
                                    navigateExploreFolder = { uid ->
                                        navController.navigate("explore_folder/$uid")
                                    },
                                    navigateImportSudokuFile = { uri ->
                                        navController.navigate("import_sudoku_file?$uri?-1")
                                    },
                                    navigateViewSavedGame = { uid ->
                                        navController.navigate("saved_game/${uid}")
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
                                    navigateBack = {
                                        val activity = context.findActivity()
                                        activity?.intent?.data = null

                                        navController.navigateUp()
                                    }
                                )
                            }

                            animatedComposable(
                                route = "explore_folder/{uid}",
                                arguments = listOf(navArgument("uid") { type = NavType.LongType })
                            ) {
                                ExploreFolderScreen(
                                    viewModel = hiltViewModel(),
                                    navigateBack = { navController.popBackStack() },
                                    navigatePlayGame = { args ->
                                        navController.navigate(
                                            "game/${args.first}/${args.second}"
                                        ) {
                                            popUpTo("explore_folder/${args.third}")
                                        }
                                    },
                                    navigateImportFromFile = { args ->
                                        // First - uri. Second = folder uid
                                        navController.navigate("import_sudoku_file?${args.first}?${args.second}")
                                    },
                                    navigateEditGame = { args ->
                                        navController.navigate("create_edit_sudoku/${args.first}/${args.second}")
                                    },
                                    navigateCreateSudoku = { folderUid ->
                                        navController.navigate("create_edit_sudoku/-1/$folderUid")
                                    }
                                )
                            }

                            animatedComposable(
                                route = "import_sudoku_file_deeplink",
                                deepLinks = listOf(
                                    navDeepLink {
                                        uriPattern = "content://"
                                        mimeType = "*/*"
                                        action = Intent.ACTION_VIEW
                                    }
                                )
                            ) {
                                val activity = context.findActivity()
                                if (activity != null) {
                                    val intentData = activity.intent.data
                                    if (intentData != null) {
                                        navController.navigate("import_sudoku_file?${Uri.encode(intentData.toString())}?-1")
                                    }
                                    LaunchedEffect(intentData) {
                                        if (activity.intent.data == null) {
                                            activity.finish()
                                        }
                                    }
                                }
                            }

                            animatedComposable(Route.SETTINGS_BOARD_THEME) {
                                SettingsBoardTheme(
                                    viewModel = hiltViewModel(),
                                    navigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    val monetSudokuBoard = themeSettingsManager.monetSudokuBoard
}