@file:Suppress("WildcardImport", "NoWildcardImports")

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.project.analyzer.calibration.presentation.setup.CalibrationIntent
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.feature.dev.calibration.Res.Res
import com.project.analyzer.feature.dev.calibration.Res.calibration_add_sector
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture_radius
import com.project.analyzer.feature.dev.calibration.Res.calibration_capture_radius_hint
import com.project.analyzer.feature.dev.calibration.Res.calibration_car
import com.project.analyzer.feature.dev.calibration.Res.calibration_detected
import com.project.analyzer.feature.dev.calibration.Res.calibration_extra_sectors_subtitle
import com.project.analyzer.feature.dev.calibration.Res.calibration_extra_sectors_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_flip
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_details
import com.project.analyzer.feature.dev.calibration.Res.calibration_gate_not_captured
import com.project.analyzer.feature.dev.calibration.Res.calibration_header_track_id
import com.project.analyzer.feature.dev.calibration.Res.calibration_no_extra_sectors
import com.project.analyzer.feature.dev.calibration.Res.calibration_recapture
import com.project.analyzer.feature.dev.calibration.Res.calibration_reference_point
import com.project.analyzer.feature.dev.calibration.Res.calibration_reference_point_car_center
import com.project.analyzer.feature.dev.calibration.Res.calibration_reference_point_front_axle
import com.project.analyzer.feature.dev.calibration.Res.calibration_reference_point_rear_axle
import com.project.analyzer.feature.dev.calibration.Res.calibration_remove_sector
import com.project.analyzer.feature.dev.calibration.Res.calibration_reset
import com.project.analyzer.feature.dev.calibration.Res.calibration_save
import com.project.analyzer.feature.dev.calibration.Res.calibration_sector_finish
import com.project.analyzer.feature.dev.calibration.Res.calibration_sector_start_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_sector_title
import com.project.analyzer.feature.dev.calibration.Res.calibration_select_reference_point
import com.project.analyzer.feature.dev.calibration.Res.calibration_start_finish
import com.project.analyzer.feature.dev.calibration.Res.calibration_track_name
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.format.formatDecimal
import org.jetbrains.compose.resources.stringResource
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun CalibrationSectionCard(
    title: String,
    subtitle: String = "",
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
            style = SimAnalyzerTheme.typography.titleMedium,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
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
                    text = stringResource(Res.string.calibration_extra_sectors_title),
                    style = SimAnalyzerTheme.typography.titleSmall,
                    color = SimAnalyzerTheme.material.onSurface,
                )
                Text(
                    text = stringResource(Res.string.calibration_extra_sectors_subtitle),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }

            Button(
                enabled = !state.isBusy,
                onClick = { dispatch(CalibrationIntent.AddSector) },
            ) {
                Text(
                    text = stringResource(Res.string.calibration_add_sector),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        if (state.sectorCount <= 1) {
            Text(
                text = stringResource(Res.string.calibration_no_extra_sectors),
                style = SimAnalyzerTheme.typography.bodySmall,
                color = SimAnalyzerTheme.material.onSurfaceVariant,
            )
        } else {
            for (i in 2..state.sectorCount) {
                val finishLabel = if (i == state.sectorCount) {
                    stringResource(Res.string.calibration_start_finish)
                } else {
                    stringResource(Res.string.calibration_sector_start_title, i + 1)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.calibration_sector_title, i),
                        style = SimAnalyzerTheme.typography.bodyLarge,
                    )
                    IconButton(
                        enabled = !state.isBusy,
                        onClick = { dispatch(CalibrationIntent.RemoveSector(i)) },
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.calibration_remove_sector, i),
                        )
                    }
                }

                GateRow(
                    title = stringResource(Res.string.calibration_sector_start_title, i),
                    gate = state.sectorStart(i),
                    enabled = !state.isBusy,
                    onClick = { dispatch(CalibrationIntent.CaptureSectorStart(i)) },
                    showFlipAction = true,
                    onFlip = { dispatch(CalibrationIntent.FlipSectorStartDirection(i)) },
                )

                Text(
                    text = stringResource(Res.string.calibration_sector_finish, finishLabel),
                    style = SimAnalyzerTheme.typography.bodySmall,
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
        ) {
            Text(
                text = stringResource(Res.string.calibration_save),
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }

        OutlinedButton(
            enabled = !state.isBusy,
            onClick = onReset,
        ) {
            Text(
                text = stringResource(Res.string.calibration_reset),
                style = SimAnalyzerTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
internal fun TrackNameBlock(state: CalibrationState, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = state.trackName,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = stringResource(Res.string.calibration_track_name),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            },
            singleLine = true,
        )

        val detectedTrack = listOfNotNull(state.sessionTrackName, state.sessionTrackId)
            .joinToString(" / ")
            .trim()

        if (detectedTrack.isNotBlank()) {
            InfoRow(label = stringResource(Res.string.calibration_detected), value = detectedTrack)
        }
        InfoRow(label = stringResource(Res.string.calibration_header_track_id), value = state.trackId.ifBlank { "-" })
        state.sessionCarModel?.takeIf { it.isNotBlank() }?.let {
            InfoRow(label = stringResource(Res.string.calibration_car), value = it)
        }
    }
}

