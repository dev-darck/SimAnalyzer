@file:Suppress("WildcardImport", "NoWildcardImports")

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
import com.project.analyzer.feature.dev.calibration.Res.Res
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_ahead
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_angled
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_at_line
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_behind
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_cancel
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_capturing
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_correct_direction
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_crossed
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_distance
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_dot
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_edit
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_flip
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_forward
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_inside
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_perpendicular
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_wrong_direction
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import kotlinx.collections.immutable.PersistentList
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
fun GateDebugSection(
    gates: PersistentList<GateDebugInfo>,
    editingGate: EditingGate?,
    halfWidthMeters: Float,
    isCapturing: Boolean,
    modifier: Modifier = Modifier,
    onEditGate: (EditingGate) -> Unit = {},
    onCaptureGate: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onFlipGate: (EditingGate) -> Unit = {},
    onRadius: (Float) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
                style = SimAnalyzerTheme.typography.bodyLarge,
                color = SimAnalyzerTheme.material.onSurface,
            )
            Text(
                stringResource(
                    Res.string.calibration_gate_distance,
                    formatDecimal(gate.distanceMeters, decimals = 1),
                ),
                style = SimAnalyzerTheme.typography.bodySmall,
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
                Text(
                    text = stringResource(Res.string.calibration_gate_flip),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        if (isEditing) {
            Button(
                onClick = onCapture,
                enabled = !isCapturing,
            ) {
                Text(
                    text = stringResource(
                        if (isCapturing) Res.string.calibration_gate_capturing else Res.string.calibration_capture,
                    ),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
            OutlinedTextField(
                value = halfWidthMeters.toString(),
                onValueChange = { onRadius(it.toFloatOrNull() ?: halfWidthMeters) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(86.dp),
            )
            OutlinedButton(onClick = onCancel) {
                Text(
                    text = stringResource(Res.string.calibration_gate_cancel),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        } else if (onEdit != null) {
            OutlinedButton(onClick = onEdit) {
                Text(
                    text = stringResource(Res.string.calibration_gate_edit),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun GateDirectionInfo(gate: GateDebugInfo) {
    val directionStatus = gate.directionDot?.let { dot ->
        when {
            dot > 0.5f -> stringResource(
                Res.string.calibration_gate_correct_direction,
            ) to SimAnalyzerTheme.extended.teal

            dot < -0.5f -> stringResource(Res.string.calibration_gate_wrong_direction) to SimAnalyzerTheme.extended.red

            abs(
                dot,
            ) < 0.3f -> stringResource(Res.string.calibration_gate_perpendicular) to SimAnalyzerTheme.extended.amber

            else -> stringResource(Res.string.calibration_gate_angled) to SimAnalyzerTheme.extended.yellow
        }
    }

    if (gate.gateForward != null && gate.directionDot != null) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(
                        Res.string.calibration_gate_forward,
                        formatDecimal(gate.gateForward.x, decimals = 2),
                        formatDecimal(gate.gateForward.y, decimals = 2),
                    ),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
                Text(
                    stringResource(
                        Res.string.calibration_gate_dot,
                        formatDecimal(gate.directionDot, decimals = 2),
                    ),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(2.dp))

            Text(
                stringResource(
                    Res.string.calibration_gate_inside,
                    gate.isInside.toString(),
                    formatDecimal(gate.margin, decimals = 2),
                    formatDecimal(gate.dParallel, decimals = 2),
                ),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )

            gate.signedDistanceFromPlane?.let { signedDist ->
                val positionText = when {
                    signedDist > 1f -> stringResource(
                        Res.string.calibration_gate_ahead,
                        formatDecimal(signedDist, decimals = 1),
                    )

                    signedDist < -1f -> stringResource(
                        Res.string.calibration_gate_behind,
                        formatDecimal(-signedDist, decimals = 1),
                    )

                    else -> stringResource(Res.string.calibration_gate_at_line)
                }
                Text(
                    positionText,
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            directionStatus?.let { (text, color) ->
                Text(
                    text,
                    style = SimAnalyzerTheme.typography.bodySmall,
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
            stringResource(Res.string.calibration_gate_crossed),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.extended.teal,
        )
    }
}
