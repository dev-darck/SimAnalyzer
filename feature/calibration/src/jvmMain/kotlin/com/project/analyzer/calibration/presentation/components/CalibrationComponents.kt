package com.project.analyzer.calibration.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.setup.CalibrationIntent
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun SectorsBlock(
    state: CalibrationState,
    dispatch: (CalibrationIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sectors", style = MaterialTheme.typography.titleMedium)

            Button(
                enabled = !state.isBusy,
                onClick = { dispatch(CalibrationIntent.AddSector) }
            ) { Text("＋ Add sector") }
        }

        if (state.sectorCount == 0) {
            Text("No sectors yet. Press ‘Add sector’.", style = MaterialTheme.typography.bodySmall)
        } else {
            for (i in 1..state.sectorCount) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sector S$i", style = MaterialTheme.typography.bodyLarge)

                    if (i >= 2) {
                        IconButton(
                            enabled = !state.isBusy,
                            onClick = { dispatch(CalibrationIntent.RemoveSector(i)) }
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove sector S$i")
                        }
                    }
                }

                GateRow(
                    title = "S$i Start",
                    gate = state.sectorStart(i),
                    enabled = !state.isBusy && i >= 2,
                    onClick = { if (i >= 2) dispatch(CalibrationIntent.CaptureSectorStart(i)) },
                    onFlip = { if (i >= 2) dispatch(CalibrationIntent.FlipSectorStartDirection(i)) }
                )

                GateRow(
                    title = "S$i Finish (auto)",
                    gate = state.sectorFinish(i),
                    enabled = false,
                    onClick = {},
                    onFlip = {}
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
internal fun ActionsBlock(
    state: CalibrationState,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = !state.isBusy && state.isReadyToSave(),
            onClick = onSave
        ) { Text("Save") }

        OutlinedButton(
            enabled = !state.isBusy,
            onClick = onReset
        ) { Text("Reset") }
    }
}

@Composable
internal fun TrackNameBlock(state: CalibrationState, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = state.trackName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Track name") },
            singleLine = true
        )
        Text("trackId: ${state.trackId.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun SettingsBlock(
    state: CalibrationState,
    onRp: (ReferencePoint) -> Unit,
    onRadius: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Reference:", modifier = Modifier.width(90.dp))
            ReferencePointDropdown(selected = state.referencePoint, onSelected = onRp)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Radius (m):", modifier = Modifier.width(90.dp))
            OutlinedTextField(
                value = state.halfWidthMeters.toString(),
                onValueChange = { onRadius(it.toFloatOrNull() ?: state.halfWidthMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp)
            )
            Text("Close to the line", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun ReferencePointDropdown(selected: ReferencePoint, onSelected: (ReferencePoint) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selected.name) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReferencePoint.entries.forEach { rp ->
                DropdownMenuItem(
                    text = { Text(rp.name) },
                    onClick = {
                        expanded = false
                        onSelected(rp)
                    }
                )
            }
        }
    }
}

@Composable
fun GateRow(
    title: String,
    gate: Gate?,
    enabled: Boolean,
    onClick: () -> Unit,
    onFlip: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (gate != null) {
                val fwd = gate.forwardV2()
                val angle = Math.toDegrees(kotlin.math.atan2(fwd.x.toDouble(), fwd.y.toDouble()))
                Text(
                    "pos=(%.1f, %.1f) fwd=${"%.0f".format(angle)}°".format(
                        gate.center.x, gate.center.y
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("Not captured", style = MaterialTheme.typography.bodySmall)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (gate != null && onFlip != null) {
                OutlinedButton(onClick = onFlip) {
                    Text("Flip ↻")
                }
            }
            Button(onClick = onClick, enabled = enabled) {
                Text(if (gate == null) "Capture" else "Recapture")
            }
        }
    }
}

internal fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit()
        .systemClipboard
        .setContents(StringSelection(text), null)
}
