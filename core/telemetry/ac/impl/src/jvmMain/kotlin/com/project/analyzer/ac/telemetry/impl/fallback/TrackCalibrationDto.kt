package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.telemetry.ac.api.model.math.Vec2
import kotlinx.serialization.Serializable

@Serializable
data class Vec2Dto(val x: Float, val y: Float)

@Serializable
data class GateDto(
    val center: Vec2Dto,
    val forward: Vec2Dto,
    val normal: Vec2Dto,
    val triggerRadiusMeters: Float = 25f,
    val debugHalfWidthMeters: Float = 30f,
)

@Serializable
data class SectorDto(
    val index: Int,
    val start: GateDto,
    val finish: GateDto,
)

@Serializable
data class TrackCalibrationDto(
    val trackId: String,
    val trackName: String,
    val createdAtEpochMs: Long,
    val referencePoint: String,
    val startFinish: GateDto,
    val sectors: List<SectorDto>,
)

internal fun Vec2Dto.v2() = Vec2(x, y)
internal fun GateDto.centerV2() = center.v2()
internal fun GateDto.forwardV2() = forward.v2()
internal fun GateDto.normalV2() = normal.v2()
