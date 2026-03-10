package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.format.color
import com.analyzer.trackmap.presentation.model.TrackMapMarkerRowUi

@Composable
internal fun TrackMapCalibrationEditorMarkerPanel(
    markerRows: List<TrackMapMarkerRowUi>,
    selectedMarkerId: String?,
    isAddPointMode: Boolean,
    onAddPointModeChange: (Boolean) -> Unit,
    onSelectMarker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackMapEditorPanel(
        title = "Markers / ${markerRows.size}",
        modifier = modifier,
        trailing = {
            TrackMapGhostButton(
                label = if (isAddPointMode) "Cancel" else "New marker",
                onClick = { onAddPointModeChange(!isAddPointMode) },
                active = isAddPointMode,
            )
        },
    ) {
        TrackMapMarkerTableHeader()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (markerRows.isEmpty()) {
                TrackMapEditorMessage(
                    message = if (isAddPointMode) {
                        "Click on the track to place Start / Finish."
                    } else {
                        "No markers yet. Create Start / Finish to start building sectors."
                    },
                )
            } else {
                markerRows.forEach { row ->
                    TrackMapMarkerTableRow(
                        title = row.title,
                        startLabel = row.startLabel,
                        endLabel = row.endLabel,
                        color = row.marker.color(),
                        selected = row.marker.gateId == selectedMarkerId,
                        onClick = { onSelectMarker(row.marker.gateId) },
                    )
                }
            }
        }
    }
}
