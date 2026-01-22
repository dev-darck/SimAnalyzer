package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme

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
            .background(SimAnalyzerTheme.material.surface)
    ) {
        BlockHeader(lapCount)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 22.dp, bottom = 8.dp)
        ) {
            TimingHeaderRow()
            TimingDivider()

            TimingRow(
                type = "Current lap",
                time = currentLapTime,
                delta = deltaCurrentTime,
                deltaColor = if (!deltaCurrentIsPositive) positiveColor else negativeColor
            )

            TimingDivider()

            TimingRow(
                type = "Last lap",
                time = lastLapTime,
                delta = deltaLastTime,
                deltaColor = if (!deltaLastIsPositive) positiveColor else negativeColor
            )

            TimingDivider()

            TimingRow(
                type = "Best lap",
                time = bestLapTime,
                delta = "-",
                deltaColor = SimAnalyzerTheme.extended.surface50
            )
        }
    }
}

@Composable
private fun TimingRow(
    type: String,
    time: String,
    delta: String,
    deltaColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = type,
            modifier = Modifier.weight(weight = 0.55f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            text = time,
            modifier = Modifier.weight(weight = 0.30f),
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Text(
            text = delta,
            modifier = Modifier.weight(weight = 0.15f),
            color = deltaColor,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TimingHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TYPE",
            modifier = Modifier.weight(weight = 0.55f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall
        )

        Text(
            text = "TIME",
            modifier = Modifier.weight(weight = 0.30f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )

        Text(
            text = "DELTA",
            modifier = Modifier.weight(weight = 0.15f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Timing Board",
            fontSize = 20.sp,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
            color = SimAnalyzerTheme.material.onSurface
        )

        Text(
            text = "Lap $lapCount",
            fontSize = 12.sp,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            color = SimAnalyzerTheme.extended.surface50
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
