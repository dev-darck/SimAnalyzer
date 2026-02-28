@file:Suppress("WildcardImport", "NoWildcardImports")

package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.analyzer.settings.domain.model.TelemetrySettings
import com.analyzer.settings.presentation.RecordingWarningKind
import com.project.analyzer.feature.screens.settings.Res.Res
import com.project.analyzer.feature.screens.settings.Res.telemetry_acquisition_title
import com.project.analyzer.feature.screens.settings.Res.telemetry_browse
import com.project.analyzer.feature.screens.settings.Res.telemetry_current_data_size
import com.project.analyzer.feature.screens.settings.Res.telemetry_laps_unlimited
import com.project.analyzer.feature.screens.settings.Res.telemetry_laps_value
import com.project.analyzer.feature.screens.settings.Res.telemetry_max_recorded_laps
import com.project.analyzer.feature.screens.settings.Res.telemetry_preview_storage_location
import com.project.analyzer.feature.screens.settings.Res.telemetry_recording_enabled
import com.project.analyzer.feature.screens.settings.Res.telemetry_sampling_rate
import com.project.analyzer.feature.screens.settings.Res.telemetry_sampling_rate_tick
import com.project.analyzer.feature.screens.settings.Res.telemetry_sampling_rate_value
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_location
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_placeholder
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_size_unknown
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_size_zero
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_unit_b
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_unit_gb
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_unit_kb
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_unit_mb
import com.project.analyzer.feature.screens.settings.Res.telemetry_storage_unit_tb
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_high_rate
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_high_rate_many_laps
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_many_laps
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_title
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_unlimited
import com.project.analyzer.feature.screens.settings.Res.telemetry_warning_unlimited_high_rate
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.components.Button
import com.project.analyzer.ui.components.InfoBar
import com.project.analyzer.ui.components.InfoBarSeverity
import com.project.analyzer.ui.components.SimAnalyzerButtonSize
import com.project.analyzer.ui.components.SimAnalyzerButtonVariant
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.slider.CustomSlider
import com.project.analyzer.ui.slider.THUMB_RADIUS
import com.project.analyzer.ui.textField.TextField
import org.jetbrains.compose.resources.stringResource
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_RATE = TelemetrySettings.MIN_SAMPLING_RATE_HZ
private const val MAX_RATE = TelemetrySettings.MAX_SAMPLING_RATE_HZ
private const val MID_RATE = (MIN_RATE + MAX_RATE) / 2

private const val MIN_LAPS = TelemetrySettings.MIN_MAX_RECORDED_LAPS
private const val MAX_LAPS = TelemetrySettings.MAX_MAX_RECORDED_LAPS
private const val MID_LAPS = (MIN_LAPS + MAX_LAPS) / 2

private val STORAGE_FIELD_HEIGHT = 36.dp

@Composable
internal fun TelemetryAcquisitionBlock(
    samplingRateHz: Int,
    storageLocation: String,
    storageLocationError: String?,
    storageSizeBytes: Long?,
    recordingEnabled: Boolean,
    recordingWarning: RecordingWarningKind?,
    maxRecordedLaps: Int,
    modifier: Modifier = Modifier,
    onSamplingRateChange: (Int) -> Unit = {},
    onStorageLocationChange: (String) -> Unit = {},
    onBrowseClick: () -> Unit = {},
    onRecordingEnabledChange: (Boolean) -> Unit = {},
    onMaxRecordedLapsChange: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier
            .clip(shape = SimAnalyzerTheme.shapes.large)
            .background(color = SimAnalyzerTheme.material.surface)
            .padding(all = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.telemetry_acquisition_title),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        RecordingEnabledSection(
            enabled = recordingEnabled,
            onEnabledChange = onRecordingEnabledChange,
        )

        Spacer(modifier = Modifier.height(12.dp))

        SamplingRateSection(
            samplingRateHz = samplingRateHz,
            onSamplingRateChange = onSamplingRateChange,
        )

        Spacer(modifier = Modifier.height(12.dp))

        MaxRecordedLapsSection(
            maxRecordedLaps = maxRecordedLaps,
            onMaxRecordedLapsChange = onMaxRecordedLapsChange,
        )

        val warning = recordingWarning
        if (warning != null) {
            Spacer(modifier = Modifier.height(8.dp))
            InfoBar(
                title = stringResource(Res.string.telemetry_warning_title),
                message = recordingWarningText(warning),
                severity = InfoBarSeverity.Warning,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StorageLocationSection(
            storageLocation = storageLocation,
            error = storageLocationError,
            storageSizeBytes = storageSizeBytes,
            onStorageLocationChange = onStorageLocationChange,
            onBrowseClick = onBrowseClick,
        )
    }
}

@Composable
private fun RecordingEnabledSection(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.telemetry_recording_enabled),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodyLarge,
        )

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SimAnalyzerTheme.material.onPrimary,
                checkedTrackColor = SimAnalyzerTheme.material.primary,
                uncheckedThumbColor = SimAnalyzerTheme.material.onSurfaceVariant,
                uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f),
            ),
        )
    }
}

