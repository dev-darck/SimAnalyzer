package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.di.Stabilizer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackFuelAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.FuelSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.model.LapTimingSnapshot
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.EvoFileInfoSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.pose.PhysicsPoseExtractor
import com.project.analyzer.ac.telemetry.impl.internal.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.shm.AcSharedMemory
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFilePhysics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.utils.TelemetryIdentityIds
import com.project.analyzer.utils.logger.logger
import com.project.analyzer.utils.shm.writeWString
import dev.zacsweers.metro.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Inject
class AcEvoFallbackShmPatcher(
    @param:Stabilizer
    private val fileInfoExtractor: EvoFileInfoSource,
    private val lapAnalyzer: FallbackLapAnalyzer,
    private val fuelAnalyzer: FallbackFuelAnalyzer,
) {

    private val logger = logger()

    private var lastSessionEpoch: Long = -1L
    private val identityResetDetector = IdentityResetDetector()
    private val seenPenaltyIds = ArrayDeque<String>(PENALTY_DEDUP_CAPACITY)
    private val seenPenaltySet = HashSet<String>(PENALTY_DEDUP_CAPACITY * 2)
    private var lastPatchedTrackId: String = ""
    private var lastPatchedCarModel: String = ""
    private var lastPatchedIdentityLog: String = ""
    private var lastPatchedIdentityLogMs: Long = 0L
    private var lastPatchedSessionType: EvoSessionType = EvoSessionType.UNKNOWN
    private var pendingSessionTypeOverrideFromShm: Int? = null
    private var pendingSessionTypeOverrideToShm: Int? = null
    private var lastFilePollNs: Long = 0L
    private var cachedInfo: EvoFileInfo = EvoFileInfo()
    private var syntheticSessionIndex: Int = 0
    private var pendingSessionRestartHint: AcSessionRestartHint = AcSessionRestartHint.NONE
    private var mainMenuRestartReadyForResume: Boolean = false

    private var lastGameState: GameConnectionState = GameConnectionState.DISCONNECTED
    private var lastProcessedPhysicsPacketId: Int = -1
    private val poseExtractor = PhysicsPoseExtractor()
    private val respawnDetector = RespawnResetDetector()

    fun clear() {
        fileInfoExtractor.clear()
        lapAnalyzer.reset()
        fuelAnalyzer.reset()
        lastSessionEpoch = -1L

        identityResetDetector.reset()

        clearPenaltyDedup()

        lastPatchedTrackId = ""
        lastPatchedCarModel = ""
        lastPatchedIdentityLog = ""
        lastPatchedIdentityLogMs = 0L
        lastPatchedSessionType = EvoSessionType.UNKNOWN
        pendingSessionTypeOverrideFromShm = null
        pendingSessionTypeOverrideToShm = null
        lastFilePollNs = 0L
        cachedInfo = EvoFileInfo()
        syntheticSessionIndex = 0
        pendingSessionRestartHint = AcSessionRestartHint.NONE
        mainMenuRestartReadyForResume = false

        lastGameState = GameConnectionState.DISCONNECTED
        lastProcessedPhysicsPacketId = -1
        respawnDetector.reset()
    }

    internal fun patchIfNeeded(
        shm: AcSharedMemory,
        loopStartNanos: Long,
        gameState: GameConnectionState,
    ): AcSessionRestartHint {
        val info = pollFileInfo(loopStartNanos)
        if (!hasSignal(info)) return AcSessionRestartHint.NONE

        handleSessionEpochChange(
            info = info,
            loopStartNanos = loopStartNanos,
            gameState = gameState,
        )
        handleIdentityChange(info = info, loopStartNanos = loopStartNanos)
        handleGameStateTransition(
            gameState = gameState,
            loopStartNanos = loopStartNanos,
            physicsPacketId = shm.physics.packetId,
        )
        handlePenalty(info)

        val calibration = lapAnalyzer.loadCalibration(trackId = normalizedTrackId(info))
        patchStatics(shm.graphics, shm.statics, info, calibration)

        val sessionTypeBoundary = handleSessionTypeBoundary(info.sessionType)
        patchGraphicsBase(shm.graphics, info, gameState)
        if (sessionTypeBoundary) {
            bumpSyntheticSessionIndexForBoundary(shm.graphics)
        }

        if (gameState != GameConnectionState.IN_SESSION) return AcSessionRestartHint.NONE

        applySessionToGraphics(shm.graphics, info.sessionType)

        val restartHint = consumeRestartHintIfReady(gameState)
        val physicsPacketId = shm.physics.packetId
        if (!shouldProcessPhysicsPacket(physicsPacketId)) return restartHint

        maybeSoftResetAfterRespawn(
            nowNs = loopStartNanos,
            physicsPacketId = physicsPacketId,
            physics = shm.physics,
            calibration = calibration,
        )

        val lapSnapshot = processLapFrame(nowNs = loopStartNanos, physics = shm.physics)
        val fuelSnapshot = processFuelFrame(
            fuelLiters = shm.physics.fuel,
            lapSnapshot = lapSnapshot,
        )

        patchGraphics(
            graphics = shm.graphics,
            snapshot = lapSnapshot,
            fuelSnapshot = fuelSnapshot,
            hasCalibration = calibration != null,
        )
        return restartHint
    }

    private fun handleSessionEpochChange(info: EvoFileInfo, loopStartNanos: Long, gameState: GameConnectionState) {
        if (info.sessionEpoch == lastSessionEpoch) return

        val restartHint = when {
            lastSessionEpoch < 0L -> AcSessionRestartHint.NONE
            info.sessionEpochStartedFromMainMenu -> AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU
            else -> AcSessionRestartHint.PRESERVE_GROUP
        }
        lastSessionEpoch = info.sessionEpoch
        pendingSessionRestartHint = mergeRestartHints(pendingSessionRestartHint, restartHint)
        if (restartHint == AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU) {
            mainMenuRestartReadyForResume = gameState != GameConnectionState.IN_SESSION
        }

        resetAll(reason = "sessionEpoch changed", clearTrack = false, clearCar = false)
        identityResetDetector.observe(info, loopStartNanos)
    }

    private fun handleIdentityChange(info: EvoFileInfo, loopStartNanos: Long) {
        val identityChange = identityResetDetector.update(info, loopStartNanos)
        if (identityChange == IdentityChange.NONE) return

        val clearTrack = identityChange == IdentityChange.TRACK || identityChange == IdentityChange.BOTH
        val clearCar = identityChange == IdentityChange.CAR || identityChange == IdentityChange.BOTH
        resetAll(reason = "identity changed: $identityChange", clearTrack = clearTrack, clearCar = clearCar)
        identityResetDetector.observe(info, loopStartNanos)
    }

    private fun handleGameStateTransition(gameState: GameConnectionState, loopStartNanos: Long, physicsPacketId: Int) {
        if (gameState == lastGameState) return

        if (lastGameState == GameConnectionState.IN_MENU && gameState == GameConnectionState.IN_SESSION) {
            lapAnalyzer.rebasePose(loopStartNanos)
            respawnDetector.onResumed(loopStartNanos, physicsPacketId)
            lastProcessedPhysicsPacketId = -1
        }
        if (lastGameState == GameConnectionState.IN_SESSION &&
            gameState != GameConnectionState.IN_SESSION &&
            pendingSessionRestartHint == AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU
        ) {
            mainMenuRestartReadyForResume = true
        }
        lastGameState = gameState
    }

    private fun handlePenalty(info: EvoFileInfo) {
        val penaltyId = info.penaltyId ?: return
        if (!seenPenaltySet.contains(penaltyId)) {
            lapAnalyzer.onPenaltyDetected()
            markPenaltySeen(penaltyId)
        }
        fileInfoExtractor.clearPenalty()
    }

    private fun normalizedTrackId(info: EvoFileInfo): String? = TrackIdNormalizer
        .normalize(track = info.trackId ?: info.trackName.orEmpty(), layout = info.layoutId)
        .takeIf { it.isNotBlank() }

    private fun shouldProcessPhysicsPacket(physicsPacketId: Int): Boolean {
        if (physicsPacketId <= 0) return false
        if (physicsPacketId == lastProcessedPhysicsPacketId) return false
        lastProcessedPhysicsPacketId = physicsPacketId
        return true
    }

    private fun maybeSoftResetAfterRespawn(
        nowNs: Long,
        physicsPacketId: Int,
        physics: SPageFilePhysics,
        calibration: TrackCalibration?,
    ) {
        if (calibration == null || !lapAnalyzer.isSyncedToStartFinish()) return

        val pose = poseExtractor.extract(physics, calibration.referencePoint) ?: return
        val shouldSoftReset = respawnDetector.update(
            nowNs = nowNs,
            physicsPacketId = physicsPacketId,
            speedKmh = physics.speedKmh,
            tyreContactPoint = physics.tyreContactPoint,
            position = pose.position,
        )
        if (!shouldSoftReset) return

        logger.debug { "FallbackSHM soft reset: respawn detected (synced session)" }
        fuelAnalyzer.resetLapTracking(
            fuelLiters = physics.fuel,
            completedLaps = lapAnalyzer.getCompletedLapsCount(),
            startFinishSyncId = lapAnalyzer.getStartFinishSyncId(),
        )
        lapAnalyzer.softResetAfterRespawn(nowNs)
        clearPenaltyDedup()
    }

    private fun processLapFrame(nowNs: Long, physics: SPageFilePhysics): LapTimingSnapshot {
        lapAnalyzer.processPhysicsFrame(nowNs, physics)
        return lapAnalyzer.getSnapshot(nowNs)
    }

    private fun processFuelFrame(fuelLiters: Float, lapSnapshot: LapTimingSnapshot): FuelSnapshot {
        fuelAnalyzer.processFrame(
            fuelLiters = fuelLiters,
            completedLaps = lapSnapshot.completedLapsCount,
            lastLapTimeMs = lapSnapshot.lastLapTimeMs ?: 0,
            startFinishSyncId = lapSnapshot.startFinishSyncId,
        )
        return fuelAnalyzer.getSnapshot(currentFuelLiters = fuelLiters)
    }

    private fun handleSessionTypeBoundary(newSessionType: EvoSessionType): Boolean {
        if (!shouldResetForSessionTypeChange(lastPatchedSessionType, newSessionType)) {
            if (newSessionType != lastPatchedSessionType) {
                logger.debug {
                    "FallbackSHM sessionType: ${lastPatchedSessionType.name} ->" +
                        " ${newSessionType.name} (shmValue=${newSessionType.shmValue})"
                }
                lastPatchedSessionType = newSessionType
            }
            return false
        }

        logger.debug {
            "FallbackSHM sessionType boundary: ${lastPatchedSessionType.name} -> ${newSessionType.name}," +
                " resetting fallback lap/fuel runtime"
        }

        pendingSessionTypeOverrideFromShm = lastPatchedSessionType.shmValue
        pendingSessionTypeOverrideToShm = newSessionType.shmValue

        resetRuntimeForBoundary()

        lastPatchedSessionType = newSessionType
        return true
    }

    private fun bumpSyntheticSessionIndexForBoundary(graphics: SPageFileGraphics) {
        syntheticSessionIndex = maxOf(syntheticSessionIndex, graphics.sessionIndex)
        syntheticSessionIndex += 1
        graphics.sessionIndex = syntheticSessionIndex
    }

    private fun resetRuntimeForBoundary() {
        lapAnalyzer.resetSession()
        fuelAnalyzer.reset()
        clearPenaltyDedup()
        respawnDetector.reset()
        lastProcessedPhysicsPacketId = -1
    }

    private fun pollFileInfo(nowNs: Long): EvoFileInfo {
        if ((nowNs - lastFilePollNs) >= FILE_POLL_INTERVAL_NS) {
            cachedInfo = fileInfoExtractor.poll()
            lastFilePollNs = nowNs
        }
        return cachedInfo
    }

    private fun resetAll(reason: String, clearTrack: Boolean, clearCar: Boolean) {
        logger.debug { "FallbackSHM reset: $reason" }

        lapAnalyzer.resetSession()
        fuelAnalyzer.reset()
        clearPenaltyDedup()

        if (clearTrack) lastPatchedTrackId = ""
        if (clearCar) lastPatchedCarModel = ""

        lastPatchedIdentityLog = ""
        lastPatchedIdentityLogMs = 0L
        syntheticSessionIndex = 0
        pendingSessionTypeOverrideFromShm = null
        pendingSessionTypeOverrideToShm = null
    }

    private fun consumeRestartHintIfReady(gameState: GameConnectionState): AcSessionRestartHint {
        if (gameState != GameConnectionState.IN_SESSION) return AcSessionRestartHint.NONE

        return when (pendingSessionRestartHint) {
            AcSessionRestartHint.NONE -> AcSessionRestartHint.NONE

            AcSessionRestartHint.PRESERVE_GROUP -> {
                pendingSessionRestartHint = AcSessionRestartHint.NONE
                AcSessionRestartHint.PRESERVE_GROUP
            }

            AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU -> {
                if (!mainMenuRestartReadyForResume) return AcSessionRestartHint.NONE
                pendingSessionRestartHint = AcSessionRestartHint.NONE
                mainMenuRestartReadyForResume = false
                AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU
            }
        }
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

    private fun patchStatics(
        graphics: SPageFileGraphics,
        statics: SPageFileStatic,
        info: EvoFileInfo,
        calibration: TrackCalibration? = null,
    ) {
        ensureStaticDefaults(statics = statics, calibration = calibration)
        val effectiveTrackId = resolveEffectiveTrackId(normalizedTrackId(info).orEmpty())
        val effectiveCarModel = resolveEffectiveCarModel(info.carModel?.trim().orEmpty())
        applyPatchedIdentity(
            graphics = graphics,
            statics = statics,
            trackId = effectiveTrackId,
            carModel = effectiveCarModel,
            epoch = info.sessionEpoch,
        )
        applyDriverToStatics(statics, info.driverName)
    }

    private fun ensureStaticDefaults(statics: SPageFileStatic, calibration: TrackCalibration?) {
        if (statics.numCars <= 0) statics.numCars = 1
        if (statics.numberOfSessions <= 0) statics.numberOfSessions = 1
        if (statics.sectorCount <= 0) {
            statics.sectorCount = (calibration?.sectors?.size ?: 3).coerceAtLeast(1)
        }
    }

    private fun resolveEffectiveTrackId(trackId: String): String = when {
        trackId.isNotBlank() -> trackId.also { lastPatchedTrackId = it }
        lastPatchedTrackId.isNotBlank() -> lastPatchedTrackId
        else -> ""
    }

    private fun resolveEffectiveCarModel(carModel: String): String = when {
        carModel.isNotBlank() -> carModel.also { lastPatchedCarModel = it }
        lastPatchedCarModel.isNotBlank() -> lastPatchedCarModel
        else -> ""
    }

    private fun applyPatchedIdentity(
        graphics: SPageFileGraphics,
        statics: SPageFileStatic,
        trackId: String,
        carModel: String,
        epoch: Long,
    ) {
        logIdentityIfNeeded(trackId, carModel, epoch)
        trackId.takeIf(String::isNotBlank)?.let(statics.track::writeWString)
        carModel.takeIf(String::isNotBlank)?.let(statics.carModel::writeWString)
        applyCarIdToGraphics(graphics, TelemetryIdentityIds.stableCarId(carModel))
    }

    private fun applyCarIdToGraphics(graphics: SPageFileGraphics, carId: Int?) {
        val effectiveCarId = carId ?: return
        if (graphics.playerCarID <= 0) {
            graphics.playerCarID = effectiveCarId
        }
        if (graphics.carID.isNotEmpty() && graphics.carID[0] <= 0) {
            graphics.carID[0] = effectiveCarId
        }
    }

    private fun logIdentityIfNeeded(trackId: String, carModel: String, epoch: Long) {
        val identMsg = "FallbackSHM identity: track=$trackId car=$carModel epoch=$epoch"
        val nowMs = System.currentTimeMillis()
        if (identMsg != lastPatchedIdentityLog && (nowMs - lastPatchedIdentityLogMs) > IDENTITY_LOG_MIN_INTERVAL_MS) {
            lastPatchedIdentityLog = identMsg
            lastPatchedIdentityLogMs = nowMs
            logger.debug { identMsg }
        }
    }

    private fun applyDriverToStatics(statics: SPageFileStatic, driverName: String?) {
        val driverParts = driverName?.trim()?.split(" ")?.filter { it.isNotBlank() } ?: return
        val name = driverParts.getOrNull(0).orEmpty()
        if (name.isNotBlank()) {
            statics.playerName.writeWString(name)
        }
        val surname = driverParts.drop(1).joinToString(" ")
        if (surname.isNotBlank()) {
            statics.playerSurname.writeWString(surname)
        }
    }

    private fun patchGraphicsBase(graphics: SPageFileGraphics, info: EvoFileInfo, gameState: GameConnectionState) {
        if (graphics.session < 0) {
            graphics.session = when {
                gameState != GameConnectionState.IN_SESSION -> -1
                info.sessionType == EvoSessionType.UNKNOWN -> -1
                else -> info.sessionType.shmValue
            }
        }

        if (graphics.sessionIndex > 0) {
            syntheticSessionIndex = maxOf(syntheticSessionIndex, graphics.sessionIndex)
            return
        }
        if (syntheticSessionIndex > 0) {
            graphics.sessionIndex = syntheticSessionIndex
        }
    }

    private fun patchGraphics(
        graphics: SPageFileGraphics,
        snapshot: LapTimingSnapshot,
        fuelSnapshot: FuelSnapshot,
        hasCalibration: Boolean,
    ) {
        if (hasCalibration) {
            applyAnalyzerTimingToGraphics(graphics, snapshot)
        }
        applyDeltaToGraphics(graphics, snapshot)
        applyFuelToGraphics(graphics, fuelSnapshot)
    }

    private fun applySessionToGraphics(graphics: SPageFileGraphics, sessionType: EvoSessionType) {
        if (sessionType == EvoSessionType.UNKNOWN) return

        val patched = sessionType.shmValue
        val native = graphics.session

        if (native == patched) {
            clearPendingSessionTypeOverride()
            return
        }

        if (native < 0) {
            graphics.session = patched
            return
        }

        if (matchesPendingSessionTypeOverride(native = native, patched = patched)) {
            graphics.session = patched
            return
        }

        // In ACE fallback mode we treat file-derived sessionType as authoritative once available.
        // Native graphics.session can flap on pause/resume/UI transitions and cause false lifecycle restarts.
        graphics.session = patched
        if (pendingSessionTypeOverrideFromShm != null || pendingSessionTypeOverrideToShm != null) {
            clearPendingSessionTypeOverride()
        }
    }

    private fun clearPendingSessionTypeOverride() {
        pendingSessionTypeOverrideFromShm = null
        pendingSessionTypeOverrideToShm = null
    }

    private fun matchesPendingSessionTypeOverride(native: Int, patched: Int): Boolean {
        val pendingFrom = pendingSessionTypeOverrideFromShm ?: return false
        val pendingTo = pendingSessionTypeOverrideToShm ?: return false
        return pendingTo == patched && native == pendingFrom
    }

    private fun applyAnalyzerTimingToGraphics(graphics: SPageFileGraphics, snapshot: LapTimingSnapshot) {
        graphics.completedLaps = snapshot.completedLapsCount
        if (snapshot.currentLapTimeMs > 0) {
            graphics.iCurrentTime = snapshot.currentLapTimeMs
        }
        if ((snapshot.lastLapTimeMs ?: 0) > 0) {
            graphics.iLastTime = snapshot.lastLapTimeMs ?: 0
        }
        if ((snapshot.bestLapTimeMs ?: 0) > 0) {
            graphics.iBestTime = snapshot.bestLapTimeMs ?: 0
        }
        graphics.currentSectorIndex = snapshot.currentSectorIndex
        if ((snapshot.lastSectorTimeMs ?: 0) > 0) {
            graphics.lastSectorTime = snapshot.lastSectorTimeMs ?: 0
        }
        if (snapshot.currentSectorTimeMs > 0) {
            graphics.iSplit = snapshot.currentSectorTimeMs
        }
        if (graphics.isValidLap == 0 && snapshot.currentLapValid) {
            graphics.isValidLap = 1
        }
    }

    private fun applyDeltaToGraphics(graphics: SPageFileGraphics, snapshot: LapTimingSnapshot) {
        if (graphics.iDeltaLapTime == 0 && (snapshot.deltaLapTimeMs ?: 0) != 0) {
            graphics.iDeltaLapTime = snapshot.deltaLapTimeMs ?: 0
            graphics.isDeltaPositive = if (snapshot.isDeltaPositive) 1 else 0
        }
    }

    private fun applyFuelToGraphics(graphics: SPageFileGraphics, fuelSnapshot: FuelSnapshot) {
        fuelSnapshot.fuelPerLapLiters
            ?.takeIf { it > 0f }
            ?.let { graphics.fuelXLap = it }
        fuelSnapshot.fuelEstimatedLaps
            ?.takeIf { it > 0f }
            ?.let { graphics.fuelEstimatedLaps = it }
    }

    private companion object {

        const val PENALTY_DEDUP_CAPACITY = 32
        val FILE_POLL_INTERVAL_NS = 100.milliseconds.inWholeNanoseconds
        val IDENTITY_LOG_MIN_INTERVAL_MS = 500.milliseconds.inWholeMilliseconds
    }

    private fun hasSignal(info: EvoFileInfo): Boolean {
        if (info.sessionEpoch > 0L) return true
        if (!info.trackId.isNullOrBlank()) return true
        if (!info.trackName.isNullOrBlank()) return true
        if (!info.carModel.isNullOrBlank()) return true
        if (!info.driverName.isNullOrBlank()) return true
        if (!info.playerCarUuid.isNullOrBlank()) return true
        if (info.sessionType != EvoSessionType.UNKNOWN) return true
        if (info.hasPenalty) return true
        return false
    }

    private fun shouldResetForSessionTypeChange(current: EvoSessionType, next: EvoSessionType): Boolean {
        if (current == EvoSessionType.UNKNOWN) return false
        if (next == EvoSessionType.UNKNOWN) return false
        return current != next
    }

    private fun mergeRestartHints(
        current: AcSessionRestartHint,
        incoming: AcSessionRestartHint,
    ): AcSessionRestartHint = when {
        current == AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU ||
            incoming == AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU -> AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU

        current == AcSessionRestartHint.PRESERVE_GROUP ||
            incoming == AcSessionRestartHint.PRESERVE_GROUP -> AcSessionRestartHint.PRESERVE_GROUP

        else -> AcSessionRestartHint.NONE
    }
}

private enum class IdentityChange {
    NONE,
    TRACK,
    CAR,
    BOTH,
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
                (
                    abs(
                        tyreContactPoint[0],
                    ) + abs(tyreContactPoint[2]) + abs(tyreContactPoint[3]) + abs(tyreContactPoint[5])
                    ) > 0.001f
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
        val cooldownOk = (nowNs - lastResetNs) > 2.seconds.inWholeNanoseconds
        val resumeOk = (nowNs - lastResumeNs) > 2.seconds.inWholeNanoseconds

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
            carModel = c ?: stable.carModel,
        )
        pending = stable
        pendingSinceNs = nowNs
    }

    fun update(info: EvoFileInfo, nowNs: Long): IdentityChange {
        val candidate = buildCandidateIdentity(info) ?: return IdentityChange.NONE
        if (candidate != pending) {
            pending = candidate
            pendingSinceNs = nowNs
            return IdentityChange.NONE
        }

        if (!isIdentityDebounced(nowNs)) return IdentityChange.NONE

        return resolveIdentityChange(candidate)
    }

    private fun buildCandidateIdentity(info: EvoFileInfo): Identity? {
        val newTrack = info.trackId?.trim().takeIf { !it.isNullOrBlank() }
        val newCar = info.carModel?.trim().takeIf { !it.isNullOrBlank() }
        if (newTrack == null && newCar == null) return null
        return Identity(
            trackId = newTrack ?: stable.trackId,
            carModel = newCar ?: stable.carModel,
        )
    }

    private fun isIdentityDebounced(nowNs: Long): Boolean = (nowNs - pendingSinceNs) >= IDENTITY_DEBOUNCE_NS

    private fun resolveIdentityChange(candidate: Identity): IdentityChange {
        val trackChanged = candidate.trackId.hasChangedFrom(stable.trackId)
        val carChanged = candidate.carModel.hasChangedFrom(stable.carModel)
        adoptMissingStableIdentity(candidate)

        if (!trackChanged && !carChanged) {
            stable = Identity(
                trackId = candidate.trackId ?: stable.trackId,
                carModel = candidate.carModel ?: stable.carModel,
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

    private fun adoptMissingStableIdentity(candidate: Identity) {
        if (stable.trackId == null && candidate.trackId != null) {
            stable = stable.copy(trackId = candidate.trackId)
        }
        if (stable.carModel == null && candidate.carModel != null) {
            stable = stable.copy(carModel = candidate.carModel)
        }
    }

    private fun String?.hasChangedFrom(previous: String?): Boolean =
        this != null && previous != null && this != previous

    private companion object {

        private val IDENTITY_DEBOUNCE_NS = 300.milliseconds.inWholeNanoseconds
    }
}
