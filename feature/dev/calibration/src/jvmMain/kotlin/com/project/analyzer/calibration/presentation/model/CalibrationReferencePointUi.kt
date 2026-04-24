package com.project.analyzer.calibration.presentation.model

import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

internal enum class CalibrationReferencePointUi {
    CarCenter,
    FrontAxle,
    RearAxle,
}

internal fun CalibrationReferencePointUi.toDomain(): ReferencePoint = when (this) {
    CalibrationReferencePointUi.CarCenter -> ReferencePoint.CAR_CENTER
    CalibrationReferencePointUi.FrontAxle -> ReferencePoint.FRONT_AXLE
    CalibrationReferencePointUi.RearAxle -> ReferencePoint.REAR_AXLE
}

internal fun ReferencePoint.toUi(): CalibrationReferencePointUi = when (this) {
    ReferencePoint.CAR_CENTER -> CalibrationReferencePointUi.CarCenter
    ReferencePoint.FRONT_AXLE -> CalibrationReferencePointUi.FrontAxle
    ReferencePoint.REAR_AXLE -> CalibrationReferencePointUi.RearAxle
}
