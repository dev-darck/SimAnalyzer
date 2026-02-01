package com.project.analyzer.ac.telemetry.impl.fallback.logfile.model

internal data class Parsed(
    val hardBoundary: Boolean = false,
    val gameStarted: Boolean = false,
    val physicsTrackName: String? = null,
    val gameStartedTrackName: String? = null,
    val slugBase: String? = null,
    val slugLayout: String? = null,
    val containerFolder: String? = null,
    val containerLayout: String? = null,
    val carModel: String? = null,
    val carSource: CarSource = CarSource.NONE,
    val driverName: String? = null,
    val driverSteamId: String? = null,
    val penalty: Penalty? = null,
    val sessionType: EvoSessionType? = null
)
