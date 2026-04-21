package com.project.analyzer.telemetry.api.model.session

import com.project.analyzer.telemetry.api.contract.SessionPhase
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.SimStatus

public data class SessionFrame(
    val status: SimStatus? = null,
    val sessionType: SessionType? = null,
    val phase: SessionPhase? = null,
    val phaseLabel: String? = null,
    val sessionName: String? = null,

    val track: TrackInfo? = null,
    val car: CarInfo? = null,
    val driver: DriverInfo? = null,

    val sessionTimeLeftSec: Float? = null,
    val sessionTimeElapsedSec: Float? = null,
    val sessionTimeLeftLabel: String? = null,
    val sessionWaitTimeLabel: String? = null,
    val sessionTimeToNextLabel: String? = null,

    val completedLaps: Int? = null,
    val plannedLaps: Int? = null, // numberOfLaps
    val position: Int? = null,

    val flags: Flags? = null,
    val penalty: Penalty? = null,
    val pit: PitState? = null,

    val sessionIndex: Int? = null,
    val numberOfSessions: Int? = null,
    val isAiControlled: Boolean? = null,
    val isOnline: Boolean? = null,
    val isTimedRace: Boolean? = null,
    val hasExtraLap: Boolean? = null,

    // Active cars info
    val activeCars: Int? = null,
    val numCars: Int? = null,

    // Gaps (for multiplayer)
    val gapAheadMs: Int? = null,
    val gapBehindMs: Int? = null,

    // Setup menu
    val isSetupMenuVisible: Boolean? = null,
    val isPaused: Boolean? = null,
    val uiEnableDrive: Boolean? = null,
    val uiEnableSetup: Boolean? = null,
    val isReadyToNextBlinking: Boolean? = null,
    val showWaitingForPlayers: Boolean? = null,
    val isDisconnectedFromServer: Boolean? = null,
    val restartSeasonEnabled: Boolean? = null,
    val endSessionFlag: Int? = null,

    // Display indices
    val mainDisplayIndex: Int? = null,
    val secondaryDisplayIndex: Int? = null,
    val lightsOn: Int? = null,
    val lightsMode: Int? = null,

    // Client/session quality
    val playerPingMs: Int? = null,
    val playerLatencyMs: Int? = null,
    val playerCpuUsage: Int? = null,
    val playerCpuUsageAvg: Int? = null,
    val playerQos: Int? = null,
    val playerQosAvg: Int? = null,
    val playerFps: Int? = null,
    val playerFpsAvg: Int? = null,
)
