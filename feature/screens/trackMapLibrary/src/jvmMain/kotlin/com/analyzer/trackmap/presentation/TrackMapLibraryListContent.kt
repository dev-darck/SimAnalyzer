@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.trackmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import com.analyzer.trackmap.presentation.components.TrackMapSectionCard
import com.analyzer.trackmap.presentation.model.TrackMapLibraryCardUi
import com.project.analyzer.feature.screens.trackMapLibrary.Res.*
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TrackMapLibraryListContent(items: List<TrackMapLibraryCardUi>, onOpenEditor: (TrackMapLibraryCardUi) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrackMapSectionCard(
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
            TrackMapSectionCard(
                title = stringResource(Res.string.track_map_library_empty_title),
                subtitle = stringResource(Res.string.track_map_library_empty_subtitle),
            )
        } else {
            val listState = rememberLazyListState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 10.dp),
                ) {
                    items(
                        items = items,
                        key = { item ->
                            item.mapKey
                        },
                    ) { item ->
                        TrackMapLibraryCard(
                            card = item,
                            onOpenEditor = { onOpenEditor(item) },
                        )
                    }
                }
                AppVerticalScrollbar(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    adapter = rememberScrollbarAdapter(listState),
                )
            }
        }
    }
}

@Composable
private fun TrackMapLibraryCard(card: TrackMapLibraryCardUi, onOpenEditor: () -> Unit) {
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = card.trackName.ifBlank { card.trackId },
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val gameLabel = card.gameId.ifBlank { "unknown" }
                Text(
                    text = "$gameLabel · ${card.trackId}",
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    style = SimAnalyzerTheme.typography.bodySmall,
                )
                card.layoutId?.takeIf { it.isNotBlank() }?.let { layoutId ->
                    Text(
                        text = layoutId,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        style = SimAnalyzerTheme.typography.bodySmall,
                    )
                }
            }
        }

        TrackMapPreview(
            state = card.preview,
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
                text = stringResource(Res.string.track_map_library_points, card.pointCount),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.track_map_library_distance, formatDistance(card.distanceMeters)),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(
                    Res.string.track_map_library_width,
                    formatDecimal(card.averageTrackWidthMeters, decimals = 1),
                ),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.track_map_library_pit, card.pitPointCount),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = formatEpoch(card.createdAtEpochMs),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenEditor) {
                Text(
                    text = stringResource(Res.string.track_map_library_edit),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
            OutlinedButton(onClick = { showPoints = !showPoints }) {
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

        if (showPoints) {
            val maxPreview = 200
            val previewLines = remember(card.points) {
                card.points.take(maxPreview).joinToString(separator = "\n") { point ->
                    "%.3f, %.3f".format(point.x, point.y)
                }
            }
            val truncatedCount = (card.points.size - maxPreview).coerceAtLeast(0)
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
