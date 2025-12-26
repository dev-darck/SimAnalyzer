package com.project.analyzer.calibration.presentation.verify.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.verify.state.EditingGate
import com.project.analyzer.calibration.presentation.verify.state.GateDebugInfo
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
                onRadius = onRadius
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = when {
            isEditing -> MaterialTheme.colorScheme.primaryContainer
            gate.distanceMeters < 10f -> MaterialTheme.colorScheme.tertiaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                    onRadius = onRadius
                )
            }

            GateDirectionInfo(gate = gate)
            GateCrossedStatus(gate = gate)
        }
    }
}

@Composable
private fun GateHeader(gate: GateDebugInfo) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (gate.isCrossed) Color.Green else Color.Red)
        )

        Column {
            Text(gate.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "Distance: ${"%.1f".format(gate.distanceMeters)}m",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    gate.distanceMeters < 5f -> Color.Green
                    gate.distanceMeters < 15f -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
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
    onRadius: (Float) -> Unit
) {
    val showFlipButton = gate.directionDot != null && gate.directionDot < -0.3f

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showFlipButton && !isEditing) {
            Button(
                onClick = onFlip,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Red.copy(alpha = 0.8f)
                )
            ) {
                Text("Flip ↻", color = Color.White)
            }
        }

        if (isEditing) {
            Button(
                onClick = onCapture,
                enabled = !isCapturing,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isCapturing) "Capturing..." else "Capture")
            }
            OutlinedTextField(
                value = halfWidthMeters.toString(),
                onValueChange = { onRadius(it.toFloatOrNull() ?: halfWidthMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
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
            dot > 0.5f -> "✓ Correct direction" to Color.Green
            dot < -0.5f -> "⚠ Wrong direction!" to Color.Red
            abs(dot) < 0.3f -> "→ Perpendicular" to Color(0xFFFF9800)
            else -> "~ Angled" to Color.Yellow
        }
    }

    if (gate.gateForward != null && gate.directionDot != null) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Gate fwd: (${"%.2f".format(gate.gateForward.x)}, ${"%.2f".format(gate.gateForward.y)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Dot: ${"%.2f".format(gate.directionDot)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                "isInside: ${gate.isInside}; margin: ${"%.2f".format(gate.margin)}, dParallel: ${"%.2f".format(gate.dParallel)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            gate.signedDistanceFromPlane?.let { signedDist ->
                val positionText = when {
                    signedDist > 1f -> "📍 ${"%.1f".format(signedDist)}m ahead of gate"
                    signedDist < -1f -> "📍 ${"%.1f".format(-signedDist)}m behind gate"
                    else -> "📍 At gate line"
                }
                Text(
                    positionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            directionStatus?.let { (text, color) ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun GateCrossedStatus(gate: GateDebugInfo) {
    if (gate.isCrossed && gate.lastCrossedTimeMs != null) {
        Text(
            "✓ Crossed",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Green
        )
    }
}
