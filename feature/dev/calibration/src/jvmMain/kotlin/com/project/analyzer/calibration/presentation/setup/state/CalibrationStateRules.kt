package com.project.analyzer.calibration.presentation.setup.state

import com.project.analyzer.calibration.presentation.model.CalibrationGateUi

internal fun CalibrationState.sectorStart(index: Int): CalibrationGateUi? = when (index) {
    1 -> startFinish
    else -> sectorStartMarks.getOrNull(index - 2)
}

internal fun CalibrationState.sectorFinish(index: Int): CalibrationGateUi? = when {
    sectorCount == 1 -> startFinish
    index < sectorCount -> sectorStart(index + 1)
    index == sectorCount -> startFinish
    else -> null
}

internal fun CalibrationState.isReadyToSave(): Boolean {
    if (trackId.isBlank()) return false
    if (startFinish == null) return false
    return true
}
