@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.calibration.presentation.trackmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.CalibrationSectionCard
import com.project.analyzer.calibration.trackmap.TrackMapRecorderState
import com.project.analyzer.feature.dev.calibration.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrackMapLibraryScreen() {
    val viewModel = metroViewModel<TrackMapLibraryViewModel>()
    val items by viewModel.items.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalibrationSectionCard(
            title = stringResource(Res.string.track_map_library_title),
            subtitle = stringResource(Res.string.track_map_library_subtitle),
        ) {
            Text(
                text = stringResource(Res.string.track_map_library_total, items.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        if (items.isEmpty()) {
            CalibrationSectionCard(
                title = stringResource(Res.string.track_map_library_empty_title),
                subtitle = stringResource(Res.string.track_map_library_empty_subtitle),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(items, key = { it.map.gameId + it.map.trackId + it.map.layoutId.orEmpty() }) { item ->
                    TrackMapLibraryCard(item)
                }
            }
        }
    }
}

@Composable
private fun TrackMapLibraryCard(item: TrackMapLibraryItem) {
    var showPoints by remember(item) { mutableStateOf(false) }
    val previewState = remember(item) {
        TrackMapRecorderState(
            recording = false,
            trackId = item.map.trackId,
            trackName = item.map.trackName,
            layoutId = item.map.layoutId?.takeIf { it.isNotBlank() },
            points = item.points,
            leftWidthsMeters = item.leftWidthsMeters,
            rightWidthsMeters = item.rightWidthsMeters,
            pointCount = item.points.size,
            totalDistanceMeters = item.distanceMeters,
            bounds = item.bounds,
            pitPoints = item.pitPoints,
            pitPointCount = item.pitPoints.size,
            averageTrackWidthMeters = averageWidth(
                leftWidths = item.leftWidthsMeters,
                rightWidths = item.rightWidthsMeters,
            ),
            pitEntryPoint = item.pitEntryPoint,
            pitExitPoint = item.pitExitPoint,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .border(1.dp, SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f), SimAnalyzerTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.map.trackName.ifBlank { item.map.trackId },
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val gameLabel = item.map.gameId.ifBlank { "unknown" }
                Text(
                    text = "$gameLabel · ${item.map.trackId}",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
                item.map.layoutId?.takeIf { it.isNotBlank() }?.let { layoutId ->
                    Text(
                        text = layoutId,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.bodySmall,
                    )
                }
            }
        }

        TrackMapPreview(
            state = previewState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            showStatus = false,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.track_map_library_points, item.points.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.track_map_library_distance, formatDistance(item.distanceMeters)),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    Res.string.track_map_library_width,
                    formatDecimal(
                        averageWidth(
                            leftWidths = item.leftWidthsMeters,
                            rightWidths = item.rightWidthsMeters,
                        ),
                        decimals = 1,
                    ),
                ),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.track_map_library_pit, item.pitPoints.size),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = formatEpoch(item.map.createdAtEpochMs),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        OutlinedButton(onClick = { showPoints = !showPoints }) {
            Text(
                text = stringResource(
                    if (showPoints) Res.string.track_map_library_hide_points else Res.string.track_map_library_show_points,
                ),
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }

        if (showPoints) {
            val maxPreview = 200
            val previewLines = remember(item.points) {
                item.points.take(maxPreview).joinToString(separator = "\n") { point ->
                    "%.3f, %.3f".format(point.x, point.y)
                }
            }
            val truncatedCount = (item.points.size - maxPreview).coerceAtLeast(0)
            val previewText = if (truncatedCount > 0) {
                previewLines + stringResource(Res.string.track_map_library_more_points, truncatedCount)
            } else {
                previewLines
            }

            Text(
                text = previewText,
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
    }
}

private fun formatDistance(distanceMeters: Float): String = if (distanceMeters >= 1000f) {
    "%.2f km".format(distanceMeters / 1000f)
} else {
    "%.1f m".format(distanceMeters)
}

private fun formatEpoch(epochMs: Long): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    return Instant.ofEpochMilli(epochMs)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}

private fun averageWidth(leftWidths: List<Float>, rightWidths: List<Float>): Float {
    if (leftWidths.isEmpty() || rightWidths.isEmpty()) return 0f
    val size = minOf(leftWidths.size, rightWidths.size)
    if (size == 0) return 0f
    var sum = 0f
    for (index in 0 until size) {
        sum += leftWidths[index] + rightWidths[index]
    }
    return sum / size
}