@Composable
internal fun SettingsBlock(state: CalibrationState, onRp: (ReferencePoint) -> Unit, onRadius: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.calibration_reference_point),
                style = SimAnalyzerTheme.typography.bodyMedium,
                color = SimAnalyzerTheme.material.onSurface,
            )
            ReferencePointDropdown(selected = state.referencePoint, onSelected = onRp)
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(Res.string.calibration_capture_radius),
                style = SimAnalyzerTheme.typography.bodyMedium,
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
                    text = stringResource(Res.string.calibration_capture_radius_hint),
                    style = SimAnalyzerTheme.typography.bodySmall,
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
                style = SimAnalyzerTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.calibration_select_reference_point),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReferencePoint.entries.forEach { rp ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = rp.displayName(),
                            style = SimAnalyzerTheme.typography.bodySmall,
                        )
                    },
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
fun GateRow(
    title: String,
    gate: Gate?,
    enabled: Boolean,
    onClick: () -> Unit,
    showFlipAction: Boolean = false,
    onFlip: () -> Unit = {},
) {
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
            Text(title, style = SimAnalyzerTheme.typography.bodyLarge)
            if (gate != null) {
                val fwd = gate.forwardV2()
                val angle = Math.toDegrees(kotlin.math.atan2(fwd.x.toDouble(), fwd.y.toDouble()))
                Text(
                    stringResource(
                        Res.string.calibration_gate_details,
                        formatDecimal(gate.center.x, decimals = 1),
                        formatDecimal(gate.center.y, decimals = 1),
                        formatDecimal(angle, decimals = 0),
                    ),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(Res.string.calibration_gate_not_captured),
                    style = SimAnalyzerTheme.typography.bodySmall,
                    color = SimAnalyzerTheme.material.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (gate != null && showFlipAction) {
                OutlinedButton(onClick = onFlip) {
                    Text(
                        text = stringResource(Res.string.calibration_flip),
                        style = SimAnalyzerTheme.typography.labelMedium,
                    )
                }
            }
            Button(onClick = onClick, enabled = enabled) {
                Text(
                    text = stringResource(
                        if (gate == null) Res.string.calibration_capture else Res.string.calibration_recapture,
                    ),
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
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
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
        Text(
            text = value,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = SimAnalyzerTheme.material.onSurface,
        )
    }
}

internal fun copyToClipboard(text: String) {
    Toolkit.getDefaultToolkit()
        .systemClipboard
        .setContents(StringSelection(text), null)
}

@Composable
private fun ReferencePoint.displayName(): String = when (this) {
    ReferencePoint.CAR_CENTER -> stringResource(Res.string.calibration_reference_point_car_center)
    ReferencePoint.FRONT_AXLE -> stringResource(Res.string.calibration_reference_point_front_axle)
    ReferencePoint.REAR_AXLE -> stringResource(Res.string.calibration_reference_point_rear_axle)
}
