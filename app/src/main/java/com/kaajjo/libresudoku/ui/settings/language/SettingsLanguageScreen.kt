package com.kaajjo.libresudoku.ui.settings.language

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.WEBLATE_ENGAGE
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.components.PreferenceRow
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.settings.SettingsScaffoldLazyColumn
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
    ) {  paddingValues ->
        ScrollbarLazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
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
            items(appLanguages.toList()) { language ->
                LanguageItem(
                    languageName = language.second,
                    selected = currentLanguage == language.first,
                    onClick = {
                        val locale = if (language.first == "") {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(language.first)
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
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp )
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PreferenceRow(
        modifier = modifier.background(
            if (selected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        title = languageName,
        onClick = onClick,
        action = {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    )
}