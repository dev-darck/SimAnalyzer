@file:Suppress("WildcardImport", "NoWildcardImports")

package com.project.analyzer.calibration.presentation.setup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.ActionsBlock
import com.project.analyzer.calibration.presentation.components.CalibrationSectionCard
import com.project.analyzer.calibration.presentation.components.GateRow
import com.project.analyzer.calibration.presentation.components.SectorsBlock
import com.project.analyzer.calibration.presentation.components.SettingsBlock
import com.project.analyzer.calibration.presentation.components.TrackNameBlock
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.feature.dev.calibration.Res.Res
import com.project.analyzer.feature.dev.calibration.Res.calibration_actions_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_actions_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture_settings_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture_settings_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_capture_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_capture_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_car
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_detected_track
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_override_note
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_track_id
import com.project.analyzer.feature.dev.calibration.Res.calibration_live_telemetry_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_live_telemetry_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_no_saved_yet
import com.project.analyzer.feature.dev.calibration.Res.calibration_saved_calibrations_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_saved_calibrations_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_start_finish
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_capturing
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_not_ready
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_ready_to_save
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_sectors
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_start_finish_missing
import com.project.analyzer.feature.dev.calibration.Res.calibration_status_start_finish_ready
import com.project.analyzer.feature.dev.calibration.Res.calibration_track_identity_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_track_identity_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify
import com.project.analyzer.feature.dev.calibration.Res.calibration_verify_last_saved
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun CalibrationScreen(onVerify: (String) -> Unit = {}) {
    val viewModel = metroViewModel<CalibrationViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    CalibrationContent(
        state = state,
        dispatchEvent = viewModel::dispatch,
        onVerify = onVerify,
    )
}

@Composable
private fun CalibrationContent(
    state: CalibrationState,
    dispatchEvent: (CalibrationIntent) -> Unit,
    onVerify: (String) -> Unit,
) {
    val screenScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .padding(16.dp)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        CalibrationHeader(state)

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_track_identity_title),
            subtitle = stringResource(Res.string.calibration_track_identity_subtitle),
        ) {
            TrackNameBlock(state) { dispatchEvent(CalibrationIntent.TrackNameChanged(it)) }
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_capture_settings_title),
            subtitle = stringResource(Res.string.calibration_capture_settings_subtitle),
        ) {
            SettingsBlock(
                state = state,
                onRp = { dispatchEvent(CalibrationIntent.ReferencePointChanged(it)) },
                onRadius = { dispatchEvent(CalibrationIntent.TriggerRadiusChanged(it)) },
            )
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_gate_capture_title),
            subtitle = stringResource(Res.string.calibration_gate_capture_subtitle),
        ) {
            GateRow(
                title = stringResource(Res.string.calibration_start_finish),
                gate = state.startFinish,
                enabled = !state.isBusy,
                onClick = { dispatchEvent(CalibrationIntent.CaptureStartFinish) },
                showFlipAction = true,
                onFlip = { dispatchEvent(CalibrationIntent.FlipStartFinishDirection) },
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectorsBlock(
                state = state,
                dispatch = dispatchEvent,
            )
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_actions_title),
            subtitle = stringResource(Res.string.calibration_actions_subtitle),
        ) {
            ActionsBlock(
                state = state,
                onSave = { dispatchEvent(CalibrationIntent.Save) },
                onReset = { dispatchEvent(CalibrationIntent.Reset) },
            )

            if (state.canVerify) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { onVerify(state.lastSavedTrackId!!) }) {
                    Text(
                        text = stringResource(Res.string.calibration_verify_last_saved),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
            }

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                MessageBanner(message)
            }
        }

        CalibrationSectionCard(
            title = stringResource(Res.string.calibration_saved_calibrations_title),
            subtitle = stringResource(Res.string.calibration_saved_calibrations_subtitle),
        ) {
            ChooseToVerify(
                state = state,
                dispatchEvent = dispatchEvent,
                onVerify = onVerify,
            )
        }

        state.debugTelemetry?.let { debugText ->
            CalibrationSectionCard(
                title = stringResource(Res.string.calibration_live_telemetry_title),
                subtitle = stringResource(Res.string.calibration_live_telemetry_subtitle),
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
private fun ChooseToVerify(
    state: CalibrationState,
    dispatchEvent: (CalibrationIntent) -> Unit,
    onVerify: (String) -> Unit = {},
) {
    val dispatch by rememberUpdatedState(dispatchEvent)

    LaunchedEffect(Unit) {
        dispatch(CalibrationIntent.LoadAllCalibrations)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.listOfData.isEmpty()) {
            Text(
                stringResource(Res.string.calibration_no_saved_yet),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        } else {
            state.listOfData.forEach { trackId ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SimAnalyzerTheme.shapes.medium)
                        .background(SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = trackId,
                        style = SimAnalyzerTheme.typography.bodyMedium,
                        color = SimAnalyzerTheme.material.onSurface,
                    )
                    OutlinedButton(onClick = { onVerify(trackId) }) {
                        Text(
                            text = stringResource(Res.string.calibration_verify),
                            style = SimAnalyzerTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalibrationHeader(state: CalibrationState) {
    val detectedTrack = listOfNotNull(state.sessionTrackName, state.sessionTrackId)
        .joinToString(" / ")
        .trim()

    CalibrationSectionCard(
        title = stringResource(Res.string.calibration_header_title),
        subtitle = stringResource(Res.string.calibration_header_subtitle),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderRow(
                label = stringResource(Res.string.calibration_header_detected_track),
                value = detectedTrack.ifBlank { "-" },
            )
            HeaderRow(
                label = stringResource(Res.string.calibration_header_track_id),
                value = state.trackId.ifBlank { "-" },
            )
            HeaderRow(
                label = stringResource(Res.string.calibration_header_car),
                value = state.sessionCarModel?.ifBlank { "-" } ?: "-",
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(Res.string.calibration_header_override_note),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                label = stringResource(
                    if (state.startFinish != null) {
                        Res.string.calibration_status_start_finish_ready
                    } else {
                        Res.string.calibration_status_start_finish_missing
                    },
                ),
                accent = if (state.startFinish !=
                    null
                ) {
                    SimAnalyzerTheme.extended.teal
                } else {
                    SimAnalyzerTheme.material.onSurfaceVariant
                },
            )
            StatusPill(
                label = stringResource(Res.string.calibration_status_sectors, state.sectorCount),
                accent = SimAnalyzerTheme.material.primary,
            )
            StatusPill(
                label = stringResource(
                    if (state.isReadyToSave()) {
                        Res.string.calibration_status_ready_to_save
                    } else {
                        Res.string.calibration_status_not_ready
                    },
                ),
                accent = if (state.isReadyToSave()) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.extended.amber,
            )
            if (state.isBusy) {
                StatusPill(
                    label = stringResource(Res.string.calibration_status_capturing),
                    accent = SimAnalyzerTheme.extended.orange,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(label: String, value: String) {
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
private fun StatusPill(label: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(accent.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.4f),
                shape = SimAnalyzerTheme.corners.pill,
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

@Preview
@Composable
private fun CalibrationPreview() {
    SimAnalyzerTheme {
        CalibrationContent(
            state = CalibrationState(trackName = "Track 1", lastSavedTrackId = "track_1"),
            dispatchEvent = {},
            onVerify = {},
        )
    }
}
