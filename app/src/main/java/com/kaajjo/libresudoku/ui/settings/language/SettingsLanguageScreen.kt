package com.kaajjo.libresudoku.ui.settings.language

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.WEBLATE_ENGAGE
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.components.locale_emoji.LocaleEmoji
import com.kaajjo.libresudoku.ui.settings.SettingsScaffoldLazyColumn
import com.kaajjo.libresudoku.ui.theme.ColorUtils.harmonizeWithPrimary
import com.kaajjo.libresudoku.ui.util.findActivity
import com.kaajjo.libresudoku.ui.util.getCurrentLocaleTag
import com.kaajjo.libresudoku.ui.util.getLangs
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun SettingsLanguageScreen(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val appLanguages by remember { mutableStateOf(getLangs(context)) }
    var currentLanguage by remember { mutableStateOf(getCurrentLocaleTag()) }

    SettingsScaffoldLazyColumn(
        titleText = stringResource(R.string.pref_app_language),
        navigator = navigator
    ) { paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HelpTranslateCard(
                    onClick = {
                        uriHandler.openUri(WEBLATE_ENGAGE)
                    }
                )
            }
            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
            items(appLanguages.toList()) { (langCode, langName) ->
                LanguageItem(
                    languageName = langName,
                    languageEmoji = LocaleEmoji.getFlagEmoji(langCode),
                    selected = currentLanguage == langCode,
                    onClick = {
                        val locale = if (langCode == "") {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(langCode)
                        }
                        AppCompatDelegate.setApplicationLocales(locale)
                        currentLanguage = getCurrentLocaleTag()

                        // react to locale change only on android < 13
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            context.findActivity()?.recreate()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun HelpTranslateCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_weblate),
                contentDescription = null
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.help_translate),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp)
                )
                Text(
                    text = stringResource(R.string.hosted_weblate)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun LanguageItem(
    languageName: String,
    languageEmoji: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val titleStyle = MaterialTheme.typography.titleLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                color = with(MaterialTheme.colorScheme) {
                    if (selected) surfaceContainerLowest.harmonizeWithPrimary()
                    else Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (languageEmoji != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val localDensity = LocalDensity.current
                with(localDensity) {
                    Text(
                        text = languageEmoji,
                        fontSize = 24.dp.toSp(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        Column(
            Modifier
                .padding(start = if (languageEmoji != null) 6.dp else 12.dp)
                .weight(1f),
        ) {
            Text(
                text = languageName,
                style = titleStyle
            )
        }
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}