package com.project.analyzer.calibration.presentation.verify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.verify.state.EditingGate
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.abs

@Composable
fun GateDebugSection(
    gates: List<GateDebugInfo>,
    editingGate: EditingGate?,
    halfWidthMeters: Float,
    isCapturing: Boolean,
    onEditGate: (EditingGate) -> Unit,
    onCaptureGate: () -> Unit,
    onCancelEdit: () -> Unit,
    onFlipGate: (EditingGate) -> Unit,
    onRadius: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        gates.forEachIndexed { index, gate ->
            val correspondingEditGate = when (index) {
                0 -> EditingGate.START_FINISH
                1 -> EditingGate.SECTOR_1_FINISH
                2 -> EditingGate.SECTOR_2_FINISH
                else -> null
            }

            GateInfoRow(
                gate = gate,
                isEditing = editingGate == correspondingEditGate,
                isCapturing = isCapturing && editingGate == correspondingEditGate,
                onEdit = correspondingEditGate?.let { { onEditGate(it) } },
                halfWidthMeters = halfWidthMeters,
                onCapture = onCaptureGate,
                onCancel = onCancelEdit,
                onFlip = { correspondingEditGate?.let { onFlipGate(it) } },
                onRadius = onRadius,
            )
        }
    }
}

@Composable
private fun GateInfoRow(
    gate: GateDebugInfo,
    isEditing: Boolean,
    isCapturing: Boolean,
    halfWidthMeters: Float,
    onEdit: (() -> Unit)?,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    onFlip: () -> Unit,
    onRadius: (Float) -> Unit,
) {
    val shape = SimAnalyzerTheme.shapes.medium
    val containerColor = when {
        isEditing -> SimAnalyzerTheme.material.primary.copy(alpha = 0.12f)
        gate.distanceMeters < 10f -> SimAnalyzerTheme.extended.amber.copy(alpha = 0.16f)
        else -> SimAnalyzerTheme.material.surfaceVariant.copy(alpha = 0.22f)
    }
    val borderColor = SimAnalyzerTheme.material.outlineVariant.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GateHeader(gate = gate)
            GateActions(
                gate = gate,
                isEditing = isEditing,
                isCapturing = isCapturing,
                halfWidthMeters = halfWidthMeters,
                onEdit = onEdit,
                onCapture = onCapture,
                onCancel = onCancel,
                onFlip = onFlip,
                onRadius = onRadius,
            )
        }

        GateDirectionInfo(gate = gate)
        GateCrossedStatus(gate = gate)
    }
}

@Composable
private fun GateHeader(gate: GateDebugInfo) {
    val statusColor = if (gate.isCrossed) SimAnalyzerTheme.extended.teal else SimAnalyzerTheme.extended.red
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier = Modifier
                .size(10.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(statusColor),
        )

        Column {
            Text(
                gate.name,
                style = MaterialTheme.typography.bodyLarge,
                color = SimAnalyzerTheme.material.onSurface,
            )
            Text(
                "Distance: ${"%.1f".format(gate.distanceMeters)}m",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    gate.distanceMeters < 5f -> SimAnalyzerTheme.extended.teal
                    gate.distanceMeters < 15f -> SimAnalyzerTheme.extended.amber
                    else -> SimAnalyzerTheme.material.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun GateActions(
    gate: GateDebugInfo,
    isEditing: Boolean,
    isCapturing: Boolean,
    halfWidthMeters: Float,
    onEdit: (() -> Unit)?,
    onCapture: () -> Unit,
    onCancel: () -> Unit,
    onFlip: () -> Unit,
    onRadius: (Float) -> Unit,
) {
    val showFlipButton = gate.directionDot != null && gate.directionDot < -0.3f

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showFlipButton && !isEditing) {
            OutlinedButton(onClick = onFlip) {
                Text("Flip")
            }
        }

        if (isEditing) {
            Button(
                onClick = onCapture,
                enabled = !isCapturing,
            ) {
                Text(if (isCapturing) "Capturing..." else "Capture")
            }
            OutlinedTextField(
                value = halfWidthMeters.toString(),
                onValueChange = { onRadius(it.toFloatOrNull() ?: halfWidthMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(86.dp),
            )
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        } else if (onEdit != null) {
            OutlinedButton(onClick = onEdit) {
                Text("Edit")
            }
        }
    }
}

@Composable
private fun GateDirectionInfo(gate: GateDebugInfo) {
    val directionStatus = gate.directionDot?.let { dot ->
        when {
            dot > 0.5f -> "Correct direction" to SimAnalyzerTheme.extended.teal
            dot < -0.5f -> "Wrong direction" to SimAnalyzerTheme.extended.red
            abs(dot) < 0.3f -> "Perpendicular" to SimAnalyzerTheme.extended.amber
            else -> "Angled" to SimAnalyzerTheme.extended.yellow
        }
    }

    if (gate.gateForward != null && gate.directionDot != null) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Gate fwd: (${"%.2f".format(gate.gateForward.x)}, ${"%.2f".format(gate.gateForward.y)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
                Text(
                    "Dot: ${"%.2f".format(gate.directionDot)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                "isInside: ${gate.isInside}; margin: ${
                    "%.2f".format(
                        gate.margin,
                    )
                }, dParallel: ${"%.2f".format(gate.dParallel)}",
                style = MaterialTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )

            gate.signedDistanceFromPlane?.let { signedDist ->
                val positionText = when {
                    signedDist > 1f -> "${"%.1f".format(signedDist)}m ahead of gate"
                    signedDist < -1f -> "${"%.1f".format(-signedDist)}m behind gate"
                    else -> "At gate line"
                }
                Text(
                    positionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            directionStatus?.let { (text, color) ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun GateCrossedStatus(gate: GateDebugInfo) {
    if (gate.isCrossed && gate.lastCrossedTimeMs != null) {
        Text(
            "Crossed",
            style = MaterialTheme.typography.bodySmall,
            color = SimAnalyzerTheme.extended.teal,
        )
    }
}
