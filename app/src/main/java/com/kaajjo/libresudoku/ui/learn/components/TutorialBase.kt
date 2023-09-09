package com.kaajjo.libresudoku.ui.learn.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.kaajjo.libresudoku.R
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialBase(
    title: String,
    navigator: DestinationsNavigator,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = {
            navigator.popBackStack()
        }) {
            Icon(
                painter = painterResource(R.drawable.ic_round_arrow_back_24),
                contentDescription = null
            )
        }
    },
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = navigationIcon
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(innerPadding)
        ) {
            content()
        }
    }
}
