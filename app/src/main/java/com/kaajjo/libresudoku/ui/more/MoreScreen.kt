package com.kaajjo.libresudoku.ui.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.update.Release
import com.kaajjo.libresudoku.core.update.UpdateUtil
import com.kaajjo.libresudoku.destinations.AboutScreenDestination
import com.kaajjo.libresudoku.destinations.AutoUpdateScreenDestination
import com.kaajjo.libresudoku.destinations.BackupScreenDestination
import com.kaajjo.libresudoku.destinations.FoldersScreenDestination
import com.kaajjo.libresudoku.destinations.LearnScreenDestination
import com.kaajjo.libresudoku.destinations.SettingsCategoriesScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.settings.autoupdate.UpdateChannel
import com.kaajjo.libresudoku.ui.theme.RoundedPolygonShape
import com.materialkolor.ktx.blend
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Destination(style = AnimatedNavigation::class)
@Composable
fun MoreScreen(
    navigator: DestinationsNavigator,
    viewModel: MoreViewModel = hiltViewModel()
) {
    val autoUpdateChannel by viewModel.updateChannel.collectAsStateWithLifecycle(UpdateChannel.Disabled)
    val updateDismissedName by viewModel.updateDismissedName.collectAsStateWithLifecycle("")

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )

            PreferenceRow(
                title = stringResource(R.string.settings_title),
                painter = painterResource(R.drawable.ic_settings_24),
                onClick = { navigator.navigate(SettingsCategoriesScreenDestination()) }
            )
            PreferenceRow(
                title = stringResource(R.string.backup_restore_title),
                painter = rememberVectorPainter(image = Icons.Rounded.SettingsBackupRestore),
                onClick = { navigator.navigate(BackupScreenDestination()) }
            )
            PreferenceRow(
                title = stringResource(R.string.title_folders),
                painter = rememberVectorPainter(Icons.Outlined.Folder),
                onClick = { navigator.navigate(FoldersScreenDestination()) }
            )
            PreferenceRow(
                title = stringResource(R.string.learn_screen_title),
                painter = painterResource(R.drawable.ic_outline_help_outline_24),
                onClick = { navigator.navigate(LearnScreenDestination()) }
            )
            PreferenceRow(
                title = stringResource(R.string.about_title),
                painter = painterResource(R.drawable.ic_outline_info_24),
                onClick = { navigator.navigate(AboutScreenDestination()) }
            )

            AnimatedVisibility(autoUpdateChannel != UpdateChannel.Disabled) {
                var latestRelease by remember { mutableStateOf<Release?>(null) }
                LaunchedEffect(Unit) {
                    if (latestRelease == null) {
                        withContext(Dispatchers.IO) {
                            runCatching {
                                latestRelease =
                                    UpdateUtil.checkForUpdate(autoUpdateChannel == UpdateChannel.Beta)
                            }
                        }
                    }
                }
                latestRelease?.let { release ->
                    AnimatedVisibility(
                        visible = release.name.toString() != updateDismissedName,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        UpdateFoundBox(
                            versionToUpdate = release.name ?: "?",
                            onClick = {
                                navigator.navigate(AutoUpdateScreenDestination())
                            },
                            onDismissed = {
                                viewModel.dismissUpdate(release)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpdateFoundBox(
    versionToUpdate: String,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = with(MaterialTheme.colorScheme) {
        primaryContainer
    }
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .background(color = containerColor)
    ) {
        val infiniteTransition = rememberInfiniteTransition()
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = ""
        )
        val shape = remember {
            RoundedPolygonShape(
                RoundedPolygon.star(
                    numVerticesPerRadius = 10,
                    innerRadius = 0.8f,
                    rounding = CornerRounding(0.3f)
                )
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(50.dp)
                .offset((-10).dp, 10.dp)
                .rotate(rotation)
                .border(
                    width = 3.dp,
                    color = with(MaterialTheme.colorScheme) {
                        primary
                            .blend(onPrimaryContainer)
                            .copy(alpha = 0.5f)
                            .compositeOver(surface)
                    },
                    shape = shape
                )
        )
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = stringResource(R.string.update_found_version, versionToUpdate),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissed) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.new_update_found_action),
                    style = MaterialTheme.typography.titleSmall,
                    color = LocalContentColor.current
                        .copy(alpha = 0.75f)
                )
            }
        }
    }
}

