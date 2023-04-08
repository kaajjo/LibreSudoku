package com.kaajjo.libresudoku.ui.settings.boardtheme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.rounded.GridGoldenratio
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaajjo.libresudoku.LocalBoardColors
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.Cell
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.ui.components.PreferenceRowSwitch
import com.kaajjo.libresudoku.ui.components.board.Board
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTitle
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.CollapsingTopAppBar
import com.kaajjo.libresudoku.ui.components.collapsing_topappbar.rememberTopAppBarScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoardTheme(
    viewModel: SettingsBoardThemeViewModel,
    navigateBack: () -> Unit
) {
    val scrollBehavior = rememberTopAppBarScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CollapsingTopAppBar(
                collapsingTitle = CollapsingTitle.medium(titleText = stringResource(R.string.board_theme_title)),
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_round_arrow_back_24),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val positionLines by viewModel.positionLines.collectAsStateWithLifecycle(initialValue = PreferencesConstants.DEFAULT_POSITION_LINES)
            val highlightMistakes by viewModel.highlightMistakes.collectAsState(initial = PreferencesConstants.DEFAULT_HIGHLIGHT_MISTAKES)
            val boardCrossHighlight by viewModel.crossHighlight.collectAsState(initial = PreferencesConstants.DEFAULT_BOARD_CROSS_HIGHLIGHT)
            BoardPreviewTheme(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                positionLines = positionLines,
                errosHighlight = highlightMistakes != 0,
                crossHighlight = boardCrossHighlight
            )

            val monetSudokuBoard by viewModel.monetSudokuBoard.collectAsStateWithLifecycle(
                PreferencesConstants.DEFAULT_MONET_SUDOKU_BOARD
            )
            PreferenceRowSwitch(
                title = stringResource(R.string.pref_boardtheme_accent),
                subtitle = stringResource(R.string.pref_boardtheme_accent_subtitle),
                checked = monetSudokuBoard,
                painter = rememberVectorPainter(Icons.Outlined.Palette),
                onClick = {
                    viewModel.updateMonetSudokuBoardSetting(!monetSudokuBoard)
                }
            )

            PreferenceRowSwitch(
                title = stringResource(R.string.pref_position_lines),
                subtitle = stringResource(R.string.pref_position_lines_summ),
                checked = positionLines,
                painter = rememberVectorPainter(Icons.Rounded.GridGoldenratio),
                onClick = { viewModel.updatePositionLinesSetting(!positionLines) }
            )

            PreferenceRowSwitch(
                title = stringResource(R.string.pref_cross_highlight),
                subtitle = stringResource(R.string.pref_cross_highlight_subtitle),
                checked = boardCrossHighlight,
                painter = rememberVectorPainter(Icons.Rounded.GridOn),
                onClick = { viewModel.updateBoardCrossHighlight(!boardCrossHighlight) }
            )
        }
    }
}

@Composable
private fun BoardPreviewTheme(
    positionLines: Boolean,
    errosHighlight: Boolean,
    crossHighlight: Boolean,
    modifier: Modifier = Modifier
) {
    val previewBoard = listOf(
        listOf(
            Cell(0, 0, 0, locked = true),
            Cell(0, 1, 0, locked = true),
            Cell(0, 2, 1, locked = true),
            Cell(0, 3, 0, locked = true),
            Cell(0, 4, 0, locked = true),
            Cell(0, 5, 0, locked = true),
            Cell(0, 6, 9, locked = true),
            Cell(0, 7, 0, locked = true),
            Cell(0, 8, 0, locked = true)
        ),
        listOf(
            Cell(1, 0, 0, locked = true),
            Cell(1, 1, 2, locked = false),
            Cell(1, 2, 0, locked = true),
            Cell(1, 3, 0, locked = true),
            Cell(1, 4, 1, locked = true),
            Cell(1, 5, 7, locked = true),
            Cell(1, 6, 0, locked = true),
            Cell(1, 7, 5, locked = true),
            Cell(1, 8, 4, locked = true)
        ),
        listOf(
            Cell(2, 0, 5, locked = false),
            Cell(2, 1, 0, locked = true),
            Cell(2, 2, 0, locked = true),
            Cell(2, 3, 0, locked = true),
            Cell(2, 4, 2, locked = true),
            Cell(2, 5, 4, locked = true),
            Cell(2, 6, 0, locked = true),
            Cell(2, 7, 0, locked = true),
            Cell(2, 8, 3, locked = true)
        ),
        listOf(
            Cell(3, 0, 2, locked = true),
            Cell(3, 1, 8, locked = true),
            Cell(3, 2, 0, locked = true),
            Cell(3, 3, 0, locked = true),
            Cell(3, 4, 0, locked = true),
            Cell(3, 5, 0, locked = true),
            Cell(3, 6, 0, locked = true),
            Cell(3, 7, 9, locked = true),
            Cell(3, 8, 0, locked = true)
        ),
        listOf(
            Cell(4, 0, 0, locked = true),
            Cell(4, 1, 0, locked = true),
            Cell(4, 2, 5, locked = true),
            Cell(4, 3, 2, locked = true),
            Cell(4, 4, 9, error = true),
            Cell(4, 5, 0, locked = true),
            Cell(4, 6, 0, locked = true),
            Cell(4, 7, 4, locked = true),
            Cell(4, 8, 7, locked = true)
        ),
        listOf(
            Cell(5, 0, 0, locked = true),
            Cell(5, 1, 7, locked = true),
            Cell(5, 2, 4, locked = true),
            Cell(5, 3, 0, locked = true),
            Cell(5, 4, 9, locked = false),
            Cell(5, 5, 0, locked = true),
            Cell(5, 6, 0, locked = true),
            Cell(5, 7, 0, locked = true),
            Cell(5, 8, 1, locked = false)
        ),
        listOf(
            Cell(6, 0, 0, locked = true),
            Cell(6, 1, 0, locked = true),
            Cell(6, 2, 0, locked = true),
            Cell(6, 3, 0, locked = true),
            Cell(6, 4, 0, locked = true),
            Cell(6, 5, 0, locked = true),
            Cell(6, 6, 0, locked = true),
            Cell(6, 7, 0, locked = true),
            Cell(6, 8, 0, locked = true)
        ),
        listOf(
            Cell(7, 0, 0, locked = true),
            Cell(7, 1, 0, locked = true),
            Cell(7, 2, 9, locked = true),
            Cell(7, 3, 0, locked = true),
            Cell(7, 4, 0, locked = true),
            Cell(7, 5, 5, locked = true),
            Cell(7, 6, 0, locked = true),
            Cell(7, 7, 0, locked = true),
            Cell(7, 8, 0, locked = true)
        ),
        listOf(
            Cell(8, 0, 0, locked = true),
            Cell(8, 1, 0, locked = true),
            Cell(8, 2, 3, locked = true),
            Cell(8, 3, 0, locked = true),
            Cell(8, 4, 4, locked = true),
            Cell(8, 5, 0, locked = true),
            Cell(8, 6, 0, locked = true),
            Cell(8, 7, 0, locked = true),
            Cell(8, 8, 0, locked = true)
        ),
    )
    var selectedCell by remember { mutableStateOf(Cell(-1, -1, 0)) }
    Board(
        modifier = modifier,
        board = previewBoard,
        size = 9,
        selectedCell = selectedCell,
        onClick = { cell -> selectedCell = if (selectedCell == cell) Cell(-1, -1, 0) else cell },
        boardColors = LocalBoardColors.current,
        positionLines = positionLines,
        errorsHighlight = errosHighlight,
        crossHighlight = crossHighlight
    )
}