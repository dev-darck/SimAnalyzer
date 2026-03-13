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
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationWorkspaceUiState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

@Composable
internal fun TrackMapCalibrationEditorWorkspace(
    uiState: TrackMapCalibrationWorkspaceUiState,
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
            uiState = uiState.canvas,
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
                title = uiState.title,
                subtitle = uiState.subtitle,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TrackMapToolbarPill(label = "Source: ${uiState.sourceLabel}")
                TrackMapToolbarPill(label = "Markers: ${uiState.markerCount}")
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
                active = uiState.canReset,
            )
            TrackMapGhostButton(
                label = "Back",
                onClick = onBack,
            )
        }

        uiState.addPointHint?.let { hint ->
            TrackMapOverlayCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
            ) {
                TrackMapToolbarPill(label = hint)
            }
        }

        TrackMapOverlayCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(18.dp),
        ) {
            TrackMapModeSelector(
                selectedMode = uiState.editMode,
                onSelectMode = onEditModeChange,
                modifier = Modifier.width(320.dp),
            )
        }
    }
}
