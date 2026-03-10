package com.project.analyzer.telemetry.ac.api.model.calibration

import kotlinx.serialization.Serializable

@Serializable
public enum class ReferencePoint {

    CAR_CENTER,
    FRONT_AXLE,
    REAR_AXLE,
}

@Serializable
public enum class TrackCalibrationSource {

    USER,
    GAME,
}

@Serializable
public data class SectorCalibration(val index: Int, val start: Gate, val finish: Gate)

@Serializable
public data class TrackCalibration(
    val trackId: String,
    val trackName: String,
    val layoutId: String? = null,
    val createdAtEpochMs: Long,
    val source: TrackCalibrationSource = TrackCalibrationSource.USER,
    val referencePoint: ReferencePoint = ReferencePoint.FRONT_AXLE,
    val startFinish: Gate,
    val sectors: List<SectorCalibration>,
)
