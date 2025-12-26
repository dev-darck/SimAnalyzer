package com.project.analyzer.calibration.presentation.state

import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.ReferencePoint

data class CalibrationState(
    val trackName: String = "",
    val trackId: String = "",

    val referencePoint: ReferencePoint = ReferencePoint.CAR_CENTER,
    val triggerRadiusMeters: Float = 6f,
    val debugHalfWidthMeters: Float = 8f,

    val startFinish: Gate? = null,
    val sectorStarts: Map<Int, Gate> = emptyMap(),
    val sectorFinishes: Map<Int, Gate> = emptyMap(),

    val isBusy: Boolean = false,
    val message: String? = null,
    val savedPathHint: String = "Downloads/track calibrations",
    val lastSavedTrackId: String? = null,
) {
    fun isReadyToSave(sectorCount: Int): Boolean {
        if (trackId.isBlank()) return false
        if (startFinish == null) return false
        for (i in 1..sectorCount) {
            if (sectorStarts[i] == null) return false
            if (sectorFinishes[i] == null) return false
        }
        return true
    }

    val canVerify: Boolean get() = !lastSavedTrackId.isNullOrBlank()
}
