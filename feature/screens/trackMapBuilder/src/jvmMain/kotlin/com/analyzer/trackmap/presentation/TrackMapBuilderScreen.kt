@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.trackmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.trackmap.presentation.components.TrackMapReferencePointDropdown
import com.analyzer.trackmap.presentation.components.TrackMapSectionCard
import com.analyzer.trackmap.presentation.model.TrackMapBuilderUiState
import com.project.analyzer.feature.screens.trackMapBuilder.Res.Res
import com.project.analyzer.feature.screens.trackMapBuilder.Res.calibration_capture_settings_subtitle
import com.project.analyzer.feature.screens.trackMapBuilder.Res.calibration_capture_settings_title
import com.project.analyzer.feature.screens.trackMapBuilder.Res.calibration_reference_point
import com.project.analyzer.feature.screens.trackMapBuilder.Res.calibration_reset
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_actions_subtitle
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_actions_title
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_auto
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_avg_width
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_build_preview_subtitle
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_build_preview_title
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_builder_subtitle
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_builder_title
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_detected_track
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_distance
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_fallback_half_width
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_game
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_guidance
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_lap
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_laps_recorded
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_layout
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_manual
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_mark_pit_entry
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_mark_pit_exit
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_need_points
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_no
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_pit_entry
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_pit_exit
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_pit_lane
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_pit_points
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_pit_source
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_points
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_sampling
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_save
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_saving
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_sectors
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_set
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_start
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_status_idle
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_status_recording
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_status_saved
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_stop
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_track_id
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_width_coverage
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_width_minus
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_width_plus
import com.project.analyzer.feature.screens.trackMapBuilder.Res.track_map_yes
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import com.project.analyzer.ui.format.formatPercent
import com.project.analyzer.ui.modifier.TestTags
import com.project.analyzer.ui.modifier.uiTestTag
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

private const val MIN_POINTS_TO_SAVE_UI = 50

@Composable
fun TrackMapBuilderScreen() {
    val viewModel = metroViewModel<TrackMapBuilderViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    TrackMapBuilderContent(
        state = state,
        onStart = viewModel::start,
        onStop = viewModel::stop,
        onReset = viewModel::reset,
        onSave = viewModel::save,
        onReferencePoint = viewModel::setReferencePoint,
        onFallbackHalfWidth = viewModel::setFallbackHalfWidthMeters,
        onPitEntry = viewModel::markPitEntry,
        onPitExit = viewModel::markPitExit,
    )
}

@Composable
internal fun TrackMapBuilderContent(
    state: TrackMapBuilderUiState,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onReset: () -> Unit = {},
    onSave: () -> Unit = {},
    onReferencePoint: (ReferencePoint) -> Unit = {},
    onFallbackHalfWidth: (Float) -> Unit = {},
    onPitEntry: () -> Unit = {},
    onPitExit: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val screenScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background),
    ) {
        Column(
            modifier = Modifier
                .uiTestTag(TestTags.TrackMapBuilderScroll)
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .padding(16.dp)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrackMapBuilderOverviewSection(state = state)
            TrackMapBuilderCaptureSettingsSection(
                state = state,
                onReferencePoint = onReferencePoint,
                onFallbackHalfWidth = onFallbackHalfWidth,
            )
            TrackMapBuilderPreviewSection(state = state)
            TrackMapBuilderActionsSection(
                state = state,
                onStart = onStart,
                onStop = onStop,
                onReset = onReset,
                onSave = onSave,
                onPitEntry = onPitEntry,
                onPitExit = onPitExit,
            )
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(screenScrollState)),
        )
    }
}

