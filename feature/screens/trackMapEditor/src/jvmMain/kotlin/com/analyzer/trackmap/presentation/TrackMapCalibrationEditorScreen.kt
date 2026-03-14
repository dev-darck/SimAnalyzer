package com.analyzer.trackmap.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.analyzer.trackmap.presentation.components.TrackMapCalibrationEditorSidebar
import com.analyzer.trackmap.presentation.components.TrackMapCalibrationEditorWorkspace
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationSidebarUiState
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationWorkspaceUiState
import com.analyzer.trackmap.presentation.model.toSidebarUiState
import com.analyzer.trackmap.presentation.model.toWorkspaceUiState
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppScrollbarAdapter
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun TrackMapCalibrationEditorScreen(
    gameId: String,
    trackId: String,
    layoutId: String?,
    onBack: () -> Unit
) {
    val material = SimAnalyzerTheme.material
    val mapKey = remember(gameId, trackId, layoutId) {
        buildTrackMapCalibrationEditorMapKey(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )
    }
    val viewModel = metroViewModel<TrackMapCalibrationEditorViewModel>(key = mapKey)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(mapKey) {
        viewModel.load(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )
    }

    var editMode by rememberSaveable(mapKey) { mutableStateOf(TrackMapCalibrationEditorMode.Move) }
    var isAddPointMode by rememberSaveable(mapKey) { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(material.background)
            .padding(18.dp),
    ) {
        val item = state.item ?: run {
            TrackMapCalibrationEditorLoadingState(message = state.message)
            return@BoxWithConstraints
        }
        val workspaceUiState = remember(item, state, editMode, isAddPointMode) {
            state.toWorkspaceUiState(
                item = item,
                editMode = editMode,
                isAddPointMode = isAddPointMode,
            )
        }
        val sidebarUiState = remember(state, isAddPointMode) {
            state.toSidebarUiState(isAddPointMode = isAddPointMode)
        }

        TrackMapCalibrationEditorLoadedContent(
            isWideLayout = maxWidth >= 1120.dp,
            workspaceUiState = workspaceUiState,
            sidebarUiState = sidebarUiState,
            onAddPointModeChange = { enabled ->
                isAddPointMode = enabled
                if (enabled) {
                    editMode = TrackMapCalibrationEditorMode.Move
                }
            },
            onAddPoint = { gate ->
                viewModel.addGateAfterSelected(gate)
                isAddPointMode = false
                editMode = TrackMapCalibrationEditorMode.Move
            },
            onBack = onBack,
            onDeletePoint = viewModel::deleteGate,
            onEditModeChange = { editMode = it },
            onReset = viewModel::reset,
            onSave = viewModel::save,
            onSelectMarker = viewModel::selectMarker,
            onUpdateGate = viewModel::updateGate,
        )
    }
}

@Composable
private fun TrackMapCalibrationEditorLoadingState(message: String?) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message ?: "Loading track map...",
            color = SimAnalyzerTheme.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrackMapCalibrationEditorLoadedContent(
    isWideLayout: Boolean,
    workspaceUiState: TrackMapCalibrationWorkspaceUiState,
    sidebarUiState: TrackMapCalibrationSidebarUiState,
    onAddPointModeChange: (Boolean) -> Unit,
    onAddPoint: (Gate) -> Unit,
    onBack: () -> Unit,
    onDeletePoint: (String) -> Unit,
    onEditModeChange: (TrackMapCalibrationEditorMode) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    if (isWideLayout) {
        TrackMapCalibrationEditorWideLayout(
            workspaceUiState = workspaceUiState,
            sidebarUiState = sidebarUiState,
            onAddPointModeChange = onAddPointModeChange,
            onAddPoint = onAddPoint,
            onBack = onBack,
            onDeletePoint = onDeletePoint,
            onEditModeChange = onEditModeChange,
            onReset = onReset,
            onSave = onSave,
            onSelectMarker = onSelectMarker,
            onUpdateGate = onUpdateGate,
        )
        return
    }

    TrackMapCalibrationEditorCompactLayout(
        workspaceUiState = workspaceUiState,
        sidebarUiState = sidebarUiState,
        onAddPointModeChange = onAddPointModeChange,
        onAddPoint = onAddPoint,
        onBack = onBack,
        onDeletePoint = onDeletePoint,
        onEditModeChange = onEditModeChange,
        onReset = onReset,
        onSave = onSave,
        onSelectMarker = onSelectMarker,
        onUpdateGate = onUpdateGate,
    )
}

@Composable
private fun TrackMapCalibrationEditorWideLayout(
    workspaceUiState: TrackMapCalibrationWorkspaceUiState,
    sidebarUiState: TrackMapCalibrationSidebarUiState,
    onAddPointModeChange: (Boolean) -> Unit,
    onAddPoint: (Gate) -> Unit,
    onBack: () -> Unit,
    onDeletePoint: (String) -> Unit,
    onEditModeChange: (TrackMapCalibrationEditorMode) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TrackMapCalibrationEditorSidebar(
            uiState = sidebarUiState,
            modifier = Modifier
                .width(328.dp)
                .fillMaxHeight(),
            scrollable = true,
            onAddPointModeChange = onAddPointModeChange,
            onDeletePoint = onDeletePoint,
            onSave = onSave,
            onSelectMarker = onSelectMarker,
        )
        TrackMapCalibrationEditorWorkspace(
            uiState = workspaceUiState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onAddPoint = onAddPoint,
            onBack = onBack,
            onEditModeChange = onEditModeChange,
            onReset = onReset,
            onSelectMarker = onSelectMarker,
            onUpdateGate = onUpdateGate,
        )
    }
}

@Composable
private fun TrackMapCalibrationEditorCompactLayout(
    workspaceUiState: TrackMapCalibrationWorkspaceUiState,
    sidebarUiState: TrackMapCalibrationSidebarUiState,
    onAddPointModeChange: (Boolean) -> Unit,
    onAddPoint: (Gate) -> Unit,
    onBack: () -> Unit,
    onDeletePoint: (String) -> Unit,
    onEditModeChange: (TrackMapCalibrationEditorMode) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TrackMapCalibrationEditorWorkspace(
                uiState = workspaceUiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp),
                onAddPoint = onAddPoint,
                onBack = onBack,
                onEditModeChange = onEditModeChange,
                onReset = onReset,
                onSelectMarker = onSelectMarker,
                onUpdateGate = onUpdateGate,
            )
            TrackMapCalibrationEditorSidebar(
                uiState = sidebarUiState,
                modifier = Modifier.fillMaxWidth(),
                scrollable = false,
                onAddPointModeChange = onAddPointModeChange,
                onDeletePoint = onDeletePoint,
                onSave = onSave,
                onSelectMarker = onSelectMarker,
            )
        }
        AppVerticalScrollbar(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            adapter = AppScrollbarAdapter(rememberScrollbarAdapter(scrollState)),
        )
    }
}
