package com.analyzer.trackmap.presentation.state

import com.analyzer.trackmap.presentation.model.TrackMapEditorGateUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorMarkerUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorPointUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorSectorUi
import com.analyzer.trackmap.presentation.model.TrackMapLibraryItemUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class TrackMapCalibrationEditorState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val item: TrackMapLibraryItemUi? = null,
    val livePosition: TrackMapEditorPointUi? = null,
    val sourceLabel: String = "NEW",
    val gates: ImmutableList<TrackMapEditorGateUi> = persistentListOf(),
    val markers: ImmutableList<TrackMapEditorMarkerUi> = persistentListOf(),
    val sectors: ImmutableList<TrackMapEditorSectorUi> = persistentListOf(),
    val selectedMarkerId: String? = null,
    val message: String? = null,
    val canSave: Boolean = false,
    val canReset: Boolean = false,
)
