package com.project.analyzer.ac.telemetry.impl.internal.mapper.ace

import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AceRawSnapshot
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoSessionType
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStatus
import com.project.analyzer.telemetry.api.contract.SessionPhase
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.DriverInfo
import com.project.analyzer.telemetry.api.model.session.PitState
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import dev.zacsweers.metro.Inject

@Inject
internal class AcEvoSessionMapper {

    fun enrich(base: SessionFrame?, snapshot: AceRawSnapshot): SessionFrame {
        val graphics = snapshot.graphics
        val physics = snapshot.physics
        val statics = snapshot.statics
        val sessionState = graphics.sessionState
        val instrumentation = graphics.instrumentation
        val baseSession = base ?: SessionFrame()
        val baseTrack = baseSession.track
        val baseCar = baseSession.car
        val baseDriver = baseSession.driver

        return baseSession.copy(
            sessionType = mapSessionType(statics.session).takeIf { it != SessionType.UNKNOWN }
                ?: baseSession.sessionType,
            phase = sessionState.phaseName.takeIf(String::isNotBlank)?.let(::mapPhase) ?: baseSession.phase,
            phaseLabel = sessionState.phaseName.ifBlank { baseSession.phaseLabel },
            sessionName = statics.sessionName.ifBlank { baseSession.sessionName },
            track = (
                baseTrack ?: TrackInfo(
                    trackName = statics.track.takeIf(String::isNotBlank),
                    layoutId = statics.trackConfiguration.takeIf(String::isNotBlank),
                )
                ).copy(
                lengthMeters = resolveTrackLength(
                    existing = baseTrack?.lengthMeters,
                    staticLength = statics.trackLengthM,
                    sessionKm = sessionState.lapLengthKm,
                ),
                trackStatus = sessionState.phaseName.ifBlank { baseTrack?.trackStatus },
            ),
            car = (
                baseCar ?: CarInfo(
                    carModel = graphics.carModel.ifBlank { null },
                    carId = graphics.playerCarStableId,
                )
                ).copy(
                ballast = physics.ballast.takeIf { it.isFinite() && it >= 0f } ?: baseCar?.ballast,
            ),
            driver = (baseDriver ?: DriverInfo()).copy(
                firstName = graphics.driverName.ifBlank { baseDriver?.firstName },
                lastName = graphics.driverSurname.ifBlank { baseDriver?.lastName },
            ),
            sessionTimeLeftSec = sessionState.timeLeftMs.takeIf { it > 0 }?.div(1000f)
                ?: baseSession.sessionTimeLeftSec,
            sessionTimeLeftLabel = sessionState.timeLeft.ifBlank { baseSession.sessionTimeLeftLabel },
            sessionWaitTimeLabel = sessionState.waitTime.ifBlank { baseSession.sessionWaitTimeLabel },
            sessionTimeToNextLabel = sessionState.timeToNextSession.ifBlank { baseSession.sessionTimeToNextLabel },
            completedLaps = graphics.totalLapCount.takeIf { it >= 0 } ?: baseSession.completedLaps,
            plannedLaps = sessionState.totalLap.takeIf { it > 0 } ?: baseSession.plannedLaps,
            position = graphics.currentPos.toInt().takeIf { it > 0 } ?: baseSession.position,
            numberOfSessions = statics.numberOfSessions.takeIf { it > 0 } ?: baseSession.numberOfSessions,
            isOnline = statics.isOnline || baseSession.isOnline == true,
            isTimedRace = statics.isTimedRace || baseSession.isTimedRace == true,
            pit = (baseSession.pit ?: PitState()).copy(
                isInPit = graphics.isInPitBox,
                isInPitLane = graphics.isInPitLane,
                pitLimiterOn = graphics.electronics.isPitLimiterOn,
                serviceDamage = graphics.pitInfo.damage,
                serviceFuel = graphics.pitInfo.fuel,
                serviceTyreFl = graphics.pitInfo.tyresLf,
                serviceTyreFr = graphics.pitInfo.tyresRf,
                serviceTyreRl = graphics.pitInfo.tyresLr,
                serviceTyreRr = graphics.pitInfo.tyresRr,
            ),
            activeCars = maxOf(graphics.activeCars, graphics.totalDrivers.toInt()).takeIf { it > 0 }
                ?: baseSession.activeCars,
            gapAheadMs = secondsToMillis(graphics.gapAhead) ?: baseSession.gapAheadMs,
            gapBehindMs = secondsToMillis(graphics.gapBehind) ?: baseSession.gapBehindMs,
            isSetupMenuVisible = graphics.status != AcEvoStatus.LIVE && sessionState.uiEnableSetup,
            isPaused = graphics.status == AcEvoStatus.PAUSE,
            uiEnableDrive = sessionState.uiEnableDrive,
            uiEnableSetup = sessionState.uiEnableSetup,
            isReadyToNextBlinking = sessionState.isReadyToNextBlinking,
            showWaitingForPlayers = sessionState.showWaitingForPlayers,
            isDisconnectedFromServer = sessionState.disconnectedFromServer,
            restartSeasonEnabled = sessionState.restartSeasonEnabled,
            endSessionFlag = sessionState.endSessionFlag,
            mainDisplayIndex = instrumentation.selectedDisplayIndex.toInt().coerceAtLeast(0),
            secondaryDisplayIndex = instrumentation.currentDisplayPage,
            lightsOn = sessionState.lightsOn,
            lightsMode = sessionState.lightsMode,
            playerPingMs = graphics.playerPing,
            playerLatencyMs = graphics.playerLatency,
            playerCpuUsage = graphics.playerCpuUsage,
            playerCpuUsageAvg = graphics.playerCpuUsageAvg,
            playerQos = graphics.playerQos,
            playerQosAvg = graphics.playerQosAvg,
            playerFps = graphics.playerFps,
            playerFpsAvg = graphics.playerFpsAvg,
        )
    }

    private fun mapPhase(value: String): SessionPhase = when {
        value.contains("green", ignoreCase = true) -> SessionPhase.GREEN_FLAG

        value.contains("start", ignoreCase = true) || value.contains(
            "count",
            ignoreCase = true,
        ) -> SessionPhase.STARTING

        value.contains("over", ignoreCase = true) ||
            value.contains("finish", ignoreCase = true) ||
            value.contains("complete", ignoreCase = true) -> SessionPhase.SESSION_OVER

        else -> SessionPhase.NONE
    }

    private fun mapSessionType(value: AcEvoSessionType): SessionType = when (value) {
        AcEvoSessionType.TIME_ATTACK -> SessionType.TIME_ATTACK

        AcEvoSessionType.RACE -> SessionType.RACE

        AcEvoSessionType.HOT_STINT -> SessionType.HOTLAP

        AcEvoSessionType.CRUISE,
        AcEvoSessionType.UNKNOWN,
        -> SessionType.UNKNOWN
    }

    private fun resolveTrackLength(existing: Float?, staticLength: Float, sessionKm: Float): Float? = when {
        staticLength.isFinite() && staticLength > 0f -> staticLength
        sessionKm.isFinite() && sessionKm > 0f -> sessionKm * 1000f
        existing != null && existing > 0f -> existing
        else -> null
    }

    private fun secondsToMillis(value: Float): Int? =
        if (value.isFinite() && value > 0f) (value * 1000f).toInt() else null
}
