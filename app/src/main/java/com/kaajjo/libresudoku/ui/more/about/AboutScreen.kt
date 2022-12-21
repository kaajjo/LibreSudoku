package com.kaajjo.libresudoku.ui.more.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.kaajjo.libresudoku.BuildConfig
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.components.PreferenceRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigateBack: () -> Unit,
    navigateOpenSourceLicenses: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_about)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxWidth()
        ) {
            PreferenceRow(
                title = stringResource(R.string.about_version),
                subtitle = BuildConfig.VERSION_NAME,
                painter = painterResource(R.drawable.ic_outline_info_24),
            )
            val uriHandler = LocalUriHandler.current
            PreferenceRow(
                title = stringResource(R.string.about_github_project),
                painter = painterResource(R.drawable.ic_github_24dp),
                onClick = {
                    uriHandler.openUri("https://github.com/kaajjo/Libre-Sudoku")
                }
            )
            PreferenceRow(
                title = stringResource(R.string.libraries_licenses_title),
                painter = painterResource(R.drawable.ic_outline_info_24),
                onClick = navigateOpenSourceLicenses
            )
        }
    }
}