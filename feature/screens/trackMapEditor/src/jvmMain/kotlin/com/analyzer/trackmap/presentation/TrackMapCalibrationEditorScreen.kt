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
import com.project.analyzer.theme.SimAnalyzerTheme
import com.project.analyzer.ui.scrollbar.AppVerticalScrollbar
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun TrackMapCalibrationEditorScreen(gameId: String, trackId: String, layoutId: String?, onBack: () -> Unit) {
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
        val item = state.item
        if (item == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.message ?: "Loading track map...",
                    color = material.onSurfaceVariant,
                )
            }
            return@BoxWithConstraints
        }

        val wideLayout = maxWidth >= 1120.dp
        if (wideLayout) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                TrackMapCalibrationEditorSidebar(
                    state = state,
                    isAddPointMode = isAddPointMode,
                    modifier = Modifier
                        .width(328.dp)
                        .fillMaxHeight(),
                    scrollable = true,
                    onAddPointModeChange = { enabled ->
                        isAddPointMode = enabled
                        if (enabled) {
                            editMode = TrackMapCalibrationEditorMode.Move
                        }
                    },
                    onDeletePoint = viewModel::deleteGate,
                    onSave = viewModel::save,
                    onSelectMarker = viewModel::selectMarker,
                )
                TrackMapCalibrationEditorWorkspace(
                    item = item,
                    state = state,
                    editMode = editMode,
                    isAddPointMode = isAddPointMode,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onAddPoint = { gate ->
                        viewModel.addGateAfterSelected(gate)
                        isAddPointMode = false
                        editMode = TrackMapCalibrationEditorMode.Move
                    },
                    onBack = onBack,
                    onEditModeChange = { editMode = it },
                    onReset = viewModel::reset,
                    onSelectMarker = viewModel::selectMarker,
                    onUpdateGate = viewModel::updateGate,
                )
            }
            return@BoxWithConstraints
        }

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
                    item = item,
                    state = state,
                    editMode = editMode,
                    isAddPointMode = isAddPointMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(540.dp),
                    onAddPoint = { gate ->
                        viewModel.addGateAfterSelected(gate)
                        isAddPointMode = false
                        editMode = TrackMapCalibrationEditorMode.Move
                    },
                    onBack = onBack,
                    onEditModeChange = { editMode = it },
                    onReset = viewModel::reset,
                    onSelectMarker = viewModel::selectMarker,
                    onUpdateGate = viewModel::updateGate,
                )
                TrackMapCalibrationEditorSidebar(
                    state = state,
                    isAddPointMode = isAddPointMode,
                    modifier = Modifier.fillMaxWidth(),
                    scrollable = false,
                    onAddPointModeChange = { enabled ->
                        isAddPointMode = enabled
                        if (enabled) {
                            editMode = TrackMapCalibrationEditorMode.Move
                        }
                    },
                    onDeletePoint = viewModel::deleteGate,
                    onSave = viewModel::save,
                    onSelectMarker = viewModel::selectMarker,
                )
            }
            AppVerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}
