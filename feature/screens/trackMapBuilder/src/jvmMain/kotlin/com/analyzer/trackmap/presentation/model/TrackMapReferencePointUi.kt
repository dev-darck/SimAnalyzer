package com.analyzer.trackmap.presentation.model

import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint

internal enum class TrackMapReferencePointUi {
    CarCenter,
    FrontAxle,
    RearAxle,
}

internal fun ReferencePoint.toUi(): TrackMapReferencePointUi = when (this) {
    ReferencePoint.CAR_CENTER -> TrackMapReferencePointUi.CarCenter
    ReferencePoint.FRONT_AXLE -> TrackMapReferencePointUi.FrontAxle
    ReferencePoint.REAR_AXLE -> TrackMapReferencePointUi.RearAxle
}

internal fun TrackMapReferencePointUi.toDomain(): ReferencePoint = when (this) {
    TrackMapReferencePointUi.CarCenter -> ReferencePoint.CAR_CENTER
    TrackMapReferencePointUi.FrontAxle -> ReferencePoint.FRONT_AXLE
    TrackMapReferencePointUi.RearAxle -> ReferencePoint.REAR_AXLE
}
