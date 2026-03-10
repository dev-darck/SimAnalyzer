package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.project.analyzer.theme.SimAnalyzerTheme

@Composable
internal fun TrackMapEditorPanel(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(material.surface)
            .border(1.dp, chrome.borderSubtle, SimAnalyzerTheme.shapes.large),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(material.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = SimAnalyzerTheme.typography.titleMedium,
                color = material.onSurface,
            )
            trailing?.invoke()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun TrackMapGhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(
                if (active) chrome.fillSelection else material.background,
            )
            .border(
                1.dp,
                if (active) chrome.borderInteractiveStrong else chrome.borderSecondary,
                SimAnalyzerTheme.corners.pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelMedium,
            color = material.onSurface,
        )
    }
}

@Composable
internal fun TrackMapOverlayCard(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Row(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(material.surface.copy(alpha = 0.92f))
            .border(1.dp, chrome.borderSubtle, SimAnalyzerTheme.corners.pill)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun TrackMapOverlayInfoCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Column(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(material.surface.copy(alpha = 0.94f))
            .border(1.dp, chrome.borderSubtle, SimAnalyzerTheme.shapes.large)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = SimAnalyzerTheme.typography.titleSmall,
            color = material.onSurface,
        )
        Text(
            text = subtitle,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = material.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TrackMapToolbarPill(label: String, modifier: Modifier = Modifier) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.corners.pill)
            .background(material.primary.copy(alpha = 0.16f))
            .border(1.dp, chrome.borderInteractive, SimAnalyzerTheme.corners.pill)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelMedium,
            color = material.onSurface,
        )
    }
}

@Composable
internal fun TrackMapPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val material = SimAnalyzerTheme.material
    val background = if (enabled) material.primary else material.primary.copy(alpha = 0.45f)
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelLarge,
            color = material.onPrimary,
        )
    }
}

@Composable
internal fun TrackMapSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(material.background)
            .border(1.dp, chrome.borderSecondary, SimAnalyzerTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelMedium,
            color = if (enabled) material.onSurface else material.onSurfaceVariant.copy(alpha = 0.65f),
        )
    }
}

@Composable
internal fun TrackMapMarkerTableHeader() {
    val material = SimAnalyzerTheme.material
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(material.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackMapTableHeaderCell(text = "Name", modifier = Modifier.weight(1f))
        TrackMapTableHeaderCell(text = "Start", modifier = Modifier.width(70.dp))
        TrackMapTableHeaderCell(text = "End", modifier = Modifier.width(70.dp))
    }
}

@Composable
private fun TrackMapTableHeaderCell(text: String, modifier: Modifier = Modifier) {
    val material = SimAnalyzerTheme.material
    Text(
        text = text,
        modifier = modifier,
        style = SimAnalyzerTheme.typography.labelSmall,
        color = material.onSurfaceVariant,
    )
}

@Composable
internal fun TrackMapMarkerTableRow(
    title: String,
    startLabel: String,
    endLabel: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(if (selected) chrome.fillSelection else chrome.tableRowOdd)
            .border(
                1.dp,
                if (selected) chrome.borderInteractive else chrome.dividerSubtle.copy(alpha = 0.6f),
                SimAnalyzerTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = title,
                style = SimAnalyzerTheme.typography.bodySmall,
                color = material.onSurface,
            )
        }
        Text(
            text = startLabel,
            modifier = Modifier.width(70.dp),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = material.onSurfaceVariant,
        )
        Text(
            text = endLabel,
            modifier = Modifier.width(70.dp),
            style = SimAnalyzerTheme.typography.bodySmall,
            color = material.onSurfaceVariant,
        )
    }
}

@Composable
internal fun TrackMapEditorField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = SimAnalyzerTheme.typography.labelSmall,
            color = material.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SimAnalyzerTheme.shapes.medium)
                .background(material.background)
                .border(1.dp, chrome.borderSecondary, SimAnalyzerTheme.shapes.medium)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                style = SimAnalyzerTheme.typography.bodyMedium,
                color = material.onSurface,
            )
            trailing?.invoke()
        }
    }
}

@Composable
internal fun TrackMapColorField(color: Color, modifier: Modifier = Modifier) {
    TrackMapEditorField(
        label = "Color",
        value = "#${color.value.toString(16).takeLast(6).uppercase()}",
        modifier = modifier,
        trailing = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(SimAnalyzerTheme.shapes.small)
                    .background(color),
            )
        },
    )
}

@Composable
internal fun TrackMapModeSelector(
    selectedMode: TrackMapCalibrationEditorMode,
    onSelectMode: (TrackMapCalibrationEditorMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TrackMapCalibrationEditorMode.entries.forEach { mode ->
            TrackMapGhostButton(
                label = mode.title,
                onClick = { onSelectMode(mode) },
                modifier = Modifier.weight(1f),
                active = mode == selectedMode,
            )
        }
    }
}

@Composable
internal fun TrackMapEditorMessage(message: String) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(chrome.fillSelection.copy(alpha = 0.75f))
            .border(1.dp, chrome.borderInteractive, SimAnalyzerTheme.shapes.medium)
            .padding(10.dp),
    ) {
        Text(
            text = message,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = material.onSurface,
        )
    }
}
