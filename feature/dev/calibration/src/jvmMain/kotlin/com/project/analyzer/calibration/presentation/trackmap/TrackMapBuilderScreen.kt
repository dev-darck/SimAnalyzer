package com.project.analyzer.calibration.presentation.trackmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.CalibrationSectionCard
import com.project.analyzer.calibration.presentation.components.ReferencePointDropdown
import com.project.analyzer.calibration.trackmap.TrackMapRecorderState
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

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
private fun TrackMapBuilderContent(
    state: TrackMapRecorderState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onReferencePoint: (ReferencePoint) -> Unit,
    onFallbackHalfWidth: (Float) -> Unit,
    onPitEntry: () -> Unit,
    onPitExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalibrationSectionCard(
            title = "Track map builder",
            subtitle = "Record a clean lap and save the track line.",
        ) {
            val detectedTrack = listOfNotNull(
                state.trackName.takeIf { it.isNotBlank() },
                state.trackId.takeIf { it.isNotBlank() },
            ).joinToString(" / ").ifBlank { "-" }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val gameLabel = state.gameLabel.ifBlank { state.gameId }
                if (gameLabel.isNotBlank()) {
                    InfoRow(label = "Game", value = gameLabel)
                }
                InfoRow(label = "Detected track", value = detectedTrack)
                InfoRow(label = "Track id", value = state.trackId.ifBlank { "-" })
                state.layoutId?.let { InfoRow(label = "Layout", value = it) }
                InfoRow(label = "Lap", value = state.lapIndex?.toString() ?: "-")
                InfoRow(label = "Laps recorded", value = state.lapsRecorded.toString())
                InfoRow(label = "Pit lane", value = if (state.isInPitLane) "Yes" else "No")
                InfoRow(label = "Pit source", value = if (state.pitOverrideActive) "Manual" else "Auto")
                InfoRow(label = "Pit entry", value = if (state.pitEntryPoint != null) "Set" else "-")
                InfoRow(label = "Pit exit", value = if (state.pitExitPoint != null) "Set" else "-")
                InfoRow(label = "Pit points", value = state.pitPointCount.toString())
                InfoRow(label = "Points", value = state.pointCount.toString())
                InfoRow(label = "Distance", value = "%.1f m".format(state.totalDistanceMeters))
                InfoRow(label = "Avg width", value = "%.1f m".format(state.averageTrackWidthMeters))
                InfoRow(
                    label = "Width coverage",
                    value = "L %.0f%% / R %.0f%%".format(
                        state.leftCoverageRatio * 100f,
                        state.rightCoverageRatio * 100f,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    label = if (state.recording) "Recording" else "Idle",
                    accent = if (state.recording) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.material.onSurfaceVariant,
                )
                if (state.lastSavedTrackId != null) {
                    StatusPill(
                        label = "Saved: ${state.lastSavedTrackId}",
                        accent = SimAnalyzerTheme.material.primary,
                    )
                }
            }
        }

        CalibrationSectionCard(
            title = "Capture settings",
            subtitle = "Reference point and sampling rules.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Reference point",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SimAnalyzerTheme.material.onSurface,
                )
                ReferencePointDropdown(selected = state.referencePoint, onSelected = onReferencePoint)

                Text(
                    text = "Sampling: min ${state.minSpacingMeters}m, max ${state.maxSpacingMeters}m, " +
                        "turn ${state.minAngleDeg} deg, speed >= ${state.minSpeedKmh} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onFallbackHalfWidth(state.fallbackHalfWidthMeters - 0.5f) }) {
                        Text("Width -")
                    }
                    OutlinedButton(onClick = { onFallbackHalfWidth(state.fallbackHalfWidthMeters + 0.5f) }) {
                        Text("Width +")
                    }
                    Text(
                        text = "Fallback half-width: %.1fm".format(state.fallbackHalfWidthMeters),
                        style = MaterialTheme.typography.bodySmall,
                        color = SimAnalyzerTheme.material.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                state.guidanceText?.let { hint ->
                    Text(
                        text = "Guidance: $hint",
                        style = MaterialTheme.typography.bodySmall,
                        color = SimAnalyzerTheme.extended.amber,
                    )
                }
            }
        }

        CalibrationSectionCard(
            title = "Build preview",
            subtitle = "Live track map while recording.",
        ) {
            TrackMapPreview(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                showStatus = false,
            )
        }

        CalibrationSectionCard(
            title = "Actions",
            subtitle = "Start/stop recording and save the map.",
        ) {
            val canSave = !state.recording && !state.isSaving && state.pointCount >= MIN_POINTS_TO_SAVE_UI

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart,
                    enabled = !state.recording && !state.isSaving,
                ) {
                    Text("Start")
                }

                Button(
                    onClick = onStop,
                    enabled = state.recording,
                ) {
                    Text("Stop")
                }

                OutlinedButton(
                    onClick = onReset,
                    enabled = !state.isSaving,
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = onSave,
                    enabled = canSave,
                ) {
                    Text(if (state.isSaving) "Saving..." else "Save")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPitEntry,
                    enabled = state.recording,
                ) {
                    Text("Mark pit entry")
                }
                OutlinedButton(
                    onClick = onPitExit,
                    enabled = state.recording,
                ) {
                    Text("Mark pit exit")
                }
            }

            if (state.pointCount < MIN_POINTS_TO_SAVE_UI) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Need at least $MIN_POINTS_TO_SAVE_UI points to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                MessageBanner(message)
            }
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
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StatusPill(label: String, accent: androidx.compose.ui.graphics.Color) {
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
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
            style = MaterialTheme.typography.bodySmall,
            color = accent,
        )
    }
}

@Preview
@Composable
private fun TrackMapBuilderPreview() {
    SimAnalyzerTheme {
        TrackMapBuilderContent(
            state = TrackMapRecorderState(
                gameLabel = "Assetto Corsa",
                trackId = "spa",
                trackName = "Spa-Francorchamps",
                pointCount = 1200,
                totalDistanceMeters = 7120.5f,
            ),
            onStart = {},
            onStop = {},
            onReset = {},
            onSave = {},
            onReferencePoint = {},
            onFallbackHalfWidth = {},
            onPitEntry = {},
            onPitExit = {},
        )
    }
}
