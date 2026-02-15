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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.analyzer.settings.domain.model.TelemetrySettings
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.modifier.onClick
import com.project.analyzer.ui.slider.CustomSlider
import com.project.analyzer.ui.slider.THUMB_RADIUS
import com.project.analyzer.ui.textField.TextField
import kotlin.math.roundToInt

private const val MIN_RATE = TelemetrySettings.MIN_SAMPLING_RATE_HZ
private const val MAX_RATE = TelemetrySettings.MAX_SAMPLING_RATE_HZ
private const val MID_RATE = (MIN_RATE + MAX_RATE) / 2

private const val MIN_LAPS = TelemetrySettings.MIN_MAX_RECORDED_LAPS
private const val MAX_LAPS = TelemetrySettings.MAX_MAX_RECORDED_LAPS
private const val MID_LAPS = (MIN_LAPS + MAX_LAPS) / 2

private val STORAGE_FIELD_HEIGHT = 36.dp
private val STORAGE_FIELD_RADIUS = 8.dp

@Composable
internal fun TelemetryAcquisitionBlock(
    samplingRateHz: Int,
    storageLocation: String,
    storageLocationError: String?,
    storageSizeLabel: String,
    recordingEnabled: Boolean,
    recordingWarning: String?,
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
            .padding(all = 16.dp)
    ) {
        Text(
            text = "Telemetry acquisition",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        RecordingEnabledSection(
            enabled = recordingEnabled,
            onEnabledChange = onRecordingEnabledChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        SamplingRateSection(
            samplingRateHz = samplingRateHz,
            onSamplingRateChange = onSamplingRateChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        MaxRecordedLapsSection(
            maxRecordedLaps = maxRecordedLaps,
            onMaxRecordedLapsChange = onMaxRecordedLapsChange
        )

        val warning = recordingWarning
        if (warning != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = warning,
                color = SimAnalyzerTheme.extended.orange,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        StorageLocationSection(
            storageLocation = storageLocation,
            error = storageLocationError,
            storageSizeLabel = storageSizeLabel,
            onStorageLocationChange = onStorageLocationChange,
            onBrowseClick = onBrowseClick
        )
    }
}

@Composable
private fun RecordingEnabledSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Recording enabled",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )

        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SimAnalyzerTheme.material.onPrimary,
                checkedTrackColor = SimAnalyzerTheme.material.primary,
                uncheckedThumbColor = SimAnalyzerTheme.material.onSurfaceVariant,
                uncheckedTrackColor = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.25f)
            )
        )
    }
}

@Composable
private fun SamplingRateSection(
    samplingRateHz: Int,
    onSamplingRateChange: (Int) -> Unit
) {
    var sliderPosition by remember(samplingRateHz) {
        mutableFloatStateOf(samplingRateHz.toFloat())
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sampling rate (Hz)",
                color = SimAnalyzerTheme.material.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Box(
                modifier = Modifier
                    .clip(SimAnalyzerTheme.shapes.small)
                    .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${sliderPosition.roundToInt()} Hz",
                    color = SimAnalyzerTheme.material.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = THUMB_RADIUS),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$MIN_RATE Hz",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "$MID_RATE Hz",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "$MAX_RATE Hz",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MaxRecordedLapsSection(
    maxRecordedLaps: Int,
    onMaxRecordedLapsChange: (Int) -> Unit
) {
    var sliderPosition by remember(maxRecordedLaps) {
        mutableFloatStateOf(maxRecordedLaps.toFloat())
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Max recorded laps",
                color = SimAnalyzerTheme.material.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            val laps = sliderPosition.roundToInt()
            val label = if (laps == 0) "Unlimited" else "$laps laps"
            Box(
                modifier = Modifier
                    .clip(SimAnalyzerTheme.shapes.small)
                    .background(SimAnalyzerTheme.material.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label,
                    color = SimAnalyzerTheme.material.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
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
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = THUMB_RADIUS),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$MIN_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "$MID_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Text(
                text = "$MAX_LAPS",
                color = SimAnalyzerTheme.material.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StorageLocationSection(
    storageLocation: String,
    error: String?,
    storageSizeLabel: String,
    onStorageLocationChange: (String) -> Unit,
    onBrowseClick: () -> Unit
) {
    Column {
        Text(
            text = "Storage location",
            color = SimAnalyzerTheme.material.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StoragePathField(
                value = storageLocation,
                placeholder = "Select folder...",
                onValueChange = onStorageLocationChange,
                onClick = onBrowseClick,
                modifier = Modifier.weight(1f)
            )

            BrowseButton(
                onClick = onBrowseClick,
                modifier = Modifier.height(STORAGE_FIELD_HEIGHT)
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = SimAnalyzerTheme.material.error,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Current data size: $storageSizeLabel",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
            fontSize = 12.sp,
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
        textStyle = TextStyle(
            color = textColor,
            fontSize = 12.sp,
        ),
        modifier = modifier.onClick(onClick = onClick)
    )
}

@Composable
private fun BrowseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(STORAGE_FIELD_HEIGHT)
            .widthIn(min = 64.dp)
            .onClick(onClick = onClick)
            .background(
                color = SimAnalyzerTheme.material.primary.copy(alpha = 0.7f),
                shape = RoundedCornerShape(STORAGE_FIELD_RADIUS)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Browse",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = SimAnalyzerTheme.material.onSurface
        )
    }
}

@Preview
@Composable
private fun TelemetryAcquisitionBlockPreview() {
    SimAnalyzerTheme {
        TelemetryAcquisitionBlock(
            samplingRateHz = 50,
            storageLocation = "/sdcard/telemetry",
            storageLocationError = null,
            storageSizeLabel = "12.3 MB",
            recordingEnabled = true,
            recordingWarning = "Unlimited laps at high Hz can create very large files.",
            maxRecordedLaps = 25,
        )
    }
}
