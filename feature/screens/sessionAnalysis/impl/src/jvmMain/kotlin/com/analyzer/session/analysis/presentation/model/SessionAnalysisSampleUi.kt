package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisSampleUi(
    val frameId: Long,
    val lapNumber: Int,
    val sectorIndex: Int,
    val sampleIndexInLap: Int,
    val trackPosition: Float? = null,
    val trackX: Float? = null,
    val trackY: Float? = null,
    val elapsedMs: Int? = null,
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
    val handlingState: SessionAnalysisHandlingStateUi = SessionAnalysisHandlingStateUi.Neutral,
    val tyreFl: SessionAnalysisTyreUi? = null,
    val tyreFr: SessionAnalysisTyreUi? = null,
    val tyreRl: SessionAnalysisTyreUi? = null,
    val tyreRr: SessionAnalysisTyreUi? = null,
)
