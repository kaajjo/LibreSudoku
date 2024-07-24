package com.kaajjo.libresudoku.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.SudokuParser
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.destinations.BackupScreenDestination
import com.kaajjo.libresudoku.destinations.HomeScreenDestination
import com.kaajjo.libresudoku.destinations.SettingsCategoriesScreenDestination
import com.kaajjo.libresudoku.destinations.SettingsLanguageScreenDestination
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.util.getCurrentLocaleString
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@Destination
@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = hiltViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val currentLanguage by remember {
        mutableStateOf(
            getCurrentLocaleString(context)
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .systemBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.intro_rules))
                    Board(
                        board = viewModel.previewBoard,
                        size = 9,
                        selectedCell = viewModel.selectedCell,
                        onClick = { cell -> viewModel.selectedCell = cell }
                    )

                    Button(
                        onClick = {
                            viewModel.setFirstLaunch()
                            navigator.popBackStack()
                            navigator.navigate(HomeScreenDestination())
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(stringResource(R.string.action_start))
                    }

                    ItemRowBigIcon(
                        title = stringResource(R.string.pref_app_language),
                        icon = Icons.Rounded.Language,
                        subtitle = currentLanguage,
                        onClick = { navigator.navigate(SettingsLanguageScreenDestination()) },
                    )
                    ItemRowBigIcon(
                        title = stringResource(R.string.onboard_restore_backup),
                        icon = Icons.Rounded.Restore,
                        subtitle = stringResource(R.string.onboard_restore_backup_description),
                        onClick = {
                            navigator.navigate(BackupScreenDestination)
                        }
                    )
                    ItemRowBigIcon(
                        title = stringResource(R.string.settings_title),
                        icon = Icons.Rounded.Settings,
                        subtitle = stringResource(R.string.onboard_settings_description),
                        onClick = {
                            navigator.navigate(SettingsCategoriesScreenDestination(false))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRowBigIcon(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = { },
    onClick: () -> Unit = { },
    subtitle: String? = null,
    shape: Shape = MaterialTheme.shapes.large,
    onLongClick: ((() -> Unit))? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    subtitleStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
    containerColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
    iconBackground: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconSize: Dp = 42.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.background(
                        color = iconBackground,
                        shape = MaterialTheme.shapes.medium
                    )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                            .padding(6.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = titleStyle
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = subtitleStyle,
                            color = LocalContentColor.current.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            trailing()
        }
    }
}

@HiltViewModel
class WelcomeViewModel
@Inject constructor(
    private val settingsDataManager: AppSettingsManager
) : ViewModel() {
    var selectedCell by mutableStateOf(Cell(-1, -1, 0))

    // all heart shaped ❤
    val previewBoard = SudokuParser().parseBoard(
        board = listOf(
            "072000350340502018100030009800000003030000070050000020008000600000103000760050041",
            "017000230920608054400010009200000001060000020040000090002000800000503000390020047",
            "052000180480906023600020007500000008020000060030000090005000300000708000370060014",
            "025000860360208017700010003600000002040000090030000070006000100000507000490030058",
            "049000380280309056600050007300000002010000030070000090003000800000604000420080013",
            "071000420490802073300060009200000007060000090010000080007000900000703000130090068",
            "023000190150402086800050004700000008090000030080000010008000700000306000530070029",
            "097000280280706013300080007600000002040000060030000090001000400000105000860040051",
            "049000180160904023700010004200000008090000060080000050005000600000706000470020031"
        ).random(),
        gameType = GameType.Default9x9
    )

    fun setFirstLaunch(value: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setFirstLaunch(value)
        }
    }
}
