package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

internal data class CornerMeasurements(
    val samples: List<SessionAnalysisSample> = emptyList(),
    val representativeSample: SessionAnalysisSample,
    val startTrackPosition: Float,
    val apexTrackPosition: Float,
    val endTrackPosition: Float,
    val brakePointTrackPosition: Float? = null,
    val throttlePickupTrackPosition: Float? = null,
    val coastingRatio: Float = 0f,
    val trailBrakingScore: Int = 100,
    val entrySpeedKmh: Float? = null,
    val apexSpeedKmh: Float? = null,
    val exitSpeedKmh: Float? = null,
    val timeLossMs: Int = 0,
    val understeerRatio: Float = 0f,
    val oversteerRatio: Float = 0f,
    val wheelLockup: Boolean = false,
    val wheelSpin: Boolean = false,
)
