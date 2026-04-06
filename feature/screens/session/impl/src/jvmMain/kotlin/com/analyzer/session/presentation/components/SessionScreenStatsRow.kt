package com.analyzer.session.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.analyzer.session.presentation.model.SessionStatsUi
import com.project.analyzer.feature.screens.session.impl.Res.Res
import com.project.analyzer.feature.screens.session.impl.Res.session_stats_favorite_car
import com.project.analyzer.feature.screens.session.impl.Res.session_stats_incidents
import com.project.analyzer.feature.screens.session.impl.Res.session_stats_sessions
import com.project.analyzer.feature.screens.session.impl.Res.session_stats_total_distance
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.StatItem
import com.project.analyzer.ui.components.StatsRow
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionScreenStatsRow(stats: SessionStatsUi, modifier: Modifier = Modifier) {
    StatsRow(
        stats = persistentListOf(
            StatItem(title = stringResource(Res.string.session_stats_total_distance), value = stats.totalDistanceLabel),
            StatItem(title = stringResource(Res.string.session_stats_sessions), value = stats.sessionsCount.toString()),
            StatItem(
                title = stringResource(Res.string.session_stats_incidents),
                value = stats.incidentsCount.toString(),
            ),
            StatItem(title = stringResource(Res.string.session_stats_favorite_car), value = stats.favoriteCar),
        ),
        modifier = modifier,
    )
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
                favoriteCar = "Car Name",
            ),
        )
    }
}
