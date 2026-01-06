package com.project.analyzer.telemetry.ac.api.model.opponents

import com.project.analyzer.math.Vec3

public data class OpponentFrame(
    val carIndex: Int,
    val carModelId: String? = null,
    val driverName: String? = null,

    val position: Int? = null,
    val gapToPlayerMs: Int? = null,

    val lastLapTimeMs: Int? = null,
    val bestLapTimeMs: Int? = null,

    val isInPit: Boolean? = null,

    val worldPosition: Vec3? = null,
    val normalizedLapPosition: Float? = null,
)
