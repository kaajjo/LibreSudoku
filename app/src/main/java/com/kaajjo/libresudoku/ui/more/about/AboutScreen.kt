package com.kaajjo.libresudoku.ui.more.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.BuildConfig
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.CARD_MIR
import com.kaajjo.libresudoku.core.CRYPTO_BTC
import com.kaajjo.libresudoku.core.CRYPTO_TON
import com.kaajjo.libresudoku.core.CRYPTO_USDT_TRC20
import com.kaajjo.libresudoku.core.GITHUB_REPOSITORY
import com.kaajjo.libresudoku.core.TELEGRAM_CHANNEL
import com.kaajjo.libresudoku.core.WEBLATE_ENGAGE
import com.kaajjo.libresudoku.destinations.AboutLibrariesScreenDestination
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.theme.ColorUtils.harmonizeWithPrimary
import com.kaajjo.libresudoku.ui.theme.icons.Bitcoin
import com.kaajjo.libresudoku.ui.theme.icons.ExteraGram
import com.kaajjo.libresudoku.ui.theme.icons.LibreSudokuIconAlt
import com.kaajjo.libresudoku.ui.theme.icons.Mir
import com.kaajjo.libresudoku.ui.theme.icons.Ton
import com.kaajjo.libresudoku.ui.theme.icons.Usdt
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    navigator: DestinationsNavigator
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .align(Alignment.CenterHorizontally)
            ) {
                Icon(
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.Center)
                        .size(48.dp),
                    imageVector = Icons.Rounded.LibreSudokuIconAlt,
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .align(Alignment.CenterHorizontally)
            ) {
                Text(
                    stringResource(
                        R.string.app_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                )
            }

            FlowRow(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                AboutSectionBox(
                    title = stringResource(R.string.about_github_project),
                    subtitle = stringResource(R.string.about_github_source_code),
                    icon = ImageVector.vectorResource(R.drawable.ic_github_24dp),
                    onClick = { uriHandler.openUri(GITHUB_REPOSITORY) }
                )
                AboutSectionBox(
                    title = stringResource(R.string.weblate),
                    subtitle = stringResource(R.string.help_translate),
                    icon = ImageVector.vectorResource(R.drawable.ic_weblate),
                    onClick = { uriHandler.openUri(WEBLATE_ENGAGE) }
                )
                AboutSectionBox(
                    title = stringResource(R.string.telegram),
                    subtitle = stringResource(R.string.telegram_link),
                    icon = Icons.Rounded.ExteraGram,
                    onClick = { uriHandler.openUri(TELEGRAM_CHANNEL) }
                )
                AboutSectionBox(
                    title = stringResource(R.string.libraries_licenses),
                    subtitle = stringResource(R.string.libraries_licenses_title),
                    icon = Icons.Outlined.Info,
                    onClick = { navigator.navigate(AboutLibrariesScreenDestination()) }
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.donation_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(7.dp))
                    ) {
                        Text(
                            text = stringResource(R.string.donation_description),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DonationItem(
                            title = stringResource(R.string.crypto_bitcoin),
                            information = CRYPTO_BTC,
                            icon = Icons.Filled.Bitcoin,
                            onClick = { clipboardManager.setText(AnnotatedString(text = CRYPTO_BTC)) }
                        )
                        DonationItem(
                            title = stringResource(R.string.crypto_ton),
                            information = CRYPTO_TON,
                            icon = Icons.Rounded.Ton,
                            onClick = { clipboardManager.setText(AnnotatedString(text = CRYPTO_TON)) }
                        )
                        DonationItem(
                            title = stringResource(R.string.crypto_usdt),
                            information = CRYPTO_USDT_TRC20,
                            icon = Icons.Rounded.Usdt,
                            onClick = { clipboardManager.setText(AnnotatedString(text = CRYPTO_USDT_TRC20)) }
                        )
                        DonationItem(
                            title = stringResource(R.string.card_mir),
                            information = CARD_MIR,
                            icon = Icons.Filled.Mir,
                            onClick = { clipboardManager.setText(AnnotatedString(text = CARD_MIR.filter { it != ' ' })) }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowScope.AboutSectionBox(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    additionalContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxRowHeight()
            .weight(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.harmonizeWithPrimary(),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = subtitle
                )
            }
            if (additionalContent != null) {
                additionalContent()
            }
        }
    }
}

@Composable
fun DonationItem(
    title: String,
    information: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.harmonizeWithPrimary())
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = information,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null
            )
        }
    }
}