package com.analyzer.session.details.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.analyzer.session.details.presentation.model.SessionDetailStatsUi
import com.project.analyzer.feature.screens.sessionDetails.Res.Res
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_stats_average_lap
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_stats_best_lap
import com.project.analyzer.feature.screens.sessionDetails.Res.session_details_stats_incidents
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.StatItem
import com.project.analyzer.ui.components.StatsRow
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailsStatsRow(stats: SessionDetailStatsUi, modifier: Modifier = Modifier) {
    StatsRow(
        stats = listOf(
            StatItem(title = stringResource(Res.string.session_details_stats_best_lap), value = stats.bestLapLabel),
            StatItem(
                title = stringResource(Res.string.session_details_stats_average_lap),
                value = stats.averageLapLabel,
            ),
            StatItem(
                title = stringResource(Res.string.session_details_stats_incidents),
                value = stats.incidentsCount.toString(),
            ),
        ),
        modifier = modifier,
        horizontalSpacing = 12,
    )
}

@Preview
@Composable
private fun SessionDetailsStatsRowPreview() {
    SimAnalyzerTheme {
        SessionDetailsStatsRow(
            stats = SessionDetailStatsUi(
                bestLapLabel = "1:58.253",
                averageLapLabel = "2:00.417",
                incidentsCount = 3,
            ),
            modifier = Modifier,
        )
    }
}
