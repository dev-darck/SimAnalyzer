@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.project.analyzer.calibration.presentation.verify

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.CalibrationSectionCard
import com.project.analyzer.calibration.presentation.components.copyToClipboard
import com.project.analyzer.calibration.presentation.components.formatMs
import com.project.analyzer.calibration.presentation.verify.components.DirectionInfoCard
import com.project.analyzer.calibration.presentation.verify.components.GateDebugSection
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun CalibrationVerifyScreen(trackId: String, onBack: () -> Unit) {
    val viewModel: CalibrationVerifyViewModel = metroViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(trackId) {
        viewModel.start(trackId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalibrationSectionCard(
            title = "Verify calibration",
            subtitle = state.calibration?.trackName ?: trackId
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack) { Text("Back") }
                Button(onClick = { if (state.isRunning) viewModel.stop() else viewModel.start(trackId) }) {
                    Text(if (state.isRunning) "Stop" else "Start")
                }
                OutlinedButton(onClick = { viewModel.resetSession() }) { Text("Reset session") }
            }

            Spacer(modifier = Modifier.height(10.dp))

            StatusRow(label = "Track ID", value = state.calibration?.trackId ?: trackId)
            StatusRow(label = "Status", value = if (state.isRunning) "Running" else "Stopped")
            StatusRow(label = "Lap", value = state.lapIndex.toString())
            StatusRow(label = "Sector", value = state.currentSectorIndex.toString())
            StatusRow(label = "Speed", value = "%.1f km/h".format(state.speedKmh))

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                MessageBanner(message)
            }
        }

        CalibrationSectionCard(
            title = "Lap timing",
            subtitle = "Live lap and sector splits."
        ) {
            TimingRow("Lap", state.currentLapMs, state.lastLapMs, state.bestLapMs)
            TimingRow(
                "S1",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 1 },
                state.lastS1Ms,
                state.bestS1Ms
            )
            TimingRow(
                "S2",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 2 },
                state.lastS2Ms,
                state.bestS2Ms
            )
            TimingRow(
                "S3",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 3 },
                state.lastS3Ms,
                state.bestS3Ms
            )
        }

        CalibrationSectionCard(
            title = "Direction and gates",
            subtitle = "Verify alignment and crossings."
        ) {
            DirectionInfoCard(
                forward = state.currentForward,
                headingDegrees = state.headingDegrees
            )
            Spacer(modifier = Modifier.height(10.dp))
            GateDebugSection(
                gates = state.gateDebugInfo,
                editingGate = state.editingGate,
                isCapturing = state.isCapturing,
                halfWidthMeters = state.halfWidthMeters,
                onEditGate = viewModel::startEditingGate,
                onCaptureGate = viewModel::captureCurrentGate,
                onCancelEdit = viewModel::cancelEditing,
                onFlipGate = viewModel::flipGateDirection,
                onRadius = viewModel::onRadiusChanged,
            )
        }

        state.debugTelemetry?.let { debugText ->
            CalibrationSectionCard(
                title = "Telemetry snapshot",
                subtitle = "Click to copy the raw payload."
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SimAnalyzerTheme.shapes.medium)
                        .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f))
                        .border(
                            width = 1.dp,
                            color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f),
                            shape = SimAnalyzerTheme.shapes.medium
                        )
                        .onClick { copyToClipboard(debugText) }
                        .padding(12.dp)
                ) {
                    Text(
                        text = debugText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SimAnalyzerTheme.material.onSurface
                    )
                }
            }
        }

        CalibrationSectionCard(
            title = "Recent events",
            subtitle = "Latest gate crossings and sync changes."
        ) {
            Text(
                text = "Last event: ${state.lastEvent ?: "-"}",
                style = MaterialTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.events.isEmpty()) {
                Text(
                    text = "No events yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 220.dp)
                        .clip(SimAnalyzerTheme.shapes.medium)
                        .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.events.forEach { event ->
                        Text(
                            text = event,
                            style = MaterialTheme.typography.bodySmall,
                            color = SimAnalyzerTheme.material.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimingRow(name: String, current: Long?, last: Long?, best: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = SimAnalyzerTheme.material.onSurface,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimingCell("Cur", current)
            TimingCell("Last", last)
            TimingCell("Best", best)
        }
    }
}

@Composable
private fun TimingCell(label: String, value: Long?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant
        )
        Text(
            text = formatMs(value),
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageBanner(message: String) {
    val lower = message.lowercase()
    val isError = lower.contains("error") || lower.contains("not ready") || lower.contains("missing")
    val accent = if (isError) SimAnalyzerTheme.material.error else SimAnalyzerTheme.material.onSurfaceVariant
    val background = if (isError) {
        SimAnalyzerTheme.material.error.copy(alpha = 0.12f)
    } else {
        SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(background)
            .border(1.dp, accent.copy(alpha = 0.4f), SimAnalyzerTheme.shapes.medium)
            .padding(10.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = accent
        )
    }
}
