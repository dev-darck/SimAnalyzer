package com.analyzer.session.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.SessionStatCard

@Composable
internal fun SessionDetailsStatsRow(
    stats: SessionDetailStatsUi,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 900.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionStatCard(title = "Best Lap", value = stats.bestLapLabel)
                SessionStatCard(title = "Avg Lap (Valid)", value = stats.averageLapLabel)
                SessionStatCard(title = "Total Incidents", value = stats.incidentsCount.toString())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionStatCard(
                    title = "Best Lap",
                    value = stats.bestLapLabel,
                    modifier = Modifier.weight(1f)
                )
                SessionStatCard(
                    title = "Avg Lap (Valid)",
                    value = stats.averageLapLabel,
                    modifier = Modifier.weight(1f)
                )
                SessionStatCard(
                    title = "Total Incidents",
                    value = stats.incidentsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SessionDetailsStatsRowPreview() {
    SimAnalyzerTheme {
        SessionDetailsStatsRow(
            stats = SessionDetailStatsUi(
                bestLapLabel = "1:58.253",
                averageLapLabel = "2:00.417",
                incidentsCount = 3
            ),
            modifier = Modifier
        )
    }
}
