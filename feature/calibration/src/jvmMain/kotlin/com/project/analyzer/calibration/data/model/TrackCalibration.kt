package com.project.analyzer.calibration.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TrackCalibration(
    val trackId: String,
    val trackName: String,
    val createdAtEpochMs: Long,
    val referencePoint: ReferencePoint,

    val startFinish: Gate,
    val sectors: List<SectorCalibration>,
)

@Serializable
data class SectorCalibration(
    val index: Int,
    val start: Gate,
    val finish: Gate,
)
