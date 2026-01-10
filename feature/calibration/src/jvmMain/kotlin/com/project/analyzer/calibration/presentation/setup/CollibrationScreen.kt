package com.project.analyzer.calibration.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.chooseSaveDirectory
import com.project.analyzer.calibration.presentation.components.ActionsBlock
import com.project.analyzer.calibration.presentation.components.GateRow
import com.project.analyzer.calibration.presentation.components.SectorsBlock
import com.project.analyzer.calibration.presentation.components.SettingsBlock
import com.project.analyzer.calibration.presentation.components.TrackNameBlock
import com.project.analyzer.calibration.presentation.setup.CalibrationIntent.SetFileToSave
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
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
    val window = LocalWindow.current
    var openChooser by remember { mutableStateOf(false) }

    LaunchedEffect(openChooser) {
        if (openChooser) {
            chooseSaveDirectory(window)?.let { file ->
                viewModel.dispatch(SetFileToSave(file.path))
            }
            openChooser = false
        }
    }

    CalibrationContent(
        state = state,
        onChooseDirectory = { openChooser = true },
        dispatchEvent = viewModel::dispatch,
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

        HorizontalDivider()

        TrackNameBlock(state) { dispatchEvent(CalibrationIntent.TrackNameChanged(it)) }

        SettingsBlock(
            state = state,
            onRp = { dispatchEvent(CalibrationIntent.ReferencePointChanged(it)) },
            onRadius = { dispatchEvent(CalibrationIntent.TriggerRadiusChanged(it)) },
        )

        state.debugTelemetry?.let { debugText ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = debugText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider()

        GateRow(
            title = "Start/Finish",
            gate = state.startFinish,
            enabled = !state.isBusy,
            onClick = { dispatchEvent(CalibrationIntent.CaptureStartFinish) },
            onFlip = { dispatchEvent(CalibrationIntent.FlipStartFinishDirection) }
        )

        HorizontalDivider()

        SectorsBlock(
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
                Text("Verify currently saved")
            }
        }

        state.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.size(20.dp))

        ChooseToVerify(
            state = state,
            dispatchEvent = dispatchEvent,
            onVerify = onVerify
        )

        Spacer(modifier = Modifier.weight(1f))
        Text("Path to save: ${state.savedPathHint}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChooseToVerify(
    state: CalibrationState,
    dispatchEvent: (CalibrationIntent) -> Unit,
    onVerify: (String) -> Unit = {}
) {
    val dispatch by rememberUpdatedState(dispatchEvent)
    HorizontalDivider()

    LaunchedEffect(Unit) {
        dispatch(CalibrationIntent.LoadAllFiles)
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (data in state.listOfData) {
            Button(onClick = { onVerify(data) }) {
                Text("Verify track id $data")
            }
        }
    }

    HorizontalDivider()
}

@Preview
@Composable
private fun CalibrationPreview() {
    CalibrationContent(
        state = CalibrationState(trackName = "Track 1", lastSavedTrackId = "track_1"),
        onChooseDirectory = {},
        dispatchEvent = {},
        onVerify = {}
    )
}
