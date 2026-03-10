package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.presentation.format.color
import com.analyzer.trackmap.presentation.model.TrackMapMarkerRowUi
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState

@Composable
internal fun TrackMapCalibrationEditorInspectorPanel(
    state: TrackMapCalibrationEditorState,
    selectedRow: TrackMapMarkerRowUi?,
    selectedMarker: TrackMapCalibrationEditorMarker?,
    markers: List<TrackMapCalibrationEditorMarker>,
    isAddPointMode: Boolean,
    onDeletePoint: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrackMapEditorPanel(
        title = "Editor",
        modifier = modifier,
    ) {
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
                color = selectedRow.marker.color(),
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

        if (isAddPointMode) {
            TrackMapEditorMessage(
                message = if (selectedMarker?.gateId == "sf" && markers.size == 1) {
                    "Click on the track to insert the first sector boundary after Start / Finish."
                } else {
                    "Click on the track to insert a new marker after ${selectedRow.title}."
                },
            )
        }

        state.message?.let { message ->
            TrackMapEditorMessage(message)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackMapPrimaryButton(
                label = if (state.isSaving) "Saving..." else "Save",
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.weight(1f),
            )
            TrackMapSecondaryButton(
                label = "Delete",
                onClick = { selectedMarker?.let { marker -> onDeletePoint(marker.gateId) } },
                enabled = selectedMarker?.gateId != "sf",
                modifier = Modifier.width(92.dp),
            )
        }
    }
}
