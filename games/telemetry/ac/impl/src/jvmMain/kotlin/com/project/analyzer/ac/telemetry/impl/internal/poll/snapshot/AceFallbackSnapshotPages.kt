package com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot

import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoSessionType
import com.project.analyzer.ac.telemetry.impl.shm.ace.structure.AcEvoStatus
import com.project.analyzer.utils.shm.writeWString
import com.sun.jna.Memory

internal class AceFallbackSnapshotPages {

    private val graphicsMemory: Memory = Memory(SPageFileGraphics().size().toLong())
    private val staticsMemory: Memory = Memory(SPageFileStatic().size().toLong())

    val graphics: SPageFileGraphics = SPageFileGraphics().apply { attach(graphicsMemory) }
    val statics: SPageFileStatic = SPageFileStatic().apply { attach(staticsMemory) }

    fun seedFrom(snapshot: AceRawSnapshot) {
        graphicsMemory.clear()
        staticsMemory.clear()
        graphics.read()
        statics.read()

        val rawGraphics = snapshot.graphics
        val rawStatics = snapshot.statics
        val sessionState = rawGraphics.sessionState

        graphics.packetId = rawGraphics.packetId
        graphics.status = rawGraphics.status.toLegacyStatus()
        graphics.session = rawStatics.session.toLegacySession()
        graphics.completedLaps = rawGraphics.totalLapCount.coerceAtLeast(0)
        graphics.position = rawGraphics.currentPos.toInt().coerceAtLeast(0)
        graphics.iCurrentTime = rawGraphics.currentLapTimeMs.coerceAtLeast(0)
        graphics.iLastTime = rawGraphics.lastLaptimeMs.coerceAtLeast(0)
        graphics.iBestTime = rawGraphics.bestLaptimeMs.coerceAtLeast(0)
        graphics.sessionTimeLeft = (sessionState.timeLeftMs.coerceAtLeast(0) / 1000f)
        graphics.distanceTraveled = (rawGraphics.currentKm.coerceAtLeast(0f) * 1000f)
        graphics.numberOfLaps = sessionState.totalLap.coerceAtLeast(0)
        graphics.activeCars = maxOf(rawGraphics.activeCars, rawGraphics.totalDrivers.toInt()).coerceAtLeast(0)
        graphics.playerCarID = rawGraphics.playerCarStableId
        graphics.penaltyTime = rawGraphics.raceCutGainedTimeMs.coerceAtLeast(0) / 1000f
        graphics.normalizedCarPosition = rawGraphics.npos
        graphics.isInPit = if (rawGraphics.isInPitBox) 1 else 0
        graphics.isInPitLane = if (rawGraphics.isInPitLane) 1 else 0
        graphics.currentSectorIndex = -1
        graphics.lastSectorTime = 0
        graphics.sessionIndex = ((rawStatics.eventId and 0xFF) shl 8) or (rawStatics.sessionId and 0xFF)
        graphics.isSetupMenuVisible = if (rawGraphics.status != AcEvoStatus.LIVE && sessionState.uiEnableSetup) 1 else 0
        graphics.isValidLap = if (rawGraphics.isValidLap) 1 else 0
        graphics.fuelXLap =
            rawGraphics.fuelPerLap.takeIf { it > 0f } ?: rawGraphics.fuelLiterPerLap.takeIf { it > 0f } ?: 0f
        graphics.fuelEstimatedLaps =
            rawGraphics.fuelEstimatedLaps.takeIf { it > 0f } ?: rawGraphics.lapsPossibleWithFuel.takeIf { it > 0f }
                ?: 0f

        statics.smVersion.writeWString(rawStatics.smVersion)
        statics.acVersion.writeWString(rawStatics.acEvoVersion)
        statics.track.writeWString(rawStatics.track)
        statics.trackConfiguration.writeWString(rawStatics.trackConfiguration)
        statics.carModel.writeWString(rawGraphics.carModel)
        statics.playerName.writeWString(rawGraphics.driverName)
        statics.playerSurname.writeWString(rawGraphics.driverSurname)
        statics.numCars = graphics.activeCars.coerceAtLeast(1)
        statics.numberOfSessions = rawStatics.numberOfSessions.coerceAtLeast(1)
        statics.maxRpm = snapshot.physics.currentMaxRPM
            .takeIf { it.isFinite() && it > 0f }
            ?.toInt()
            ?: 0
        statics.maxFuel = rawGraphics.maxFuel.takeIf { it.isFinite() && it > 0f } ?: 0f
        statics.maxTurboBoost = rawGraphics.maxTurboBoost.takeIf { it.isFinite() && it > 0f } ?: 0f
        statics.trackSPlineLength = rawStatics.trackLengthM.takeIf { it.isFinite() && it > 0f } ?: 0f
        statics.isTimedRace = if (rawStatics.isTimedRace) 1 else 0
        statics.isOnline = if (rawStatics.isOnline) 1 else 0
        statics.hasKERS = if (rawGraphics.hasKers) 1 else 0
        statics.sectorCount = 0
    }

    private fun AcEvoStatus?.toLegacyStatus(): Int = when (this) {
        AcEvoStatus.REPLAY -> 1

        AcEvoStatus.LIVE -> 2

        AcEvoStatus.PAUSE -> 3

        AcEvoStatus.OFF,
        null,
        -> 0
    }

    private fun AcEvoSessionType.toLegacySession(): Int = when (this) {
        AcEvoSessionType.RACE -> 2

        AcEvoSessionType.HOT_STINT -> 3

        AcEvoSessionType.TIME_ATTACK -> 4

        AcEvoSessionType.CRUISE,
        AcEvoSessionType.UNKNOWN,
        -> -1
    }
}
