package com.project.analyzer.calibration.presentation.setup

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.project.analyzer.theme.SimAnalyzerTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SimAnalyzerTheme.material.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalibrationHeader(state)

        CalibrationSectionCard(
            title = "Track identity",
            subtitle = "Match the active session to a stable trackId.",
        ) {
            TrackNameBlock(state) { dispatchEvent(CalibrationIntent.TrackNameChanged(it)) }
        }

        CalibrationSectionCard(
            title = "Capture settings",
            subtitle = "Choose the reference point and capture radius.",
        ) {
            SettingsBlock(
                state = state,
                onRp = { dispatchEvent(CalibrationIntent.ReferencePointChanged(it)) },
                onRadius = { dispatchEvent(CalibrationIntent.TriggerRadiusChanged(it)) },
            )
        }

        CalibrationSectionCard(
            title = "Gate capture",
            subtitle = "Capture Start/Finish, then add extra sector starts.",
        ) {
            GateRow(
                title = "Start/Finish",
                gate = state.startFinish,
                enabled = !state.isBusy,
                onClick = { dispatchEvent(CalibrationIntent.CaptureStartFinish) },
                onFlip = { dispatchEvent(CalibrationIntent.FlipStartFinishDirection) },
            )
            Spacer(modifier = Modifier.height(10.dp))
            SectorsBlock(
                state = state,
                dispatch = dispatchEvent,
            )
        }

        CalibrationSectionCard(
            title = "Actions",
            subtitle = "Save this calibration or reset to start over.",
        ) {
            ActionsBlock(
                state = state,
                onSave = { dispatchEvent(CalibrationIntent.Save) },
                onReset = { dispatchEvent(CalibrationIntent.Reset) },
            )

            if (state.canVerify) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { onVerify(state.lastSavedTrackId!!) }) {
                    Text("Verify last saved")
                }
            }

            state.message?.let { message ->
                Spacer(modifier = Modifier.height(10.dp))
                MessageBanner(message)
            }
        }

        CalibrationSectionCard(
            title = "Saved calibrations",
            subtitle = "Verify previously captured trackIds.",
        ) {
            ChooseToVerify(
                state = state,
                dispatchEvent = dispatchEvent,
                onVerify = onVerify,
            )
        }

        state.debugTelemetry?.let { debugText ->
            CalibrationSectionCard(
                title = "Live telemetry",
                subtitle = "Raw snapshot from the current frame.",
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
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = SimAnalyzerTheme.material.onSurface,
                    )
                }
            }
        }
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
                "No saved calibrations yet.",
                style = MaterialTheme.typography.bodySmall,
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
                        style = MaterialTheme.typography.bodyMedium,
                        color = SimAnalyzerTheme.material.onSurface,
                    )
                    OutlinedButton(onClick = { onVerify(trackId) }) {
                        Text("Verify")
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
        title = "Track calibration",
        subtitle = "Capture Start/Finish and extra sectors for telemetry splits.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderRow(label = "Detected track", value = detectedTrack.ifBlank { "-" })
            HeaderRow(label = "Track ID", value = state.trackId.ifBlank { "-" })
            HeaderRow(label = "Car", value = state.sessionCarModel?.ifBlank { "-" } ?: "-")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Saved calibrations override bundled defaults.",
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill(
                label = if (state.startFinish != null) "Start/Finish ready" else "Start/Finish missing",
                accent = if (state.startFinish !=
                    null
                ) {
                    SimAnalyzerTheme.extended.teal
                } else {
                    SimAnalyzerTheme.material.onSurfaceVariant
                },
            )
            StatusPill(
                label = "Sectors: ${state.sectorCount}",
                accent = SimAnalyzerTheme.material.primary,
            )
            StatusPill(
                label = if (state.isReadyToSave()) "Ready to save" else "Not ready",
                accent = if (state.isReadyToSave()) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.extended.amber,
            )
            if (state.isBusy) {
                StatusPill(
                    label = "Capturing",
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
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.18f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.4f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
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
            style = MaterialTheme.typography.bodySmall,
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
