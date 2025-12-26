package com.project.analyzer.calibration.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.CalibrationIntent.SetFileToSave
import com.project.analyzer.calibration.presentation.components.ActionsBlock
import com.project.analyzer.calibration.presentation.components.GateRow
import com.project.analyzer.calibration.presentation.components.SectorSplitsBlock
import com.project.analyzer.calibration.presentation.components.SettingsBlock
import com.project.analyzer.calibration.presentation.components.TrackNameBlock
import com.project.analyzer.calibration.presentation.state.CalibrationState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.awt.Window

val LocalWindow: ProvidableCompositionLocal<Window> =
    staticCompositionLocalOf {
        error("No Window provided")
    }

@Composable
fun CalibrationScreen(onVerify: (String) -> Unit = {}) {
    val viewModel = metroViewModel<CalibrationViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Calibration(
        state = state,
        dispatchEvent = viewModel::dispatch,
        onVerify = onVerify
    )
}

@Composable
private fun Calibration(
    state: CalibrationState,
    dispatchEvent: (CalibrationIntent) -> Unit = {},
    onVerify: (String) -> Unit = {}
) {
    val window = LocalWindow.current
    var openChooser by remember { mutableStateOf(false) }

    LaunchedEffect(openChooser) {
        if (openChooser) {
            chooseSaveDirectory(window)?.let { file ->
                dispatchEvent(SetFileToSave(file.path))
            }
            openChooser = false
        }
    }

    CalibrationContent(
        state = state,
        onChooseDirectory = { openChooser = true },
        dispatchEvent = dispatchEvent,
        onVerify = onVerify
    )
}

@Composable
private fun CalibrationContent(
    state: CalibrationState,
    onChooseDirectory: () -> Unit,
    dispatchEvent: (CalibrationIntent) -> Unit,
    onVerify: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Track calibration", style = MaterialTheme.typography.headlineSmall)

        Button(onClick = onChooseDirectory) {
            Text("Choose directory to save")
        }

        Divider()

        TrackNameBlock(state) { dispatchEvent(CalibrationIntent.TrackNameChanged(it)) }

        SettingsBlock(
            state = state,
            onRp = { dispatchEvent(CalibrationIntent.ReferencePointChanged(it)) },
            onRadius = { dispatchEvent(CalibrationIntent.TriggerRadiusChanged(it)) },
            onWidth = { dispatchEvent(CalibrationIntent.DebugWidthChanged(it)) },
        )

        Divider()

        GateRow(
            title = "Start/Finish",
            gate = state.startFinish,
            enabled = !state.isBusy && state.trackId.isNotBlank(),
            onClick = { dispatchEvent(CalibrationIntent.CaptureStartFinish) }
        )

        Divider()

        SectorSplitsBlock(
            state = state,
            dispatch = dispatchEvent,
        )

        ActionsBlock(
            state = state,
            onSave = { dispatchEvent(CalibrationIntent.Save) },
            onReset = { dispatchEvent(CalibrationIntent.Reset) }
        )

        if (state.canVerify) {
            Button(onClick = { onVerify(state.lastSavedTrackId!!) }) {
                Text("Verify")
            }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))
        Text("Path to save: ${state.savedPathHint}", style = MaterialTheme.typography.bodySmall)
    }
}

@Preview
@Composable
private fun CalibrationPreview() {
    Calibration(
        state = CalibrationState(trackName = "Track 1", lastSavedTrackId = "track_1")
    )
}
