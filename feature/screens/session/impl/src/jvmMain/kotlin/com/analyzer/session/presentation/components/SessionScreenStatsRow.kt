package com.analyzer.session.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.SessionStatCard

@Composable
internal fun SessionScreenStatsRow(
    stats: SessionStatsUi,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 900.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionStatCard(title = "Total Distance (km)", value = stats.totalDistanceLabel)
                SessionStatCard(title = "Sessions", value = stats.sessionsCount.toString())
                SessionStatCard(title = "Total Incidents", value = stats.incidentsCount.toString())
                SessionStatCard(title = "Favorite Car", value = stats.favoriteCar)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SessionStatCard(
                    title = "Total Distance (km)",
                    value = stats.totalDistanceLabel,
                    modifier = Modifier.weight(1f)
                )
                SessionStatCard(
                    title = "Sessions",
                    value = stats.sessionsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionStatCard(
                    title = "Total Incidents",
                    value = stats.incidentsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                SessionStatCard(
                    title = "Favorite Car",
                    value = stats.favoriteCar,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun SessionScreenStatsRowPreview() {
    SimAnalyzerTheme {
        SessionScreenStatsRow(
            stats = SessionStatsUi(
                totalDistanceLabel = "123.450",
                sessionsCount = 24,
                incidentsCount = 4,
                favoriteCar = "Car Name"
            )
        )
    }
}
