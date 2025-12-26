package com.project.analyzer.calibration.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.util.concurrent.TimeUnit

@Composable
fun CalibrationVerifyScreen(trackId: String, onBack: () -> Unit) {
    val viewModel: CalibrationVerifyViewModel = metroViewModel()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(trackId) {
        viewModel.start(trackId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Divider()

        // Live Timing
        TimingInfo("Lap", state.currentLapMs, state.lastLapMs, state.bestLapMs)
        TimingInfo("S1", state.currentSectorMs.takeIf { state.currentSectorIndex == 1 }, state.lastS1Ms, state.bestS1Ms)
        TimingInfo("S2", state.currentSectorMs.takeIf { state.currentSectorIndex == 2 }, state.lastS2Ms, state.bestS2Ms)
        TimingInfo("S3", state.currentSectorMs.takeIf { state.currentSectorIndex == 3 }, state.lastS3Ms, state.bestS3Ms)

        Divider()

        // Events
        Text("Last event: ${state.lastEvent ?: "-"}", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.events) {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }
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

private fun formatMs(ms: Long?): String {
    if (ms == null) return "--:--.--- 	"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    val millis = ms % 1000
    return String.format("%02d:%02d.%03d", minutes, seconds, millis)
}
