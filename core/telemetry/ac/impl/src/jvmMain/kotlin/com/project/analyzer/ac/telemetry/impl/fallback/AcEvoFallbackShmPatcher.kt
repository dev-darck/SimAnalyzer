package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject

@Inject
class AcEvoFallbackShmPatcher(
    private val fileInfoExtractor: AcEvoFileInfoExtractor,
    private val lapAnalyzer: FallbackLapAnalyzer,
    private val fuelAnalyzer: FallbackFuelAnalyzer,
) {

    private var syntheticPacketId: Int = 1
    private var lastSessionEpoch: Long = -1L
    private val teleportResetDetector = TeleportResetDetector()
    private var lastPenaltyTimestamp: String? = null

    fun clear() {
        fileInfoExtractor.clear()
        lapAnalyzer.reset()
        fuelAnalyzer.reset()
        syntheticPacketId = 1
        lastSessionEpoch = -1L
        teleportResetDetector.reset()
        lastPenaltyTimestamp = null
    }

    fun patchIfNeeded(shm: AcSharedMemory, loopStartNanos: Long) {
        if (!needsFallback(shm)) return

        val info = fileInfoExtractor.poll()

        if (info.sessionEpoch != lastSessionEpoch) {
            lastSessionEpoch = info.sessionEpoch
            lapAnalyzer.resetSession()
            fuelAnalyzer.reset()
            lastPenaltyTimestamp = null
        }

        if (teleportResetDetector.update(loopStartNanos, shm.physics)) {
            lapAnalyzer.resetSession()
            fuelAnalyzer.reset()
        }

        if (info.penaltyTimestamp != null && info.penaltyTimestamp != lastPenaltyTimestamp) {
            lapAnalyzer.onPenaltyDetected()
            lastPenaltyTimestamp = info.penaltyTimestamp
            fileInfoExtractor.clearPenalty()
        }

        val calibration = lapAnalyzer.loadCalibration(info.trackId)
        patchStatics(shm.statics, info, calibration)

        lapAnalyzer.processPhysicsFrame(loopStartNanos, shm.physics)

        val lapSnapshot = lapAnalyzer.getSnapshot(loopStartNanos)

        fuelAnalyzer.processFrame(
            fuelLiters = shm.physics.fuel,
            completedLaps = lapSnapshot.completedLapsCount,
            lastLapTimeMs = lapSnapshot.lastLapTimeMs ?: 0,
            startFinishSyncId = lapSnapshot.startFinishSyncId
        )
        val fuelSnapshot = fuelAnalyzer.getSnapshot(currentFuelLiters = shm.physics.fuel)

        patchGraphics(shm.graphics, lapSnapshot, fuelSnapshot)
    }

    private fun needsFallback(shm: AcSharedMemory): Boolean {
        val g = shm.graphics
        val s = shm.statics

        val graphicsEmpty = (g.packetId == 0)

        val trackBlank = s.track.all { it == ' ' || it == '\u0000' }
        val staticsEmpty = (s.sectorCount == 0 || s.numCars == 0 || trackBlank)

        return graphicsEmpty || staticsEmpty
    }

    private fun patchStatics(
        statics: SPageFileStatic,
        info: EvoFileInfo,
        calibration: TrackCalibration? = null
    ) {
        statics.numCars = 1
        statics.numberOfSessions = 1
        val totalSectors = (calibration?.sectors?.size?.plus(1) ?: 3).coerceAtLeast(1)
        statics.sectorCount = totalSectors

        statics.track.writeWString(info.trackName ?: info.trackId.orEmpty())
        statics.carModel.writeWString(info.carModel.orEmpty())
        val driverInfo = info.driverName?.trim()?.split(" ")?.filter { it.isNotBlank() }
        val name = driverInfo?.getOrNull(0).orEmpty()
        statics.playerName.writeWString(name)
        val surname = driverInfo?.drop(1)?.joinToString(" ").orEmpty()
        statics.playerSurname.writeWString(surname)
    }

    private fun patchGraphics(
        graphics: SPageFileGraphics,
        snapshot: LapTimingSnapshot,
        fuelSnapshot: FuelSnapshot,
    ) {
        graphics.packetId = syntheticPacketId++

        graphics.status = 2
        graphics.session = 1

        graphics.completedLaps = snapshot.completedLapsCount

        graphics.iCurrentTime = snapshot.currentLapTimeMs
        graphics.iLastTime = snapshot.lastLapTimeMs ?: 0
        graphics.iBestTime = snapshot.bestLapTimeMs ?: 0

        graphics.currentSectorIndex = snapshot.currentSectorIndex
        graphics.lastSectorTime = snapshot.lastSectorTimeMs ?: 0

        graphics.iSplit = snapshot.currentSectorTimeMs

        graphics.isValidLap = if (snapshot.currentLapValid) 1 else 0

        graphics.iDeltaLapTime = snapshot.deltaLapTimeMs ?: 0
        graphics.isDeltaPositive = if (snapshot.isDeltaPositive) 1 else 0

        fuelSnapshot.fuelPerLapLiters?.let { graphics.fuelXLap = it }
        fuelSnapshot.fuelEstimatedLaps?.let { graphics.fuelEstimatedLaps = it }
    }
}

private fun CharArray.writeWString(value: String) {
    for (i in indices) this[i] = '\u0000'
    val n = minOf(size - 1, value.length)
    for (i in 0 until n) this[i] = value[i]
    this[n] = '\u0000'
}

private class TeleportResetDetector {

    private var hasPrev = false
    private var prevX = 0f
    private var prevZ = 0f
    private var lastResetNs = 0L

    fun reset() {
        hasPrev = false
        prevX = 0f
        prevZ = 0f
        lastResetNs = 0L
    }

    fun update(nowNs: Long, physics: SPageFilePhysics): Boolean {
        val a = physics.tyreContactPoint
        if (a.size < 6) return false

        val x = (a[0] + a[3]) * 0.5f
        val z = (a[2] + a[5]) * 0.5f

        if (!hasPrev) {
            hasPrev = true
            prevX = x
            prevZ = z
            return false
        }

        val dx = x - prevX
        val dz = z - prevZ
        val dist2 = dx * dx + dz * dz

        prevX = x
        prevZ = z

        val teleported = dist2 > (80f * 80f) && physics.speedKmh < 5f
        val cooldownOk = (nowNs - lastResetNs) > 2_000_000_000L

        if (teleported && cooldownOk) {
            lastResetNs = nowNs
            return true
        }
        return false
    }
}
