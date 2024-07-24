package com.kaajjo.libresudoku.ui.app_crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.MainActivity
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.GITHUB_NEW_ISSUE
import com.kaajjo.libresudoku.core.GITHUB_REPOSITORY
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.TELEGRAM_CHANNEL
import com.kaajjo.libresudoku.core.utils.GlobalExceptionHandler.Companion.getExceptionString
import com.kaajjo.libresudoku.ui.components.ScrollbarLazyColumn
import com.kaajjo.libresudoku.ui.theme.LibreSudokuTheme
import com.kaajjo.libresudoku.ui.theme.icons.ExteraGram
import com.materialkolor.PaletteStyle
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashReason = getExceptionString()

        enableEdgeToEdge()
        setContent {
            val viewModel: CrashViewModel = hiltViewModel()
            val dynamicColors by viewModel.dc.collectAsStateWithLifecycle(isSystemInDarkTheme())
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle(PreferencesConstants.DEFAULT_DARK_THEME)
            val amoledBlack by viewModel.amoledBlack.collectAsStateWithLifecycle(
                PreferencesConstants.DEFAULT_AMOLED_BLACK
            )
            val colorSeed by viewModel.colorSeed.collectAsStateWithLifecycle(initialValue = Color.Red)
            val paletteStyle by viewModel.paletteStyle.collectAsStateWithLifecycle(initialValue = PaletteStyle.TonalSpot)

            LibreSudokuTheme(
                darkTheme = when (darkTheme) {
                    1 -> false
                    2 -> true
                    else -> isSystemInDarkTheme()
                },
                dynamicColor = dynamicColors,
                amoled = amoledBlack,
                colorSeed = colorSeed,
                paletteStyle = paletteStyle
            ) {
                val clipboardManager = LocalClipboardManager.current
                val uriHandler = LocalUriHandler.current

                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = stringResource(R.string.crash_screen_title),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .clip(MaterialTheme.shapes.large)
                                .clickable {
                                    clipboardManager.setText(
                                        AnnotatedString(
                                            text = crashReason
                                        )
                                    )
                                }
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp))
                                .weight(1f)
                        ) {
                            ScrollbarLazyColumn {
                                item {
                                    Text(
                                        text = crashReason,
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(onClick = {
                                clipboardManager.setText(
                                    AnnotatedString(
                                        text = crashReason
                                    )
                                )
                                uriHandler.openUri(GITHUB_NEW_ISSUE)
                            }) {
                                Icon(
                                    painterResource(R.drawable.ic_github_24dp),
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_share_crash_log))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = {
                                startActivity(
                                    Intent(
                                        this@CrashActivity,
                                        MainActivity::class.java,
                                    ).apply {
                                        flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    }
                                )
                            }) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_restart_app))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = {
                                    uriHandler.openUri(TELEGRAM_CHANNEL)
                                }) {
                                    Icon(
                                        imageVector = Icons.Rounded.ExteraGram,
                                        contentDescription = null
                                    )
                                }
                                IconButton(onClick = {
                                    uriHandler.openUri(GITHUB_REPOSITORY)
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_github_24dp),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}