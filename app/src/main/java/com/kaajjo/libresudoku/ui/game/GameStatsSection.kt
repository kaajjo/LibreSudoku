package com.kaajjo.libresudoku.ui.game

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kaajjo.libresudoku.R
import com.kaajjo.libresudoku.core.PreferencesConstants
import com.kaajjo.libresudoku.core.qqwing.GameDifficulty
import com.kaajjo.libresudoku.core.qqwing.GameType
import com.kaajjo.libresudoku.core.utils.toFormattedString
import com.kaajjo.libresudoku.data.database.model.Record
import kotlin.time.toKotlinDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AfterGameStats(
    difficulty: GameDifficulty,
    type: GameType,
    hintsUsed: Int,
    mistakesMade: Int,
    mistakesLimit: Boolean,
    mistakesLimitCount: Int,
    giveUp: Boolean,
    notesTaken: Int,
    records: List<Record>,
    timeText: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = if (giveUp) {
                if (mistakesLimit && mistakesLimitCount >= PreferencesConstants.MISTAKES_LIMIT) {
                    stringResource(R.string.saved_game_mistakes_limit)
                } else {
                    stringResource(R.string.saved_game_give_up)
                }
            } else {
                stringResource(R.string.game_completed)
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )

        if (!giveUp) {
            Text(
                text = stringResource(R.string.time),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBoxWithBottomPadding(
                    text = {
                        Text(
                            stringResource(
                                R.string.stat_time_current,
                                timeText
                            )
                        )
                    }
                )

                if (records.isNotEmpty()) {
                    StatBoxWithBottomPadding(
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.stat_time_average,
                                    DateUtils.formatElapsedTime(records.sumOf { it.time.seconds } / records.count())
                                )
                            )
                        }
                    )
                    StatBoxWithBottomPadding(
                        text = {
                            Text(
                                text = stringResource(
                                    R.string.stat_time_best,
                                    records.first().time
                                        .toKotlinDuration()
                                        .toFormattedString()
                                )
                            )
                        }
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.statistics),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatBoxWithBottomPadding(
                text = {
                    Text(
                        "${stringResource(difficulty.resName)} ${
                            stringResource(
                                type.resName
                            )
                        }"
                    )
                },
                icon = { Icon(Icons.Rounded.Grade, contentDescription = null) }
            )
            StatBoxWithBottomPadding(
                text = {
                    Text(
                        stringResource(
                            R.string.hints_used,
                            hintsUsed
                        )
                    )
                },
                icon = { Icon(Icons.Rounded.Lightbulb, contentDescription = null) }
            )
            StatBoxWithBottomPadding(
                text = {
                    Text(
                        stringResource(
                            R.string.mistakes_made,
                            mistakesMade
                        )
                    )
                },
                icon = { Icon(Icons.Rounded.Cancel, contentDescription = null) }
            )
            StatBoxWithBottomPadding(
                text = {
                    Text(
                        stringResource(
                            R.string.notes_taken,
                            notesTaken
                        )
                    )
                },
                icon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
            )
        }
    }
}


@Composable
fun StatBox(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = { }
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            text()
        }
    }
}

// TODO: Remove this when cross-axis arrangement support is added to FlowRow
// https://android-review.googlesource.com/c/platform/frameworks/support/+/2478295
@Composable
fun StatBoxWithBottomPadding(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = { }
) {
    StatBox(
        text = text,
        icon = icon,
        modifier = modifier.padding(bottom = 8.dp)
    )
}