package com.project.analyzer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.project.analyzer.core.ui.Res.Res
import com.project.analyzer.core.ui.Res.stats_row_preview_incidents
import com.project.analyzer.core.ui.Res.stats_row_preview_incidents_value
import com.project.analyzer.core.ui.Res.stats_row_preview_sessions
import com.project.analyzer.core.ui.Res.stats_row_preview_sessions_value
import com.project.analyzer.core.ui.Res.stats_row_preview_total_distance
import com.project.analyzer.core.ui.Res.stats_row_preview_total_distance_value
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource

public data class StatItem(val title: String, val value: String)

@Composable
public fun StatsRow(stats: ImmutableList<StatItem>, modifier: Modifier = Modifier, horizontalSpacing: Int = 16) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing.dp),
    ) {
        stats.forEach { stat ->
            SessionStatCard(
                title = stat.title,
                value = stat.value,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun StatsRowPreview() {
    SimAnalyzerTheme {
        StatsRow(
            stats = persistentListOf(
                StatItem(
                    title = stringResource(Res.string.stats_row_preview_total_distance),
                    value = stringResource(Res.string.stats_row_preview_total_distance_value),
                ),
                StatItem(
                    title = stringResource(Res.string.stats_row_preview_sessions),
                    value = stringResource(Res.string.stats_row_preview_sessions_value),
                ),
                StatItem(
                    title = stringResource(Res.string.stats_row_preview_incidents),
                    value = stringResource(Res.string.stats_row_preview_incidents_value),
                ),
            ),
        )
    }
}
