package com.analyzer.trackmap.presentation.state

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorContent
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

internal fun trackMapCalibrationEditorLoadingState(): TrackMapCalibrationEditorState = TrackMapCalibrationEditorState(
    isLoading = true,
)

internal fun TrackMapCalibrationEditorContent.toTrackMapCalibrationEditorState(): TrackMapCalibrationEditorState =
    TrackMapCalibrationEditorState(
        item = item,
        current = snapshot,
        original = snapshot,
        message = message,
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
    )

internal fun TrackMapCalibrationEditorState.toSavedTrackMapCalibrationEditorState(
    snapshot: TrackMapCalibrationEditorSnapshot,
    trackId: String,
): TrackMapCalibrationEditorState {
    val persistedSnapshot = snapshot.copy(source = TrackCalibrationSource.USER)
    return copy(
        isSaving = false,
        current = persistedSnapshot,
        original = persistedSnapshot,
        message = "Saved user override for $trackId",
    )
}

internal fun TrackMapCalibrationEditorState.toSaveFailureTrackMapCalibrationEditorState(
    message: String,
): TrackMapCalibrationEditorState = copy(
    isSaving = false,
    message = message,
)
