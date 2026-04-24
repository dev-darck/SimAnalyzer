package com.analyzer.trackmap.presentation.state

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorContent
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.presentation.model.TrackMapLibraryItemUi
import com.analyzer.trackmap.presentation.model.toSourceLabel
import com.analyzer.trackmap.presentation.model.toTrackMapEditorGateUi
import com.analyzer.trackmap.presentation.model.toTrackMapEditorMarkerUi
import com.analyzer.trackmap.presentation.model.toTrackMapEditorSectorUi
import com.analyzer.trackmap.presentation.model.toUi
import kotlinx.collections.immutable.toImmutableList

internal fun trackMapCalibrationEditorLoadingState(): TrackMapCalibrationEditorState = TrackMapCalibrationEditorState(
    isLoading = true,
)

internal fun TrackMapCalibrationEditorContent.toTrackMapCalibrationEditorState(): TrackMapCalibrationEditorState =
    snapshot.toTrackMapCalibrationEditorState(
        item = item.toUi(),
        message = message,
        canReset = false,
    )

internal fun trackMapCalibrationEditorNotFoundState(trackId: String): TrackMapCalibrationEditorState =
    TrackMapCalibrationEditorState(
        message = "Track map not found for $trackId",
    )

internal fun trackMapCalibrationEditorLoadFailureState(message: String): TrackMapCalibrationEditorState =
    TrackMapCalibrationEditorState(
        message = message,
    )

internal fun TrackMapCalibrationEditorState.toSavingTrackMapCalibrationEditorState(): TrackMapCalibrationEditorState =
    copy(
        isSaving = true,
        message = "Saving calibration...",
        canSave = false,
        canReset = false,
    )

internal fun TrackMapCalibrationEditorState.toSavedTrackMapCalibrationEditorState(
    snapshot: TrackMapCalibrationEditorSnapshot,
    trackId: String,
): TrackMapCalibrationEditorState {
    val item = requireNotNull(item)
    return snapshot.toTrackMapCalibrationEditorState(
        item = item,
        livePosition = livePosition,
        message = "Saved user override for $trackId",
        canReset = false,
    )
}

internal fun TrackMapCalibrationEditorState.toSaveFailureTrackMapCalibrationEditorState(
    message: String,
): TrackMapCalibrationEditorState = copy(
    isSaving = false,
    message = message,
    canSave = item != null && gates.isNotEmpty(),
)

internal fun TrackMapCalibrationEditorSnapshot.toTrackMapCalibrationEditorState(
    item: TrackMapLibraryItemUi,
    livePosition: com.analyzer.trackmap.presentation.model.TrackMapEditorPointUi? = null,
    message: String? = null,
    canReset: Boolean,
    isSaving: Boolean = false,
): TrackMapCalibrationEditorState = TrackMapCalibrationEditorState(
    isLoading = false,
    isSaving = isSaving,
    item = item,
    livePosition = livePosition,
    sourceLabel = toSourceLabel(),
    gates = gates.map { it.toTrackMapEditorGateUi() }.toImmutableList(),
    markers = markers.map { it.toTrackMapEditorMarkerUi() }.toImmutableList(),
    sectors = sectors.map { it.toTrackMapEditorSectorUi() }.toImmutableList(),
    selectedMarkerId = selectedMarkerId,
    message = message,
    canSave = !isSaving && gates.isNotEmpty(),
    canReset = !isSaving && canReset,
)