@Composable
private fun TrackMapBuilderOverviewSection(state: TrackMapBuilderUiState) {
    TrackMapSectionCard(
        title = stringResource(Res.string.track_map_builder_title),
        subtitle = stringResource(Res.string.track_map_builder_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val gameLabel = state.gameLabel.ifBlank { state.gameId }
            if (gameLabel.isNotBlank()) {
                InfoRow(label = stringResource(Res.string.track_map_game), value = gameLabel)
            }
            InfoRow(
                label = stringResource(Res.string.track_map_detected_track),
                value = state.detectedTrackLabel(),
            )
            InfoRow(label = stringResource(Res.string.track_map_track_id), value = state.trackId.ifBlank { "-" })
            state.layoutId?.let { InfoRow(label = stringResource(Res.string.track_map_layout), value = it) }
            InfoRow(label = stringResource(Res.string.track_map_lap), value = state.lapIndex?.toString() ?: "-")
            InfoRow(label = stringResource(Res.string.track_map_laps_recorded), value = state.lapsRecorded.toString())
            InfoRow(label = stringResource(Res.string.track_map_sectors), value = state.sectorProgressLabel())
            InfoRow(
                label = stringResource(Res.string.track_map_pit_lane),
                value = stringResource(if (state.isInPitLane) Res.string.track_map_yes else Res.string.track_map_no),
            )
            InfoRow(
                label = stringResource(Res.string.track_map_pit_source),
                value = stringResource(
                    if (state.pitOverrideActive) Res.string.track_map_manual else Res.string.track_map_auto,
                ),
            )
            InfoRow(
                label = stringResource(Res.string.track_map_pit_entry),
                value = if (state.pitEntryPointSet) stringResource(Res.string.track_map_set) else "-",
            )
            InfoRow(
                label = stringResource(Res.string.track_map_pit_exit),
                value = if (state.pitExitPointSet) stringResource(Res.string.track_map_set) else "-",
            )
            InfoRow(label = stringResource(Res.string.track_map_pit_points), value = state.pitPointCount.toString())
            InfoRow(label = stringResource(Res.string.track_map_points), value = state.pointCount.toString())
            InfoRow(
                label = stringResource(Res.string.track_map_distance),
                value = "${formatDecimal(state.totalDistanceMeters, decimals = 1)} m",
            )
            InfoRow(
                label = stringResource(Res.string.track_map_avg_width),
                value = "${formatDecimal(state.averageTrackWidthMeters, decimals = 1)} m",
            )
            InfoRow(
                label = stringResource(Res.string.track_map_width_coverage),
                value = state.widthCoverageLabel(),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                label = stringResource(
                    if (state.recording) Res.string.track_map_status_recording else Res.string.track_map_status_idle,
                ),
                accent = if (state.recording) {
                    SimAnalyzerTheme.extended.teal
                } else {
                    SimAnalyzerTheme.material.onSurfaceVariant
                },
            )
            state.lastSavedTrackId?.let { lastSavedTrackId ->
                StatusPill(
                    label = stringResource(Res.string.track_map_status_saved, lastSavedTrackId),
                    accent = SimAnalyzerTheme.material.primary,
                )
            }
        }
    }
}

@Composable
private fun TrackMapBuilderCaptureSettingsSection(
    state: TrackMapBuilderUiState,
    onReferencePoint: (ReferencePoint) -> Unit,
    onFallbackHalfWidth: (Float) -> Unit,
) {
    TrackMapSectionCard(
        title = stringResource(Res.string.calibration_capture_settings_title),
        subtitle = stringResource(Res.string.calibration_capture_settings_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(Res.string.calibration_reference_point),
                style = SimAnalyzerTheme.typography.bodyMedium,
                color = SimAnalyzerTheme.material.onSurface,
            )
            TrackMapReferencePointDropdown(selected = state.referencePoint, onSelected = onReferencePoint)

            Text(
                text = stringResource(
                    Res.string.track_map_sampling,
                    formatDecimal(state.minSpacingMeters, decimals = 1),
                    formatDecimal(state.maxSpacingMeters, decimals = 1),
                    formatDecimal(state.minAngleDeg, decimals = 0),
                    formatDecimal(state.minSpeedKmh, decimals = 0),
                ),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onFallbackHalfWidth(state.fallbackHalfWidthMeters - 0.5f) }) {
                    Text(
                        text = stringResource(Res.string.track_map_width_minus),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(onClick = { onFallbackHalfWidth(state.fallbackHalfWidthMeters + 0.5f) }) {
                    Text(
                        text = stringResource(Res.string.track_map_width_plus),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.track_map_fallback_half_width,
                        formatDecimal(state.fallbackHalfWidthMeters, decimals = 1),
                    ),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
            }
            state.guidanceText?.let { hint ->
                Text(
                    text = stringResource(Res.string.track_map_guidance, hint),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.extended.amber,
                )
            }
        }
    }
}

