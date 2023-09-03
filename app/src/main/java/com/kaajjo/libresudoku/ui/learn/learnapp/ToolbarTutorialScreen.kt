package com.kaajjo.libresudoku.ui.learn.learnapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.ui.components.AnimatedNavigation
import com.kaajjo.libresudoku.ui.learn.components.TutorialBase
import com.kaajjo.libresudoku.ui.onboarding.FirstGameScreen
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(style = AnimatedNavigation::class)
@Composable
fun ToolbarTutorialScreen(navigator: DestinationsNavigator) {
    TutorialBase(
        title = stringResource(R.string.learn_app_toolbar),
        navigator = navigator
    ) {
        FirstGameScreen()
        Text(
            text = stringResource(R.string.learn_app_toolbar_notes_menu),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}