package com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner

internal data class SessionAnalysisCornerReference(
    val cornerNumber: Int,
    val startTrackPosition: Float,
    val apexTrackPosition: Float,
    val endTrackPosition: Float,
    val brakePointTrackPosition: Float? = null,
    val throttlePickupTrackPosition: Float? = null,
    val entrySpeedKmh: Float? = null,
    val apexSpeedKmh: Float? = null,
    val exitSpeedKmh: Float? = null,
)
