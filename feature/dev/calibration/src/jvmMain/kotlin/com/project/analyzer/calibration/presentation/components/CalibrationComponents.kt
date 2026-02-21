package com.project.analyzer.calibration.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.setup.CalibrationIntent
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun CalibrationSectionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.large)
            .background(SimAnalyzerTheme.material.surface)
            .padding(16.dp),
    ) {
        Text(
            text = title,
            color = SimAnalyzerTheme.material.onSurface,
            style = MaterialTheme.typography.titleMedium,
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun SectorsBlock(state: CalibrationState, dispatch: (CalibrationIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Extra sectors",
                    style = MaterialTheme.typography.titleSmall,
                    color = SimAnalyzerTheme.material.onSurface,
                )
                Text(
                    text = "Sector 1 uses Start/Finish. Each sector ends at the next gate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            Button(
                enabled = !state.isBusy,
                onClick = { dispatch(CalibrationIntent.AddSector) },
            ) { Text("Add sector") }
        }

        if (state.sectorCount <= 1) {
            Text(
                text = "No extra sectors yet.",
                style = MaterialTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        } else {
            for (i in 2..state.sectorCount) {
                val finishLabel = if (i == state.sectorCount) "Start/Finish" else "S${i + 1} start"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Sector S$i", style = MaterialTheme.typography.bodyLarge)
                    IconButton(
                        enabled = !state.isBusy,
                        onClick = { dispatch(CalibrationIntent.RemoveSector(i)) },
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove sector S$i")
                    }
                }

                GateRow(
                    title = "S$i start",
                    gate = state.sectorStart(i),
                    enabled = !state.isBusy,
                    onClick = { dispatch(CalibrationIntent.CaptureSectorStart(i)) },
                    onFlip = { dispatch(CalibrationIntent.FlipSectorStartDirection(i)) },
                )

                Text(
                    text = "Finish: $finishLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ActionsBlock(state: CalibrationState, onSave: () -> Unit, onReset: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            enabled = !state.isBusy && state.isReadyToSave(),
            onClick = onSave,
        ) { Text("Save calibration") }

        OutlinedButton(
            enabled = !state.isBusy,
            onClick = onReset,
        ) { Text("Reset") }
    }
}

@Composable
internal fun TrackNameBlock(state: CalibrationState, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.trackName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Track name") },
            singleLine = true,
        )

        val detectedTrack = listOfNotNull(state.sessionTrackName, state.sessionTrackId)
            .joinToString(" / ")
            .trim()

        if (detectedTrack.isNotBlank()) {
            InfoRow(label = "Detected", value = detectedTrack)
        }
        InfoRow(label = "Track ID", value = state.trackId.ifBlank { "-" })
        state.sessionCarModel?.takeIf { it.isNotBlank() }?.let {
            InfoRow(label = "Car", value = it)
        }
    }
}

@Composable
internal fun SettingsBlock(state: CalibrationState, onRp: (ReferencePoint) -> Unit, onRadius: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Reference point",
                style = MaterialTheme.typography.bodyMedium,
                color = SimAnalyzerTheme.material.onSurface,
            )
            ReferencePointDropdown(selected = state.referencePoint, onSelected = onRp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Capture radius (m)",
                style = MaterialTheme.typography.bodyMedium,
                color = SimAnalyzerTheme.material.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = state.halfWidthMeters.toString(),
                    onValueChange = { onRadius(it.toFloatOrNull() ?: state.halfWidthMeters) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(140.dp),
                )
                Text(
                    text = "Keep the car centered on the line when capturing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun ReferencePointDropdown(selected: ReferencePoint, onSelected: (ReferencePoint) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(
                text = selected.displayName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Select reference point",
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReferencePoint.entries.forEach { rp ->
                DropdownMenuItem(
                    text = { Text(rp.displayName()) },
                    onClick = {
                        expanded = false
                        onSelected(rp)
                    },
                )
            }
        }
    }
}

@Composable
fun GateRow(title: String, gate: Gate?, enabled: Boolean, onClick: () -> Unit, onFlip: (() -> Unit)? = null) {
    val shape = SimAnalyzerTheme.shapes.medium
    val background = SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f)
    val border = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (gate != null) {
                val fwd = gate.forwardV2()
                val angle = Math.toDegrees(kotlin.math.atan2(fwd.x.toDouble(), fwd.y.toDouble()))
                Text(
                    "pos=(%.1f, %.1f) fwd=%s deg".format(gate.center.x, gate.center.y, "%.0f".format(angle)),
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            } else {
                Text(
                    "Not captured",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (gate != null && onFlip != null) {
                OutlinedButton(onClick = onFlip) {
                    Text("Flip")
                }
            }
            Button(onClick = onClick, enabled = enabled) {
                Text(if (gate == null) "Capture" else "Recapture")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
        )
    }
}

internal fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit()
        .systemClipboard
        .setContents(StringSelection(text), null)
}

private fun ReferencePoint.displayName(): String {
    val spaced = name.lowercase().replace('_', ' ')
    return spaced.replaceFirstChar { it.uppercase() }
}
