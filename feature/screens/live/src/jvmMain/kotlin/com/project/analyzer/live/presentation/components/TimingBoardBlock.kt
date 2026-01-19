package com.project.analyzer.live.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
    lapCount: Int = 0,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
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
                deltaColor = Color(0xFF43D18A)
            )

            TimingDivider()

            TimingRow(
                type = "Last lap",
                time = lastLapTime,
                delta = deltaLastTime,
                deltaColor = Color(0xFFFF4D6D)
            )

            TimingDivider()

            TimingRow(
                type = "Best lap",
                time = bestLapTime,
                delta = "-",
                deltaColor = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.7f)
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
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.55f),
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
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.45f),
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
        color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun BlockHeader(lapCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.1f))
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
            color = SimAnalyzerTheme.material.onSurface.copy(alpha = 0.5f)
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
