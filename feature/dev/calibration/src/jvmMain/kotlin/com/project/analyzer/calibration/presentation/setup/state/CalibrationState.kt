package com.project.analyzer.calibration.presentation.setup.state

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

data class CalibrationState(
    val trackName: String = "",
    val trackId: String = "",
    val sessionTrackName: String? = null,
    val sessionTrackId: String? = null,
    val sessionCarModel: String? = null,

    val referencePoint: ReferencePoint = ReferencePoint.FRONT_AXLE,
    val halfWidthMeters: Float = 4f,

    val startFinish: Gate? = null,
    val sectorStartMarks: List<Gate> = emptyList(),

    val isBusy: Boolean = false,
    val message: String? = null,
    val listOfData: List<String> = emptyList(),
    val lastSavedTrackId: String? = null,
    val debugTelemetry: String? = null,

    val currentPosition: Vec2? = null,
    val currentForward: Vec2? = null,
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f
) {

    val canVerify: Boolean get() = !lastSavedTrackId.isNullOrBlank()
    val sectorCount: Int get() = 1 + sectorStartMarks.size

    fun sectorStart(index: Int): Gate? = when (index) {
        1 -> startFinish
        else -> sectorStartMarks.getOrNull(index - 2)
    }

    fun sectorFinish(index: Int): Gate? = when {
        sectorCount == 1 -> startFinish
        index < sectorCount -> sectorStart(index + 1)
        index == sectorCount -> startFinish
        else -> null
    }

    fun isReadyToSave(): Boolean {
        if (trackId.isBlank()) return false
        if (startFinish == null) return false
        return true
    }
}
