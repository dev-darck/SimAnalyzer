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
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.ReferencePoint
import com.project.analyzer.calibration.presentation.CalibrationIntent
import com.project.analyzer.calibration.presentation.state.CalibrationState

@Composable
fun SectorSplitsBlock(
    state: CalibrationState,
    dispatch: (CalibrationIntent) -> Unit,
) {
    val split1: Gate? = state.sectorFinishes[1] ?: state.sectorStarts[2]

    val split2: Gate? = state.sectorFinishes[2] ?: state.sectorStarts[3]

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Sector lines (splits)", style = MaterialTheme.typography.titleMedium)

        GateRow(
            title = "Split1 (end S1 / start S2)",
            gate = split1,
            enabled = !state.isBusy && state.startFinish != null,
            onClick = { dispatch(CalibrationIntent.CaptureSplit1) }
        )

        GateRow(
            title = "Split2 (end S2 / start S3)",
            gate = split2,
            enabled = !state.isBusy && split1 != null,
            onClick = { dispatch(CalibrationIntent.CaptureSplit2) }
        )

        Divider()
        Text(
            "Derived sectors:\n" +
                "S1: SF -> Split1\n" +
                "S2: Split1 -> Split2\n" +
                "S3: Split2 -> SF",
            style = MaterialTheme.typography.bodySmall
        )

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
            enabled = !state.isBusy && state.isReadyToSave(3),
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
    onWidth: (Float) -> Unit,
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
                value = state.triggerRadiusMeters.toString(),
                onValueChange = { onRadius(it.toFloatOrNull() ?: state.triggerRadiusMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp)
            )
            Text("Close to the line", style = MaterialTheme.typography.bodySmall)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("HalfWidth:", modifier = Modifier.width(90.dp))
            OutlinedTextField(
                value = state.debugHalfWidthMeters.toString(),
                onValueChange = { onWidth(it.toFloatOrNull() ?: state.debugHalfWidthMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(140.dp)
            )
            Text("Only for visual", style = MaterialTheme.typography.bodySmall)
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
internal fun GateRow(
    title: String,
    gate: Gate?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(enabled = enabled, onClick = onClick) {
            Text("Capture $title")
        }

        if (gate == null) {
            Text("—", style = MaterialTheme.typography.bodySmall)
        } else {
            val c = gate.center
            val f = gate.forward
            Text(
                text = "center=(%.3f, %.3f) forward=(%.3f, %.3f) r=%.1f".format(
                    c.x,
                    c.y,
                    f.x,
                    f.y,
                    gate.triggerRadiusMeters
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
