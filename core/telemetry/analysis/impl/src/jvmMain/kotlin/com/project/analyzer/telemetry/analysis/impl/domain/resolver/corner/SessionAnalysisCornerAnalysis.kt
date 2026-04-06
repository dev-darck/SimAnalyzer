package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

internal data class SessionAnalysisCornerAnalysis(
    val segmentId: Long,
    val lapNumber: Int,
    val cornerNumber: Int,
    val score: Int,
    val samples: List<SessionAnalysisSample> = emptyList(),
    val representativeSample: SessionAnalysisSample? = null,
    val startTrackPosition: Float,
    val apexTrackPosition: Float,
    val endTrackPosition: Float,
    val brakePointTrackPosition: Float? = null,
    val throttlePickupTrackPosition: Float? = null,
    val coastingRatio: Float = 0f,
    val trailBrakingScore: Int = 100,
    val apexClassification: SessionAnalysisCornerApexClassification = SessionAnalysisCornerApexClassification.GoodApex,
    val entrySpeedKmh: Float? = null,
    val apexSpeedKmh: Float? = null,
    val exitSpeedKmh: Float? = null,
    val entrySpeedDeltaKmh: Float? = null,
    val exitSpeedDeltaKmh: Float? = null,
    val timeLossMs: Int = 0,
    val understeerRatio: Float = 0f,
    val oversteerRatio: Float = 0f,
    val wheelLockup: Boolean = false,
    val wheelSpin: Boolean = false,
    val referenceApexTrackPosition: Float? = null,
)
