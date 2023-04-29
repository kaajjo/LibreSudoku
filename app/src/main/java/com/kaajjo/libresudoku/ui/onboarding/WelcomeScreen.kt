package com.kaajjo.libresudoku.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.data.datastore.AppSettingsManager
import com.kaajjo.libresudoku.ui.components.board.Board
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@Composable
fun WelcomeScreen(
    navigateToGame: () -> Unit,
    viewModel: WelcomeViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.onboard_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            FirstPage(
                selectedCellChanged = { viewModel.selectedCell = it },
                selectedCell = viewModel.selectedCell,
                board = viewModel.previewBoard,
                onFinishedClick = {
                    viewModel.setFirstLaunch()
                    navigateToGame()
                }
            )
        }
    }
}

@Composable
fun FirstPage(
    selectedCellChanged: (Cell) -> Unit,
    selectedCell: Cell,
    board: List<List<Cell>>,
    onFinishedClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.intro_what_is_sudoku))
            Text(stringResource(R.string.intro_rules))
            Board(
                board = board,
                size = 9,
                selectedCell = selectedCell,
                onClick = { cell -> selectedCellChanged(cell) },
                boardColors = LocalBoardColors.current
            )
            Text(stringResource(R.string.onboard_recommendation_prefs))
            FilledTonalButton(
                onClick = onFinishedClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.action_start))
            }
        }
    }
}

@HiltViewModel
class WelcomeViewModel
@Inject constructor(
    private val settingsDataManager: AppSettingsManager
) : ViewModel() {
    var selectedCell by mutableStateOf(Cell(-1, -1, 0))

    val previewBoard = listOf(
        listOf(
            Cell(0, 0, 0),
            Cell(0, 1, 0),
            Cell(0, 2, 1),
            Cell(0, 3, 0),
            Cell(0, 4, 0),
            Cell(0, 5, 0),
            Cell(0, 6, 9),
            Cell(0, 7, 0),
            Cell(0, 8, 0)
        ),
        listOf(
            Cell(1, 0, 0),
            Cell(1, 1, 2),
            Cell(1, 2, 0),
            Cell(1, 3, 0),
            Cell(1, 4, 1),
            Cell(1, 5, 7),
            Cell(1, 6, 0),
            Cell(1, 7, 5),
            Cell(1, 8, 4)
        ),
        listOf(
            Cell(2, 0, 5),
            Cell(2, 1, 0),
            Cell(2, 2, 0),
            Cell(2, 3, 0),
            Cell(2, 4, 2),
            Cell(2, 5, 4),
            Cell(2, 6, 0),
            Cell(2, 7, 0),
            Cell(2, 8, 3)
        ),
        listOf(
            Cell(3, 0, 2),
            Cell(3, 1, 8),
            Cell(3, 2, 0),
            Cell(3, 3, 0),
            Cell(3, 4, 0),
            Cell(3, 5, 0),
            Cell(3, 6, 0),
            Cell(3, 7, 9),
            Cell(3, 8, 0)
        ),
        listOf(
            Cell(4, 0, 0),
            Cell(4, 1, 0),
            Cell(4, 2, 5),
            Cell(4, 3, 2),
            Cell(4, 4, 0),
            Cell(4, 5, 0),
            Cell(4, 6, 0),
            Cell(4, 7, 4),
            Cell(4, 8, 7)
        ),
        listOf(
            Cell(5, 0, 0),
            Cell(5, 1, 7),
            Cell(5, 2, 4),
            Cell(5, 3, 0),
            Cell(5, 4, 9),
            Cell(5, 5, 0),
            Cell(5, 6, 0),
            Cell(5, 7, 0),
            Cell(5, 8, 1)
        ),
        listOf(
            Cell(6, 0, 0),
            Cell(6, 1, 0),
            Cell(6, 2, 0),
            Cell(6, 3, 0),
            Cell(6, 4, 0),
            Cell(6, 5, 0),
            Cell(6, 6, 0),
            Cell(6, 7, 0),
            Cell(6, 8, 0)
        ),
        listOf(
            Cell(7, 0, 0),
            Cell(7, 1, 0),
            Cell(7, 2, 9),
            Cell(7, 3, 0),
            Cell(7, 4, 0),
            Cell(7, 5, 5),
            Cell(7, 6, 0),
            Cell(7, 7, 0),
            Cell(7, 8, 0)
        ),
        listOf(
            Cell(8, 0, 0),
            Cell(8, 1, 0),
            Cell(8, 2, 3),
            Cell(8, 3, 0),
            Cell(8, 4, 4),
            Cell(8, 5, 0),
            Cell(8, 6, 0),
            Cell(8, 7, 0),
            Cell(8, 8, 0)
        ),
    )

    fun setFirstLaunch(value: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsDataManager.setFirstLaunch(value)
        }
    }
}
