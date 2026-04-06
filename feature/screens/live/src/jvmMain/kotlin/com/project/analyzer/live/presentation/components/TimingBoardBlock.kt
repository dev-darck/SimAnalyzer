@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.feature.screens.live.Res.Res
import com.project.analyzer.feature.screens.live.Res.timing_board_best_lap
import com.project.analyzer.feature.screens.live.Res.timing_board_current_lap
import com.project.analyzer.feature.screens.live.Res.timing_board_header_delta
import com.project.analyzer.feature.screens.live.Res.timing_board_header_time
import com.project.analyzer.feature.screens.live.Res.timing_board_header_type
import com.project.analyzer.feature.screens.live.Res.timing_board_lap
import com.project.analyzer.feature.screens.live.Res.timing_board_last_lap
import com.project.analyzer.feature.screens.live.Res.timing_board_no_delta
import com.project.analyzer.feature.screens.live.Res.timing_board_title
import com.project.analyzer.theme.SimAnalyzerTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun TimingBoardBlock(
    modifier: Modifier = Modifier,
    bestLapTime: String = "0:00.000",
    currentLapTime: String = "0:00.000",
    lastLapTime: String = "0:00.000",
    deltaCurrentTime: String = "+0.000",
    deltaLastTime: String = "-0.000",
    deltaCurrentIsPositive: Boolean = false,
    deltaLastIsPositive: Boolean = false,
    lapCount: Int = 0,
) {
    val positiveColor = SimAnalyzerTheme.extended.teal
    val negativeColor = SimAnalyzerTheme.extended.red

    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface),
    ) {
        BlockHeader(lapCount)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 22.dp, bottom = 8.dp),
        ) {
            TimingHeaderRow()
            TimingDivider()

            TimingRow(
                type = stringResource(Res.string.timing_board_current_lap),
                time = currentLapTime,
                delta = deltaCurrentTime,
                deltaColor = if (!deltaCurrentIsPositive) positiveColor else negativeColor,
            )

            TimingDivider()

            TimingRow(
                type = stringResource(Res.string.timing_board_last_lap),
                time = lastLapTime,
                delta = deltaLastTime,
                deltaColor = if (!deltaLastIsPositive) positiveColor else negativeColor,
            )

            TimingDivider()

            TimingRow(
                type = stringResource(Res.string.timing_board_best_lap),
                time = bestLapTime,
                delta = stringResource(Res.string.timing_board_no_delta),
                deltaColor = SimAnalyzerTheme.extended.surface50,
            )
        }
    }
}

@Composable
private fun TimingRow(type: String, time: String, delta: String, deltaColor: Color) {
    val valueStyle = SimAnalyzerTheme.typography.titleSmall.copy(
        fontFamily = SimAnalyzerTheme.fonts.mono,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type,
            modifier = Modifier.weight(weight = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodyMedium,
        )

        Text(
            text = time,
            modifier = Modifier.weight(weight = 0.30f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface,
            style = valueStyle,
            textAlign = TextAlign.Center,
        )

        Text(
            text = delta,
            modifier = Modifier.weight(weight = 0.15f),
            color = deltaColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = valueStyle,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimingHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.timing_board_header_type),
            modifier = Modifier.weight(weight = 0.55f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.labelSmall,
        )

        Text(
            text = stringResource(Res.string.timing_board_header_time),
            modifier = Modifier.weight(weight = 0.30f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f),
            style = SimAnalyzerTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )

        Text(
            text = stringResource(Res.string.timing_board_header_delta),
            modifier = Modifier.weight(weight = 0.15f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f),
            style = SimAnalyzerTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimingDivider() {
    HorizontalDivider(
        color = SimAnalyzerTheme.material.outlineVariant,
    )
}

@Composable
private fun BlockHeader(lapCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimAnalyzerTheme.material.secondaryContainer)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.timing_board_title),
            maxLines = 1,
            style = SimAnalyzerTheme.typography.labelMedium,
            color = SimAnalyzerTheme.material.onSurface,
        )

        Text(
            text = stringResource(Res.string.timing_board_lap, lapCount),
            maxLines = 1,
            style = SimAnalyzerTheme.typography.labelMedium.copy(
                fontFamily = SimAnalyzerTheme.fonts.mono,
            ),
            color = SimAnalyzerTheme.extended.surface50,
        )
    }
}

@Preview
@Composable
private fun TimingBoardBlockPreview() {
    SimAnalyzerTheme {
        TimingBoardBlock()
    }
}
