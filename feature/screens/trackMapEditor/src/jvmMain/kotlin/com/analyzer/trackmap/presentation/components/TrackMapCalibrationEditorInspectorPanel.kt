package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationInspectorUiState
import com.analyzer.trackmap.presentation.model.color

@Composable
internal fun TrackMapCalibrationEditorInspectorPanel(
    uiState: TrackMapCalibrationInspectorUiState,
    onDeletePoint: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackMapEditorPanel(
        title = "Editor",
        modifier = modifier,
    ) {
        val selectedRow = uiState.selectedRow
        if (selectedRow == null) {
            TrackMapEditorMessage("Select a marker or create Start / Finish to edit gate placement.")
            return@TrackMapEditorPanel
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            TrackMapEditorField(
                label = "Marker Name",
                value = selectedRow.title,
                modifier = Modifier.weight(1f),
            )
            TrackMapColorField(
                color = selectedRow.color(),
                modifier = Modifier.width(92.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrackMapEditorField(
                label = "Start (m)",
                value = selectedRow.startLabel,
                modifier = Modifier.weight(1f),
            )
            TrackMapEditorField(
                label = "End (m)",
                value = selectedRow.endLabel,
                modifier = Modifier.weight(1f),
            )
        }

        uiState.addPointHint?.let { hint ->
            TrackMapEditorMessage(hint)
        }

        uiState.message?.let { message ->
            TrackMapEditorMessage(message)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackMapPrimaryButton(
                label = uiState.saveLabel,
                onClick = onSave,
                enabled = uiState.canSave,
                modifier = Modifier.weight(1f),
            )
            TrackMapSecondaryButton(
                label = "Delete",
                onClick = { uiState.deleteGateId?.let(onDeletePoint) },
                enabled = uiState.deleteGateId != null,
                modifier = Modifier.width(92.dp),
            )
        }
    }
}
