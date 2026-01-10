@file:OptIn(ExperimentalFoundationApi::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package com.project.analyzer.calibration.presentation.verify

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.project.analyzer.calibration.presentation.components.copyToClipboard
import com.project.analyzer.calibration.presentation.components.formatMs
import com.project.analyzer.calibration.presentation.verify.components.DirectionInfoCard
import com.project.analyzer.calibration.presentation.verify.components.GateDebugSection
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Verify: ${state.calibration?.trackName ?: trackId}", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("Back") }
            Button(onClick = { if (state.isRunning) viewModel.stop() else viewModel.start(trackId) }) {
                Text(if (state.isRunning) "Stop" else "Start")
            }
            OutlinedButton(onClick = { viewModel.resetSession() }) { Text("Reset Session") }
        }

        state.message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        HorizontalDivider()

        TimingInfo("Lap", state.currentLapMs, state.lastLapMs, state.bestLapMs)
        TimingInfo("S1", state.currentSectorMs.takeIf { state.currentSectorIndex == 1 }, state.lastS1Ms, state.bestS1Ms)
        TimingInfo("S2", state.currentSectorMs.takeIf { state.currentSectorIndex == 2 }, state.lastS2Ms, state.bestS2Ms)
        TimingInfo("S3", state.currentSectorMs.takeIf { state.currentSectorIndex == 3 }, state.lastS3Ms, state.bestS3Ms)

        HorizontalDivider()

        DirectionInfoCard(
            forward = state.currentForward,
            headingDegrees = state.headingDegrees
        )

        Text("Gate Distances:", style = MaterialTheme.typography.titleMedium)
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

        HorizontalDivider()

        state.debugTelemetry?.let { debugText ->
            Card(
                modifier = Modifier
                    .onClick {
                        copyToClipboard(debugText)
                    }
                    .fillMaxWidth(),
            ) {
                Text(
                    text = debugText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Text("Last event: ${state.lastEvent ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.events) {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider()
    }
}

@Composable
private fun TimingInfo(name: String, current: Long?, last: Long?, best: Long?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$name:", style = MaterialTheme.typography.bodyLarge)
        Text("Cur: ${formatMs(current)}", style = MaterialTheme.typography.bodyLarge)
        Text("Last: ${formatMs(last)}", style = MaterialTheme.typography.bodyLarge)
        Text("Best: ${formatMs(best)}", style = MaterialTheme.typography.bodyLarge)
    }
}
