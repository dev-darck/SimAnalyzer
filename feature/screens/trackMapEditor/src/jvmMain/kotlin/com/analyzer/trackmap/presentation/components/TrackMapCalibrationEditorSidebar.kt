package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.presentation.model.buildTrackMapMarkerRows
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar

@Composable
internal fun TrackMapCalibrationEditorSidebar(
    state: TrackMapCalibrationEditorState,
    isAddPointMode: Boolean,
    modifier: Modifier,
    scrollable: Boolean,
    onAddPointModeChange: (Boolean) -> Unit,
    onDeletePoint: (String) -> Unit,
    onSave: () -> Unit,
    onSelectMarker: (String) -> Unit,
) {
    val markerRows = buildTrackMapMarkerRows(state.markers)
    val selectedRow = markerRows.firstOrNull { it.marker.gateId == state.selectedMarkerId } ?: markerRows.firstOrNull()
    val selectedMarker = selectedRow?.marker

    val content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        TrackMapCalibrationEditorMarkerPanel(
            markerRows = markerRows,
            selectedMarkerId = state.selectedMarkerId,
            isAddPointMode = isAddPointMode,
            onAddPointModeChange = onAddPointModeChange,
            onSelectMarker = onSelectMarker,
            modifier = Modifier.fillMaxWidth(),
        )
        TrackMapCalibrationEditorInspectorPanel(
            state = state,
            selectedRow = selectedRow,
            selectedMarker = selectedMarker,
            markers = state.markers,
            isAddPointMode = isAddPointMode,
            onDeletePoint = onDeletePoint,
            onSave = onSave,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (!scrollable) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
        return
    }

    val scrollState = rememberScrollState()
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            content()
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            adapter = rememberScrollbarAdapter(scrollState),
        )
    }
}
