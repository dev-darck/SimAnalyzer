package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

@Composable
internal fun TrackMapCalibrationEditorWorkspace(
    item: TrackMapLibraryItem,
    state: TrackMapCalibrationEditorState,
    editMode: TrackMapCalibrationEditorMode,
    isAddPointMode: Boolean,
    modifier: Modifier = Modifier,
    onAddPoint: (Gate) -> Unit,
    onBack: () -> Unit,
    onEditModeChange: (TrackMapCalibrationEditorMode) -> Unit,
    onReset: () -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    Box(modifier = modifier) {
        TrackMapCalibrationEditorCanvas(
            item = item,
            state = state,
            markers = state.markers,
            sectors = state.sectors,
            editMode = editMode,
            isAddPointMode = isAddPointMode,
            modifier = Modifier.fillMaxSize(),
            onAddPoint = onAddPoint,
            onSelectMarker = onSelectMarker,
            onUpdateGate = onUpdateGate,
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TrackMapOverlayInfoCard(
                title = item.map.trackName,
                subtitle = buildWorkspaceSubtitle(item),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackMapToolbarPill(label = "Source: ${state.source?.name ?: "NEW"}")
                TrackMapToolbarPill(label = "Markers: ${state.markers.size}")
            }
        }

        TrackMapOverlayCard(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
        ) {
            TrackMapGhostButton(
                label = "Reset",
                onClick = onReset,
                active = state.canReset,
            )
            TrackMapGhostButton(
                label = "Back",
                onClick = onBack,
            )
        }

        if (isAddPointMode) {
            TrackMapOverlayCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
            ) {
                TrackMapToolbarPill(
                    label = if (state.markers.isEmpty()) {
                        "Click on the track to place Start / Finish"
                    } else {
                        "Click on the track to insert a new marker"
                    },
                )
            }
        }

        TrackMapOverlayCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(18.dp),
        ) {
            TrackMapModeSelector(
                selectedMode = editMode,
                onSelectMode = onEditModeChange,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}

private fun buildWorkspaceSubtitle(item: TrackMapLibraryItem): String {
    val layout = item.map.layoutId?.takeIf(String::isNotBlank)
    return buildList {
        add(item.map.gameId.ifBlank { "unknown" })
        add(item.map.trackId)
        layout?.let(::add)
    }.joinToString(" / ")
}
