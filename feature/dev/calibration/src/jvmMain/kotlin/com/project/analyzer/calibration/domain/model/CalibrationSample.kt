package com.project.analyzer.calibration.domain.model

import com.project.analyzer.math.Pose2D
import com.project.analyzer.math.Vec2

data class CalibrationSample(
    val pose: Pose2D?,
    val speedKmh: Float,
    val headingRad: Float,
    val timestampNs: Long = 0L,
    val wheels: WheelDebug? = null,
    val axleForward: Vec2? = null,
    val trackId: String? = null,
    val trackName: String? = null,
    val carModel: String? = null,
)
