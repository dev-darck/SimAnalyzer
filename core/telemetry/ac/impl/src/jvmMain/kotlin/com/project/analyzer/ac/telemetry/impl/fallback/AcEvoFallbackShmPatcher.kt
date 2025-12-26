package com.project.analyzer.ac.telemetry.impl.fallback

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
) {

    private var syntheticPacketId: Int = 1
    private var lastSessionEpoch: Long = -1L
    private val teleportResetDetector = TeleportResetDetector()

    fun clear() {
        fileInfoExtractor.clear()
        lapAnalyzer.reset()
        syntheticPacketId = 1
        lastSessionEpoch = -1L
        teleportResetDetector.reset()
    }

    fun patchIfNeeded(shm: AcSharedMemory, loopStartNanos: Long) {
        if (!needsFallback(shm)) return

        val info = fileInfoExtractor.poll()

        if (info.sessionEpoch != lastSessionEpoch) {
            lastSessionEpoch = info.sessionEpoch
            lapAnalyzer.resetSession()
        }

        if (teleportResetDetector.update(loopStartNanos, shm.physics)) {
            lapAnalyzer.resetSession()
        }

        val calibration = lapAnalyzer.loadCalibration(info.trackId)
        patchStatics(shm.statics, info)

        lapAnalyzer.processPhysicsFrame(loopStartNanos, shm.physics)
        val snapshot = lapAnalyzer.getSnapshot(loopStartNanos)
        patchGraphics(shm.graphics, snapshot)
    }

    private fun needsFallback(shm: AcSharedMemory): Boolean {
        val g = shm.graphics
        val s = shm.statics

        val graphicsEmpty = (g.packetId == 0)

        val trackBlank = s.track.all { it == ' ' || it == '\u0000' }
        val staticsEmpty = (s.sectorCount == 0 || s.numCars == 0 || trackBlank)

        return graphicsEmpty || staticsEmpty
    }

    private fun patchStatics(statics: SPageFileStatic, info: EvoFileInfo, calibration: TrackCalibration? = null) {
        statics.numCars = 1
        statics.numberOfSessions = 1
        statics.sectorCount = calibration?.sectors?.size ?: 3

        statics.track.writeWString(info.trackName ?: info.trackId.orEmpty())
        statics.carModel.writeWString(info.carModel.orEmpty())
        val driverInfo = info.driverName?.split(" ")
        val name = driverInfo?.getOrNull(0).orEmpty()
        statics.playerName.writeWString(name)
        if ((driverInfo?.lastIndex ?: 0) >= 1) {
            val surname = driverInfo?.subList(1, driverInfo.lastIndex)?.joinToString(" ")
            statics.playerSurname.writeWString(surname.orEmpty())
        }
    }

    private fun patchGraphics(graphics: SPageFileGraphics, snapshot: LapTimingSnapshot) {
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

        graphics.isValidLap = 1
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
