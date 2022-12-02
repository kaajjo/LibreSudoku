package com.kaajjo.libresudoku.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.components.PreferenceRow

@Composable
fun MoreScreen(
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        PreferenceRow(
            title = stringResource(R.string.settings),
            painter = painterResource(R.drawable.ic_round_settings_24),
            onClick = {
                navController.navigate("settings/?fromGame=false")
            }
        )
        PreferenceRow(
            title = stringResource(R.string.app_about),
            painter = painterResource(R.drawable.ic_round_info_24),
            onClick = {
                navController.navigate("about")
            }
        )
        PreferenceRow(
            title = stringResource(R.string.learn_screen_title),
            painter = painterResource(R.drawable.ic_outline_help_outline_24),
            onClick = {
                navController.navigate("learn")
            }
        )
    }
}

