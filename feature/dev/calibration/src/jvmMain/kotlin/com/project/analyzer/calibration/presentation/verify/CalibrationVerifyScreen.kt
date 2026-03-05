@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.calibration.presentation.verify

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.CalibrationSectionCard
import com.project.analyzer.calibration.presentation.components.copyToClipboard
import com.project.analyzer.calibration.presentation.components.formatMs
import com.project.analyzer.calibration.presentation.verify.components.DirectionInfoCard
import com.project.analyzer.calibration.presentation.verify.components.GateDebugSection
import com.project.analyzer.feature.dev.calibration.Res.Res
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_track_id
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_back
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_best
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_cur
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_direction_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_direction_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_label_suffix
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_lap
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_lap_timing_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_lap_timing_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_last
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_last_event
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_no_events
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_recent_events_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_recent_events_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_reset_session
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_running
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_sector
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_snapshot_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_snapshot_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_speed
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_start
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_status
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_stop
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_stopped
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_title
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CalibrationVerifyScreen(trackId: String, onBack: () -> Unit) {
    val viewModel: CalibrationVerifyViewModel = metroViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(trackId) {
        viewModel.start(trackId)
    }

    val screenScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .padding(16.dp)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_verify_title),
            subtitle = state.calibration?.trackName ?: trackId,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onBack) {
                    Text(
                        text = stringResource(Res.string.calibration_verify_back),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
                Button(onClick = { if (state.isRunning) viewModel.stop() else viewModel.start(trackId) }) {
                    Text(
                        text = stringResource(
                            if (state.isRunning) Res.string.calibration_verify_stop else Res.string.calibration_verify_start,
                        ),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
                OutlinedButton(onClick = { viewModel.resetSession() }) {
                    Text(
                        text = stringResource(Res.string.calibration_verify_reset_session),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            StatusRow(
                label = stringResource(Res.string.calibration_header_track_id),
                value = state.calibration?.trackId ?: trackId,
            )
            StatusRow(
                label = stringResource(Res.string.calibration_verify_status),
                value = stringResource(
                    if (state.isRunning) Res.string.calibration_verify_running else Res.string.calibration_verify_stopped,
                ),
            )
            StatusRow(label = stringResource(Res.string.calibration_verify_lap), value = state.lapIndex.toString())
            StatusRow(
                label = stringResource(Res.string.calibration_verify_sector),
                value = state.currentSectorIndex.toString(),
            )
            StatusRow(
                label = stringResource(Res.string.calibration_verify_speed),
                value = "%.1f km/h".format(state.speedKmh),
            )

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                MessageBanner(message)
            }
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_verify_lap_timing_title),
            subtitle = stringResource(Res.string.calibration_verify_lap_timing_subtitle),
        ) {
            TimingRow(
                stringResource(Res.string.calibration_verify_lap),
                state.currentLapMs,
                state.lastLapMs,
                state.bestLapMs,
            )
            TimingRow(
                "S1",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 1 },
                state.lastS1Ms,
                state.bestS1Ms,
            )
            TimingRow(
                "S2",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 2 },
                state.lastS2Ms,
                state.bestS2Ms,
            )
            TimingRow(
                "S3",
                state.currentSectorMs.takeIf { state.currentSectorIndex == 3 },
                state.lastS3Ms,
                state.bestS3Ms,
            )
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_verify_direction_title),
            subtitle = stringResource(Res.string.calibration_verify_direction_subtitle),
        ) {
            DirectionInfoCard(
                forward = state.currentForward,
                headingDegrees = state.headingDegrees,
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
                title = stringResource(Res.string.calibration_verify_snapshot_title),
                subtitle = stringResource(Res.string.calibration_verify_snapshot_subtitle),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SimAnalyzerTheme.shapes.medium)
                        .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.18f))
                        .border(
                            width = 1.dp,
                            color = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f),
                            shape = SimAnalyzerTheme.shapes.medium,
                        )
                        .onClick { copyToClipboard(debugText) }
                        .padding(12.dp),
                ) {
                    Text(
                        text = debugText,
                        style = SimAnalyzerTheme.typography.bodySmall,
                        color = SimAnalyzerTheme.material.onSurface,
                    )
                }
            }
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_verify_recent_events_title),
            subtitle = stringResource(Res.string.calibration_verify_recent_events_subtitle),
        ) {
            Text(
                text = stringResource(Res.string.calibration_verify_last_event, state.lastEvent ?: "-"),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (state.events.isEmpty()) {
                Text(
                    text = stringResource(Res.string.calibration_verify_no_events),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            } else {
                val eventsScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 220.dp)
                        .clip(SimAnalyzerTheme.shapes.medium)
                        .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.2f))
                        .padding(10.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(eventsScrollState)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        state.events.forEach { event ->
                            Text(
                                text = event,
                                style = SimAnalyzerTheme.typography.bodySmall,
                                color = SimAnalyzerTheme.material.onSurface,
                            )
                        }
                    }
                    AppVerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(eventsScrollState),
                    )
                }
            }
        }
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            adapter = rememberScrollbarAdapter(screenScrollState),
        )
    }
}

@Composable
private fun TimingRow(name: String, current: Long?, last: Long?, best: Long?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = SimAnalyzerTheme.typography.bodyMedium,
            color = SimAnalyzerTheme.material.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimingCell(stringResource(Res.string.calibration_verify_cur), current)
            TimingCell(stringResource(Res.string.calibration_verify_last), last)
            TimingCell(stringResource(Res.string.calibration_verify_best), best)
        }
    }
}

@Composable
private fun TimingCell(label: String, value: Long?) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.calibration_verify_label_suffix, label),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Text(
            text = formatMs(value),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface,
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
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
            .padding(10.dp),
    ) {
        Text(
            text = message,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = accent,
        )
    }
}
