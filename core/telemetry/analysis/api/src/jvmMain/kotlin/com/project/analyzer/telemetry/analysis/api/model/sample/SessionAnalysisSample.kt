package com.project.analyzer.telemetry.analysis.api.model.sample

import com.project.analyzer.telemetry.analysis.api.model.handling.SessionAnalysisHandlingState
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreState

public data class SessionAnalysisSample(
    val segmentId: Long = 0L,
    val frameId: Long = 0L,
    val timestampNs: Long = 0L,
    val lapNumber: Int = 0,
    val sectorIndex: Int = -1,
    val flags: Int = 0,
    val sampleIndexInLap: Int = 0,
    val trackPosition: Float? = null,
    val trackX: Float? = null,
    val trackY: Float? = null,
    val headingRad: Float? = null,
    val speedKmh: Float? = null,
    val gear: Int? = null,
    val rpm: Float? = null,
    val throttle: Float? = null,
    val brake: Float? = null,
    val steeringAngleRad: Float? = null,
    val lateralG: Float? = null,
    val yawRateRad: Float? = null,
    val deltaToBestMs: Int? = null,
    val fuelLiters: Float? = null,
    val fuelCapacityLiters: Float? = null,
    val handlingState: SessionAnalysisHandlingState = SessionAnalysisHandlingState.Neutral,
    val tyreFl: SessionAnalysisTyreState? = null,
    val tyreFr: SessionAnalysisTyreState? = null,
    val tyreRl: SessionAnalysisTyreState? = null,
    val tyreRr: SessionAnalysisTyreState? = null,
)
