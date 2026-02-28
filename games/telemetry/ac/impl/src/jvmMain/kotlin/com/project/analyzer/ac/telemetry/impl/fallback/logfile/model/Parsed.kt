package com.project.analyzer.ac.telemetry.impl.fallback.logfile.model

internal enum class SessionTypeSource {
    NONE,
    GOTO_LOADING_PAGE,
    SELECTED_SESSION,
    REMOTE_CREATED,
    GAME_STARTED,
}

internal data class Parsed(
    val hardBoundary: Boolean = false,
    val hardBoundaryTimestampMs: Long? = null,
    val gameStarted: Boolean = false,
    val gameStartedTimestampMs: Long? = null,
    val mainMenuEntered: Boolean = false,
    val mainMenuTimestampMs: Long? = null,
    val physicsTrackName: String? = null,
    val gameStartedTrackName: String? = null,
    val slugBase: String? = null,
    val slugLayout: String? = null,
    val containerFolder: String? = null,
    val containerLayout: String? = null,
    val dynamicTrackFolder: String? = null,
    val dynamicTrackLayout: String? = null,
    val layoutFileFolder: String? = null,
    val layoutFileLayout: String? = null,
    val carModel: String? = null,
    val carSource: CarSource = CarSource.NONE,
    val driverName: String? = null,
    val driverSteamId: String? = null,
    val penalty: Penalty? = null,
    val sessionType: EvoSessionType? = null,
    val sessionTypeSource: SessionTypeSource = SessionTypeSource.NONE,
)
