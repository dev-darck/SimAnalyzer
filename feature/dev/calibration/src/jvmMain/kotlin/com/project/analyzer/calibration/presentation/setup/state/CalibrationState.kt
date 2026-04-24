package com.project.analyzer.calibration.presentation.setup.state

import com.project.analyzer.calibration.presentation.model.CalibrationGateUi
import com.project.analyzer.calibration.presentation.model.CalibrationReferencePointUi
import com.project.analyzer.math.Vec2
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal data class CalibrationState(
    val trackName: String = "",
    val trackId: String = "",
    val sessionTrackName: String? = null,
    val sessionTrackId: String? = null,
    val sessionCarModel: String? = null,

    val referencePoint: CalibrationReferencePointUi = CalibrationReferencePointUi.FrontAxle,
    val halfWidthMeters: Float = 4f,

    val startFinish: CalibrationGateUi? = null,
    val sectorStartMarks: PersistentList<CalibrationGateUi> = persistentListOf(),

    val isBusy: Boolean = false,
    val message: String? = null,
    val listOfData: PersistentList<String> = persistentListOf(),
    val lastSavedTrackId: String? = null,
    val debugTelemetry: String? = null,

    val currentPosition: Vec2? = null,
    val currentForward: Vec2? = null,
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f,
) {
    val canVerify: Boolean get() = !lastSavedTrackId.isNullOrBlank()
    val sectorCount: Int get() = 1 + sectorStartMarks.size
}
