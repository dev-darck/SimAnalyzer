package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationMarkerPanelUiState
import com.analyzer.trackmap.presentation.model.color

@Composable
internal fun TrackMapCalibrationEditorMarkerPanel(
    uiState: TrackMapCalibrationMarkerPanelUiState,
    onAddPointModeChange: (Boolean) -> Unit,
    onSelectMarker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackMapEditorPanel(
        title = "Markers / ${uiState.markerRows.size}",
        modifier = modifier,
        trailing = {
            TrackMapGhostButton(
                label = if (uiState.isAddPointMode) "Cancel" else "New marker",
                onClick = { onAddPointModeChange(!uiState.isAddPointMode) },
                active = uiState.isAddPointMode,
            )
        },
    ) {
        TrackMapMarkerTableHeader()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (uiState.markerRows.isEmpty()) {
                TrackMapEditorMessage(
                    message = if (uiState.isAddPointMode) {
                        "Click on the track to place Start / Finish."
                    } else {
                        "No markers yet. Create Start / Finish to start building sectors."
                    },
                )
            } else {
                uiState.markerRows.forEach { row ->
                    TrackMapMarkerTableRow(
                        title = row.title,
                        startLabel = row.startLabel,
                        endLabel = row.endLabel,
                        color = row.color(),
                        selected = row.gateId == uiState.selectedMarkerId,
                        onClick = { onSelectMarker(row.gateId) },
                    )
                }
            }
        }
    }
}
