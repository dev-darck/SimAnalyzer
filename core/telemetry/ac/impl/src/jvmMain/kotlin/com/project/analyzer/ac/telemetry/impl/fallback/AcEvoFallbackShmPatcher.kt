package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.AcEvoFileInfoExtractor
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.pose.PhysicsPoseExtractor
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import kotlin.math.abs

@Inject
class AcEvoFallbackShmPatcher(
    private val fileInfoExtractor: AcEvoFileInfoExtractor,
    private val lapAnalyzer: FallbackLapAnalyzer,
    private val fuelAnalyzer: FallbackFuelAnalyzer,
) {

    private var syntheticPacketId: Int = 1
    private var lastSessionEpoch: Long = -1L
    private val identityResetDetector = IdentityResetDetector()
    private val seenPenaltyIds = ArrayDeque<String>(PENALTY_DEDUP_CAPACITY)
    private val seenPenaltySet = HashSet<String>(PENALTY_DEDUP_CAPACITY * 2)
    private var lastPatchedTrackId: String = ""
    private var lastPatchedCarModel: String = ""
    private var lastPatchedIdentityLog: String = ""
    private var lastPatchedIdentityLogMs: Long = 0L

    private var lastGameState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var lastProcessedPhysicsPacketId: Int = -1
    private val poseExtractor = PhysicsPoseExtractor()
    private val respawnDetector = RespawnResetDetector()

    fun clear() {
        fileInfoExtractor.clear()
        lapAnalyzer.reset()
        fuelAnalyzer.reset()

        syntheticPacketId = 1
        lastSessionEpoch = -1L

        identityResetDetector.reset()

        clearPenaltyDedup()

        lastPatchedTrackId = ""
        lastPatchedCarModel = ""
        lastPatchedIdentityLog = ""
        lastPatchedIdentityLogMs = 0L

        lastGameState = GameConnectionState.DISCONNECTED
        lastProcessedPhysicsPacketId = -1
        respawnDetector.reset()
    }

    fun patchIfNeeded(shm: AcSharedMemory, loopStartNanos: Long, gameState: GameConnectionState) {
        if (!needsFallback(shm)) return

        val info = fileInfoExtractor.poll()

        if (info.sessionEpoch != lastSessionEpoch) {
            lastSessionEpoch = info.sessionEpoch
            resetAll(reason = "sessionEpoch changed", clearTrack = true, clearCar = true)
            identityResetDetector.observe(info, loopStartNanos)
        }

        val identityChange = identityResetDetector.update(info, loopStartNanos)
        if (identityChange != IdentityChange.NONE) {
            val clearTrack = identityChange == IdentityChange.TRACK || identityChange == IdentityChange.BOTH
            val clearCar = identityChange == IdentityChange.CAR || identityChange == IdentityChange.BOTH
            resetAll(reason = "identity changed: $identityChange", clearTrack = clearTrack, clearCar = clearCar)
            identityResetDetector.observe(info, loopStartNanos)
        }

        if (gameState != lastGameState) {
            if (lastGameState == GameConnectionState.IN_MENU && gameState == GameConnectionState.IN_SESSION) {
                lapAnalyzer.rebasePose(loopStartNanos)
                respawnDetector.onResumed(loopStartNanos, shm.physics.packetId)
                lastProcessedPhysicsPacketId = -1
            }
            lastGameState = gameState
        }

        val penaltyId = info.penaltyId
        if (penaltyId != null) {
            if (!seenPenaltySet.contains(penaltyId)) {
                lapAnalyzer.onPenaltyDetected()
                markPenaltySeen(penaltyId)
            }
            fileInfoExtractor.clearPenalty()
        }

        val calibration = lapAnalyzer.loadCalibration(info.trackId)
        patchStatics(shm.statics, info, calibration)

        if (gameState != GameConnectionState.IN_SESSION) return

        val physicsPacketId = shm.physics.packetId
        if (physicsPacketId <= 0) return
        if (physicsPacketId == lastProcessedPhysicsPacketId) return
        lastProcessedPhysicsPacketId = physicsPacketId

        if (calibration != null && lapAnalyzer.isSyncedToStartFinish()) {
            val pose = poseExtractor.extract(shm.physics, calibration.referencePoint)
            if (pose != null) {
                val shouldSoftReset = respawnDetector.update(
                    nowNs = loopStartNanos,
                    physicsPacketId = physicsPacketId,
                    speedKmh = shm.physics.speedKmh,
                    tyreContactPoint = shm.physics.tyreContactPoint,
                    position = pose.position
                )
                if (shouldSoftReset) {
                    logger.info { "FallbackSHM soft reset: respawn detected (synced session)" }

                    fuelAnalyzer.resetLapTracking(
                        fuelLiters = shm.physics.fuel,
                        completedLaps = lapAnalyzer.getCompletedLapsCount(),
                        startFinishSyncId = lapAnalyzer.getStartFinishSyncId(),
                    )
                    lapAnalyzer.softResetAfterRespawn(loopStartNanos)

                    clearPenaltyDedup()
                }
            }
        }

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

    private fun resetAll(reason: String, clearTrack: Boolean, clearCar: Boolean) {
        logger.info { "FallbackSHM reset: $reason" }

        lapAnalyzer.resetSession()
        fuelAnalyzer.reset()
        clearPenaltyDedup()

        if (clearTrack) lastPatchedTrackId = ""
        if (clearCar) lastPatchedCarModel = ""

        lastPatchedIdentityLog = ""
        lastPatchedIdentityLogMs = 0L
    }

    private fun markPenaltySeen(id: String) {
        if (seenPenaltySet.add(id)) {
            seenPenaltyIds.addLast(id)
            while (seenPenaltyIds.size > PENALTY_DEDUP_CAPACITY) {
                val removed = seenPenaltyIds.removeFirst()
                seenPenaltySet.remove(removed)
            }
        }
    }

    private fun clearPenaltyDedup() {
        seenPenaltyIds.clear()
        seenPenaltySet.clear()
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

        val trackId = info.trackId?.trim().orEmpty()
        val carModel = info.carModel?.trim().orEmpty()

        val effectiveTrackId = when {
            trackId.isNotBlank() -> trackId.also { lastPatchedTrackId = it }
            lastPatchedTrackId.isNotBlank() -> lastPatchedTrackId
            else -> info.trackName.orEmpty()
        }

        val effectiveCarModel = when {
            carModel.isNotBlank() -> carModel.also { lastPatchedCarModel = it }
            lastPatchedCarModel.isNotBlank() -> lastPatchedCarModel
            else -> ""
        }

        val identMsg = "FallbackSHM identity: track=$effectiveTrackId car=$effectiveCarModel epoch=${info.sessionEpoch}"
        val nowMs = System.currentTimeMillis()
        if (identMsg != lastPatchedIdentityLog && (nowMs - lastPatchedIdentityLogMs) > 500) {
            lastPatchedIdentityLog = identMsg
            lastPatchedIdentityLogMs = nowMs
            logger.info { identMsg }
        }

        statics.track.writeWString(effectiveTrackId)
        statics.carModel.writeWString(effectiveCarModel)

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

    private companion object {

        const val PENALTY_DEDUP_CAPACITY = 32
    }
}

private fun CharArray.writeWString(value: String) {
    for (i in indices) this[i] = '\u0000'
    val n = minOf(size - 1, value.length)
    for (i in 0 until n) this[i] = value[i]
    this[n] = '\u0000'
}

private enum class IdentityChange { NONE,
    TRACK,
    CAR,
    BOTH
}

private class RespawnResetDetector {

    private var hasPrev = false
    private var prevX = 0f
    private var prevZ = 0f

    private var lastResetNs = 0L
    private var lastResumeNs = 0L
    private var lastSeenPacketId = -1

    fun reset() {
        hasPrev = false
        prevX = 0f
        prevZ = 0f
        lastResetNs = 0L
        lastResumeNs = 0L
        lastSeenPacketId = -1
    }

    fun onResumed(nowNs: Long, physicsPacketId: Int) {
        lastResumeNs = nowNs
        lastSeenPacketId = physicsPacketId
        hasPrev = false
    }

    fun update(
        nowNs: Long,
        physicsPacketId: Int,
        speedKmh: Float,
        tyreContactPoint: FloatArray,
        position: Vec2,
    ): Boolean {
        if (physicsPacketId == lastSeenPacketId) return false
        lastSeenPacketId = physicsPacketId

        val hasContact =
            tyreContactPoint.size >= 6 &&
                (abs(tyreContactPoint[0]) + abs(tyreContactPoint[2]) + abs(tyreContactPoint[3]) + abs(tyreContactPoint[5])) > 0.001f
        if (!hasContact) {
            hasPrev = false
            return false
        }

        if (!hasPrev) {
            hasPrev = true
            prevX = position.x
            prevZ = position.y
            return false
        }

        val dx = position.x - prevX
        val dz = position.y - prevZ
        val dist2 = dx * dx + dz * dz

        prevX = position.x
        prevZ = position.y

        val teleported = dist2 > (80f * 80f) && speedKmh < 5f
        val cooldownOk = (nowNs - lastResetNs) > 2_000_000_000L
        val resumeOk = (nowNs - lastResumeNs) > 2_000_000_000L

        if (teleported && cooldownOk && resumeOk) {
            lastResetNs = nowNs
            hasPrev = false
            return true
        }
        return false
    }
}

private class IdentityResetDetector {

    private data class Identity(val trackId: String?, val carModel: String?)

    private var stable: Identity = Identity(trackId = null, carModel = null)

    private var pending: Identity = Identity(trackId = null, carModel = null)
    private var pendingSinceNs: Long = 0L

    fun reset() {
        stable = Identity(null, null)
        pending = Identity(null, null)
        pendingSinceNs = 0L
    }

    fun observe(info: EvoFileInfo, nowNs: Long) {
        val t = info.trackId?.trim().takeIf { !it.isNullOrBlank() }
        val c = info.carModel?.trim().takeIf { !it.isNullOrBlank() }
        stable = Identity(
            trackId = t ?: stable.trackId,
            carModel = c ?: stable.carModel
        )
        pending = stable
        pendingSinceNs = nowNs
    }

    fun update(info: EvoFileInfo, nowNs: Long): IdentityChange {
        val newTrack = info.trackId?.trim().takeIf { !it.isNullOrBlank() }
        val newCar = info.carModel?.trim().takeIf { !it.isNullOrBlank() }

        if (newTrack == null && newCar == null) return IdentityChange.NONE

        val candidate = Identity(
            trackId = newTrack ?: stable.trackId,
            carModel = newCar ?: stable.carModel
        )

        if (candidate != pending) {
            pending = candidate
            pendingSinceNs = nowNs
            return IdentityChange.NONE
        }

        if ((nowNs - pendingSinceNs) < IDENTITY_DEBOUNCE_NS) return IdentityChange.NONE

        val trackChanged = candidate.trackId != null &&
            stable.trackId != null &&
            candidate.trackId != stable.trackId

        val carChanged = candidate.carModel != null &&
            stable.carModel != null &&
            candidate.carModel != stable.carModel

        if (stable.trackId == null && candidate.trackId != null) stable = stable.copy(trackId = candidate.trackId)
        if (stable.carModel == null && candidate.carModel != null) stable = stable.copy(carModel = candidate.carModel)

        if (!trackChanged && !carChanged) {
            stable = Identity(
                trackId = candidate.trackId ?: stable.trackId,
                carModel = candidate.carModel ?: stable.carModel
            )
            return IdentityChange.NONE
        }

        stable = Identity(candidate.trackId, candidate.carModel)

        return when {
            trackChanged && carChanged -> IdentityChange.BOTH
            trackChanged -> IdentityChange.TRACK
            else -> IdentityChange.CAR
        }
    }

    private companion object {

        private const val IDENTITY_DEBOUNCE_NS = 300_000_000L
    }
}