@Composable
private fun SamplingRateSection(samplingRateHz: Int, onSamplingRateChange: (Int) -> Unit) {
    var sliderPosition by remember(samplingRateHz) {
        mutableFloatStateOf(samplingRateHz.toFloat())
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.telemetry_sampling_rate),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodyLarge,
            )

            Box(
                modifier = Modifier
                    .clip(SimAnalyzerTheme.shapes.small)
                    .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.telemetry_sampling_rate_value,
                        sliderPosition.roundToInt(),
                    ),
                    color = SimAnalyzerTheme.material.primary,
                    style = SimAnalyzerTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomSlider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                onSamplingRateChange(sliderPosition.roundToInt())
            },
            valueRange = MIN_RATE.toFloat()..MAX_RATE.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = THUMB_RADIUS),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.telemetry_sampling_rate_tick, MIN_RATE),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.telemetry_sampling_rate_tick, MID_RATE),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(Res.string.telemetry_sampling_rate_tick, MAX_RATE),
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MaxRecordedLapsSection(maxRecordedLaps: Int, onMaxRecordedLapsChange: (Int) -> Unit) {
    var sliderPosition by remember(maxRecordedLaps) {
        mutableFloatStateOf(maxRecordedLaps.toFloat())
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.telemetry_max_recorded_laps),
                color = SimAnalyzerTheme.material.onSurface,
                style = SimAnalyzerTheme.typography.bodyLarge,
            )

            val laps = sliderPosition.roundToInt()
            val label = if (laps == 0) {
                stringResource(Res.string.telemetry_laps_unlimited)
            } else {
                stringResource(Res.string.telemetry_laps_value, laps)
            }
            Box(
                modifier = Modifier
                    .clip(SimAnalyzerTheme.shapes.small)
                    .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    color = SimAnalyzerTheme.material.primary,
                    style = SimAnalyzerTheme.typography.labelMedium,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomSlider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            onValueChangeFinished = {
                onMaxRecordedLapsChange(sliderPosition.roundToInt())
            },
            valueRange = MIN_LAPS.toFloat()..MAX_LAPS.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = THUMB_RADIUS),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "$MIN_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = "$MID_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            Text(
                text = "$MAX_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StorageLocationSection(
    storageLocation: String,
    error: String?,
    storageSizeBytes: Long?,
    onStorageLocationChange: (String) -> Unit,
    onBrowseClick: () -> Unit,
) {
    Column {
        Text(
            text = stringResource(Res.string.telemetry_storage_location),
            color = SimAnalyzerTheme.material.onSurface,
            style = SimAnalyzerTheme.typography.bodyLarge,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            StoragePathField(
                value = storageLocation,
                placeholder = stringResource(Res.string.telemetry_storage_placeholder),
                onValueChange = onStorageLocationChange,
                onClick = onBrowseClick,
                modifier = Modifier.weight(1f),
            )

            BrowseButton(
                onClick = onBrowseClick,
                modifier = Modifier.height(STORAGE_FIELD_HEIGHT),
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = SimAnalyzerTheme.material.error,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                Res.string.telemetry_current_data_size,
                storageSizeLabel(storageSizeBytes),
            ),
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            style = SimAnalyzerTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun StoragePathField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = SimAnalyzerTheme.material.onSurface
        .copy(alpha = 0.85f)

    var text by remember(value) { mutableStateOf(value) }

    TextField(
        value = text,
        placeholder = placeholder,
        leadingIcon = Icons.Filled.Folder,
        onValueChange = {
            text = it.trim()
            onValueChange(it.trim())
        },
        singleLine = true,
        textStyle = SimAnalyzerTheme.typography.labelMedium.copy(color = textColor),
        modifier = modifier.onClick(onClick = onClick),
    )
}

@Composable
private fun BrowseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        text = stringResource(Res.string.telemetry_browse),
        onClick = onClick,
        modifier = modifier
            .height(STORAGE_FIELD_HEIGHT)
            .widthIn(min = 72.dp),
        size = SimAnalyzerButtonSize.Compact,
        variant = SimAnalyzerButtonVariant.Secondary,
    )
}

@Composable
private fun recordingWarningText(kind: RecordingWarningKind): String = when (kind) {
    RecordingWarningKind.UnlimitedHighRate -> stringResource(Res.string.telemetry_warning_unlimited_high_rate)
    RecordingWarningKind.Unlimited -> stringResource(Res.string.telemetry_warning_unlimited)
    RecordingWarningKind.HighRateManyLaps -> stringResource(Res.string.telemetry_warning_high_rate_many_laps)
    RecordingWarningKind.HighRate -> stringResource(Res.string.telemetry_warning_high_rate)
    RecordingWarningKind.ManyLaps -> stringResource(Res.string.telemetry_warning_many_laps)
}

@Composable
private fun storageSizeLabel(bytes: Long?): String {
    val units = listOf(
        stringResource(Res.string.telemetry_storage_unit_b),
        stringResource(Res.string.telemetry_storage_unit_kb),
        stringResource(Res.string.telemetry_storage_unit_mb),
        stringResource(Res.string.telemetry_storage_unit_gb),
        stringResource(Res.string.telemetry_storage_unit_tb),
    )
    val unknown = stringResource(Res.string.telemetry_storage_size_unknown)
    val zero = stringResource(Res.string.telemetry_storage_size_zero)

    val value = bytes ?: return unknown
    if (value <= 0) return zero

    var size = value.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex += 1
    }

    val format = when {
        size >= 100 -> "%.0f"
        size >= 10 -> "%.1f"
        else -> "%.2f"
    }

    return String.format(Locale.US, "$format ${units[unitIndex]}", size)
}

@Preview
@Composable
private fun TelemetryAcquisitionBlockPreview() {
    SimAnalyzerTheme {
        TelemetryAcquisitionBlock(
            samplingRateHz = 50,
            storageLocation = stringResource(Res.string.telemetry_preview_storage_location),
            storageLocationError = null,
            storageSizeBytes = 12_300_000L,
            recordingEnabled = true,
            recordingWarning = RecordingWarningKind.UnlimitedHighRate,
            maxRecordedLaps = 25,
        )
    }
}
