package com.analyzer.trackmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryHeaderUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryPointsPreviewUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryStatsUi
import com.project.analyzer.feature.screens.trackMapLibrary.Res.Res
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_distance
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_edit
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_hide_points
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_more_points
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_pit
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_points
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_show_points
import com.project.analyzer.feature.screens.trackMapLibrary.Res.track_map_library_width
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import org.jetbrains.compose.resources.stringResource
import java.util.Locale

@Composable
internal fun TrackMapLibraryCard(card: TrackMapLibraryCardUi, onOpenEditor: () -> Unit) {
    var showPoints by remember(card.mapKey) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f), SimAnalyzerTheme.shapes.large)
            .clickable(onClick = onOpenEditor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackMapLibraryCardHeader(header = card.header)
        TrackMapPreview(
            state = card.preview,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            showStatus = false,
        )
        TrackMapLibraryCardStats(stats = card.stats)
        TrackMapLibraryCardActions(
            showPoints = showPoints,
            onOpenEditor = onOpenEditor,
            onTogglePoints = { showPoints = !showPoints },
        )
        if (showPoints) {
            TrackMapLibraryPointsPreview(pointsPreview = card.pointsPreview)
        }
    }
}

@Composable
private fun TrackMapLibraryCardHeader(header: TrackMapLibraryHeaderUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = header.title,
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = header.subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            header.layoutLabel?.let { layoutLabel ->
                Text(
                    text = layoutLabel,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TrackMapLibraryCardStats(stats: TrackMapLibraryStatsUi) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TrackMapLibraryStat(
            text = stringResource(Res.string.track_map_library_points, stats.pointCount),
        )
        TrackMapLibraryStat(
            text = stringResource(
                Res.string.track_map_library_distance,
                formatDistance(stats.distanceMeters),
            ),
        )
        TrackMapLibraryStat(
            text = stringResource(
                Res.string.track_map_library_width,
                formatDecimal(stats.averageTrackWidthMeters, decimals = 1),
            ),
        )
        TrackMapLibraryStat(
            text = stringResource(Res.string.track_map_library_pit, stats.pitPointCount),
        )
        TrackMapLibraryStat(text = stats.createdAtLabel)
    }
}

@Composable
private fun TrackMapLibraryStat(text: String) {
    Text(
        text = text,
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        style = SimAnalyzerTheme.typography.bodySmall,
    )
}

@Composable
private fun TrackMapLibraryCardActions(showPoints: Boolean, onOpenEditor: () -> Unit, onTogglePoints: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onOpenEditor) {
            Text(
                text = stringResource(Res.string.track_map_library_edit),
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
        OutlinedButton(onClick = onTogglePoints) {
            Text(
                text = stringResource(
                    if (showPoints) {
                        Res.string.track_map_library_hide_points
                    } else {
                        Res.string.track_map_library_show_points
                    },
                ),
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TrackMapLibraryPointsPreview(pointsPreview: TrackMapLibraryPointsPreviewUi) {
    val previewText = remember(pointsPreview) {
        pointsPreview.points.joinToString(separator = "\n") { point ->
            String.format(Locale.US, "%.3f, %.3f", point.x, point.y)
        }
    }
    val text = if (pointsPreview.hiddenCount > 0) {
        previewText + stringResource(
            Res.string.track_map_library_more_points,
            pointsPreview.hiddenCount,
        )
    } else {
        previewText
    }

    Text(
        text = text,
        color = SimAnalyzerTheme.material.onSurfaceVariant,
        style = SimAnalyzerTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.25f),
                SimAnalyzerTheme.shapes.medium,
            )
            .padding(10.dp),
    )
}

private fun formatDistance(distanceMeters: Float): String = if (distanceMeters >= 1000f) {
    "%.2f km".format(distanceMeters / 1000f)
} else {
    "%.1f m".format(distanceMeters)
}
