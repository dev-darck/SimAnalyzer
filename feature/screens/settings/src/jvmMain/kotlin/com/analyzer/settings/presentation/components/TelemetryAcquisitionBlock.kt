package com.analyzer.settings.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.slider.CustomSlider
import com.project.analyzer.ui.slider.THUMB_RADIUS
import kotlin.math.roundToInt

private const val MIN_RATE = 10
private const val MID_RATE = 55
private const val MAX_RATE = 100

private val STORAGE_FIELD_HEIGHT = 36.dp
private val STORAGE_FIELD_RADIUS = 8.dp
private val STORAGE_FIELD_BORDER = 1.dp

@Composable
internal fun TelemetryAcquisitionBlock(
    samplingRateHz: Int,
    storageLocation: String,
    storageLocationError: String?,
    modifier: Modifier = Modifier,
    onSamplingRateChange: (Int) -> Unit = {},
    onStorageLocationChange: (String) -> Unit = {},
    onBrowseClick: () -> Unit = {}
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

        SamplingRateSection(
            samplingRateHz = samplingRateHz,
            onSamplingRateChange = onSamplingRateChange
        )

        Spacer(modifier = Modifier.height(12.dp))

        StorageLocationSection(
            storageLocation = storageLocation,
            error = storageLocationError,
            onStorageLocationChange = onStorageLocationChange,
            onBrowseClick = onBrowseClick
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
private fun StorageLocationSection(
    storageLocation: String,
    error: String?,
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
    var focused by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(STORAGE_FIELD_RADIUS)

    val border = SimAnalyzerTheme.material.primary.copy(alpha = if (focused) 0.75f else 0.45f)

    val iconTint = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.70f)
    val textColor = SimAnalyzerTheme.material.onSurface
        .copy(alpha = 0.85f)

    var text by remember(value) { mutableStateOf(value) }

    Row(
        modifier = modifier
            .height(STORAGE_FIELD_HEIGHT)
            .clip(shape)
            .background(SimAnalyzerTheme.material.background)
            .border(STORAGE_FIELD_BORDER, border, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(start = 18.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Folder,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        BasicTextField(
            value = text,
            onValueChange = {
                text = it.trim()
                onValueChange(it.trim())
            },
            singleLine = true,
            cursorBrush = SolidColor(SimAnalyzerTheme.material.primary),
            textStyle = TextStyle(
                color = textColor,
                fontSize = 12.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (text.isBlank()) {
                        Text(
                            text = placeholder,
                            color = SimAnalyzerTheme.material.onSurfaceVariant.copy(alpha = 0.55f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            }
        )
    }
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
            .clickable(onClick = onClick)
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
            storageLocationError = null
        )
    }
}