@Composable
private fun TrackMapBuilderPreviewSection(state: TrackMapBuilderUiState) {
    TrackMapSectionCard(
        title = stringResource(Res.string.track_map_build_preview_title),
        subtitle = stringResource(Res.string.track_map_build_preview_subtitle),
    ) {
        TrackMapPreview(
            state = state.preview,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp),
            showStatus = false,
        )
    }
}

@Composable
private fun TrackMapBuilderActionsSection(
    state: TrackMapBuilderUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onPitEntry: () -> Unit,
    onPitExit: () -> Unit,
) {
    val canSave = state.canSave()

    TrackMapSectionCard(
        title = stringResource(Res.string.track_map_actions_title),
        subtitle = stringResource(Res.string.track_map_actions_subtitle),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStart,
                enabled = !state.recording && !state.isSaving,
                modifier = Modifier.uiTestTag(TestTags.TrackMapBuilderStart),
            ) {
                Text(
                    text = stringResource(Res.string.track_map_start),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }

            Button(onClick = onStop, enabled = state.recording) {
                Text(
                    text = stringResource(Res.string.track_map_stop),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }

            OutlinedButton(onClick = onReset, enabled = !state.isSaving) {
                Text(
                    text = stringResource(Res.string.calibration_reset),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }

            Button(onClick = onSave, enabled = canSave) {
                Text(
                    text = stringResource(
                        if (state.isSaving) Res.string.track_map_saving else Res.string.track_map_save,
                    ),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onPitEntry, enabled = state.recording) {
                Text(
                    text = stringResource(Res.string.track_map_mark_pit_entry),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
            OutlinedButton(onClick = onPitExit, enabled = state.recording) {
                Text(
                    text = stringResource(Res.string.track_map_mark_pit_exit),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        if (state.pointCount < MIN_POINTS_TO_SAVE_UI) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.track_map_need_points, MIN_POINTS_TO_SAVE_UI),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        }

        state.message?.let { message ->
            Spacer(modifier = Modifier.height(10.dp))
            MessageBanner(message)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Text(
            text = value,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface,
        )
    }
}

@Composable
private fun StatusPill(label: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(accent.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.4f),
                shape = SimAnalyzerTheme.shapes.medium,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = accent,
            style = SimAnalyzerTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun MessageBanner(message: String) {
    val accent = SimAnalyzerTheme.material.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f))
            .border(1.dp, accent.copy(alpha = 0.4f), SimAnalyzerTheme.shapes.medium)
            .padding(10.dp),
    ) {
        Text(
            text = message,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = accent,
        )
    }
}

private fun TrackMapBuilderUiState.detectedTrackLabel(): String = listOfNotNull(
    trackName.takeIf { it.isNotBlank() },
    trackId.takeIf { it.isNotBlank() },
).joinToString(" / ").ifBlank { "-" }

private fun TrackMapBuilderUiState.sectorProgressLabel(): String = if (sectorCount > 0) {
    "$capturedSectorCount/$sectorCount"
} else {
    "-"
}

private fun TrackMapBuilderUiState.widthCoverageLabel(): String =
    "L ${formatPercent((leftCoverageRatio * 100f).toInt())} / R ${formatPercent((rightCoverageRatio * 100f).toInt())}"

private fun TrackMapBuilderUiState.canSave(): Boolean =
    !recording && !isSaving && pointCount >= MIN_POINTS_TO_SAVE_UI
