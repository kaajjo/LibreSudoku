package com.kaajjo.libresudoku.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.destinations.AboutScreenDestination
import com.kaajjo.libresudoku.destinations.FoldersScreenDestination
import com.kaajjo.libresudoku.destinations.LearnScreenDestination
import com.kaajjo.libresudoku.destinations.SettingsScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun MoreScreen(
    navigator: DestinationsNavigator
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
            onClick = { navigator.navigate(SettingsScreenDestination()) }
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
    }
}