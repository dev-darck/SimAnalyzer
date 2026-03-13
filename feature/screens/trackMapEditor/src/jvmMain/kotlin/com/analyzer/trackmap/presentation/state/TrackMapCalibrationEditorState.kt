package com.analyzer.trackmap.presentation.state

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorGate
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSector
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

data class TrackMapCalibrationEditorState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val item: TrackMapLibraryItem? = null,
    val livePosition: Vec2? = null,
    val current: TrackMapCalibrationEditorSnapshot = TrackMapCalibrationEditorSnapshot(
        referencePoint = ReferencePoint.FRONT_AXLE,
        source = null,
        gates = emptyList(),
        markers = emptyList(),
        sectors = emptyList(),
        selectedMarkerId = null,
    ),
    val original: TrackMapCalibrationEditorSnapshot = TrackMapCalibrationEditorSnapshot(
        referencePoint = ReferencePoint.FRONT_AXLE,
        source = null,
        gates = emptyList(),
        markers = emptyList(),
        sectors = emptyList(),
        selectedMarkerId = null,
    ),
    val message: String? = null,
) {

    val source: TrackCalibrationSource?
        get() = current.source

    val gates: List<TrackMapCalibrationEditorGate>
        get() = current.gates

    val markers: List<TrackMapCalibrationEditorMarker>
        get() = current.markers

    val sectors: List<TrackMapCalibrationEditorSector>
        get() = current.sectors

    val selectedMarkerId: String?
        get() = current.selectedMarkerId

    val canSave: Boolean
        get() = item != null && !isLoading && !isSaving && gates.isNotEmpty()

    val canReset: Boolean
        get() = item != null && !isLoading && !isSaving && current != original
}
