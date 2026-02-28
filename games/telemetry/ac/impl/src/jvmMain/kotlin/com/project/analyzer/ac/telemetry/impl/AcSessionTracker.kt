package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.ac.telemetry.impl.internal.AcSessionRestartHint
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.GameConnectionState
import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionField
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionPauseReason
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger

/**
 * Tracks AC session/lap runtime and emits lifecycle events from mapped frames.
 *
 * This is not a strict FSM table; it's a stateful tracker/coordinator with resume/new-session heuristics.
 */
internal class AcSessionTracker {

    private val logger = logger()

    private var lastLapIndex: Int? = null
    private var lastLapValidity: LapValidity = LapValidity.UNKNOWN
    private var lastConnectionState: GameConnectionState = GameConnectionState.DISCONNECTED

    private var lastSessionIndex: Int? = null
    private var lastSessionTimeLeftSec: Float? = null
    private var sessionState: SessionState = SessionState.NONE
    private var sessionId: Long = 0L
    private var currentSession: SessionInfo? = null
    private var currentSessionIndex: Int? = null
    private var lastRawLapIndex: Int? = null
    private var lastRawCompletedLaps: Int? = null
    private var lastSessionPlannedLaps: Int? = null
    private var lastSessionIsTimedRace: Boolean? = null

    private data class ResumeMismatch(
        val carChanged: Boolean,
        val carIdChanged: Boolean,
        val trackChanged: Boolean,
        val sessionTypeBoundary: Boolean,
        val sessionIndexBoundary: Boolean,
        val lapCounterBoundary: Boolean,
        val newCar: String,
        val newCarId: Int?,
        val newTrack: String,
        val newType: SessionType,
        val newSessionIndex: Int?,
        val newRawLap: Int?,
        val newRawCompleted: Int?,
    ) {
        val rejected: Boolean
            get() = carChanged ||
                carIdChanged ||
                trackChanged ||
                sessionTypeBoundary ||
                sessionIndexBoundary ||
                lapCounterBoundary
    }

    fun onConnectionStateChanged(
        newState: GameConnectionState,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ) {
        val oldState = lastConnectionState
        if (oldState == newState) return

        lastConnectionState = newState
        logger.atDebug(RATE_LIMITED) {
            message = "ConnectionState: $oldState -> $newState (source=$source)"
        }

        if (oldState == GameConnectionState.DISCONNECTED &&
            (newState == GameConnectionState.IN_MENU || newState == GameConnectionState.IN_SESSION)
        ) {
            emit(TelemetryLifecycleEvent.SimConnected)
        }

        when (oldState to newState) {
            GameConnectionState.IN_SESSION to GameConnectionState.IN_MENU -> {
                if (sessionState == SessionState.RUNNING && sessionId > 0L) {
                    sessionState = SessionState.PAUSED
                    logger.atDebug(RATE_LIMITED) {
                        message = "SessionPaused id=$sessionId reason=NOT_IN_SESSION (source=$source)"
                    }
                    emit(TelemetryLifecycleEvent.SessionPaused(sessionId, SessionPauseReason.NOT_IN_SESSION))
                }
            }

            GameConnectionState.IN_MENU to GameConnectionState.IN_SESSION,
            GameConnectionState.DISCONNECTED to GameConnectionState.IN_SESSION -> Unit

            GameConnectionState.IN_MENU to GameConnectionState.DISCONNECTED,
            GameConnectionState.IN_SESSION to GameConnectionState.DISCONNECTED,
                -> {
                if (sessionState != SessionState.NONE && sessionId > 0L) {
                    logger.atDebug(RATE_LIMITED) {
                        message = "SessionEnded id=$sessionId reason=SIM_DISCONNECTED (source=$source)"
                    }
                    emit(TelemetryLifecycleEvent.SessionEnded(sessionId, SessionEndReason.SIM_DISCONNECTED))
                }
                emit(TelemetryLifecycleEvent.SimDisconnected)
                resetAll()
            }

            else -> Unit
        }
    }

    fun onFrame(
        frame: TelemetryFrame,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
        restartHint: AcSessionRestartHint = AcSessionRestartHint.NONE,
    ): AcLifecycleFrameResult {
        if (restartHint != AcSessionRestartHint.NONE) {
            handleRestartHint(frame, source, restartHint, emit)
        }

        when {
            sessionState == SessionState.PAUSED && lastConnectionState == GameConnectionState.IN_SESSION -> {
                enterSessionOnFirstFrame(frame, wasPaused = true, source, emit)
            }

            sessionState == SessionState.NONE -> {
                startNewSessionFromFrame(frame, source, emit, replacedOld = false)
            }
        }

        logger.atDebug(RATE_LIMITED) {
            message = "sample " + frameKeySummary(frame)
        }

        updateSessionFromFrame(frame, source, emit)
        logInferredBoundaries(frame, source)

        val newValidity = frame.lap?.validity ?: LapValidity.UNKNOWN
        val newLapIndex = frame.lap?.currentLapIndex

        processLapIndex(frame, newLapIndex, source, emit)
        lastLapValidity = newValidity
        updateRawLapSnapshot(frame)

        val sampleSessionId = if (sessionState == SessionState.RUNNING && sessionId > 0L) sessionId else null
        return AcLifecycleFrameResult(
            frame = frame,
            sampleSessionId = sampleSessionId,
        )
    }

    fun resetAll() {
        resetSessionRuntime()
        lastConnectionState = GameConnectionState.DISCONNECTED
        lastSessionIndex = null
        lastSessionTimeLeftSec = null
        lastSessionPlannedLaps = null
        lastSessionIsTimedRace = null

        sessionState = SessionState.NONE
        currentSession = null
    }

    private fun enterSessionOnFirstFrame(
        frame: TelemetryFrame,
        wasPaused: Boolean,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ) {
        val isResume = wasPaused && isLikelyResume(frame)

        logger.atDebug(RATE_LIMITED) {
            message = "enterSessionOnFirstFrame wasPaused=$wasPaused isResume=$isResume " +
                "(source=$source) " + frameKeySummary(frame)
        }

        if (isResume) {
            sessionState = SessionState.RUNNING
            if (sessionId > 0L) {
                logger.atDebug(RATE_LIMITED) { message = "SessionResumed id=$sessionId (source=$source)" }
                emit(TelemetryLifecycleEvent.SessionResumed(sessionId))
            } else {
                startNewSessionFromFrame(frame, source, emit, replacedOld = false)
            }
            return
        }

        startNewSessionFromFrame(
            frame,
            source,
            emit,
            replacedOld = (sessionState != SessionState.NONE && sessionId > 0L),
        )
    }

    private fun isLikelyResume(frame: TelemetryFrame): Boolean {
        val cur = currentSession ?: return false
        val mismatch = computeResumeMismatch(cur, frame)

        if (mismatch.rejected) {
            logger.atDebug(RATE_LIMITED) {
                message = "resume rejected: carChanged=${mismatch.carChanged} carIdChanged=${mismatch.carIdChanged} " +
                    "trackChanged=${mismatch.trackChanged} " +
                    "sessionTypeBoundary=${mismatch.sessionTypeBoundary} " +
                    "sessionIndexBoundary=${mismatch.sessionIndexBoundary} " +
                    "lapCounterBoundary=${mismatch.lapCounterBoundary} " +
                    "cur(car=${cur.carModel}, carId=${cur.carId}, track=${cur.trackId}) " +
                    "new(car=${mismatch.newCar}, carId=${mismatch.newCarId}, track=${mismatch.newTrack}, " +
                    "type=${mismatch.newType}, idx=${mismatch.newSessionIndex}, " +
                    "rawLap=${mismatch.newRawLap}, rawCompleted=${mismatch.newRawCompleted})"
            }
        }

        return !mismatch.rejected
    }

    private fun startNewSessionFromFrame(
        frame: TelemetryFrame,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
        replacedOld: Boolean,
        replacementReason: SessionEndReason = SessionEndReason.REPLACED_BY_NEW_SESSION,
    ) {
        if (replacedOld) {
            logger.atDebug(RATE_LIMITED) {
                message = "SessionEnded id=$sessionId reason=$replacementReason (source=$source)"
            }
            emit(TelemetryLifecycleEvent.SessionEnded(sessionId, replacementReason))
        }

        sessionId += 1L
        sessionState = SessionState.RUNNING
        resetSessionRuntime()

        val sessionType = frame.session?.sessionType ?: SessionType.UNKNOWN
        val car = frame.session?.car?.carModel.orEmpty().trim()
        val carId = frame.session?.car?.carId?.takeIf { it > 0 }
        val track = frame.session?.track?.trackId.orEmpty().trim()

        currentSession = SessionInfo(
            sessionId = sessionId,
            sessionType = sessionType,
            carModel = car,
            trackId = track,
            carId = carId,
        )
        currentSessionIndex = frame.session?.sessionIndex?.takeIf { it >= 0 }
        lastSessionTimeLeftSec = frame.session?.sessionTimeLeftSec
        lastSessionPlannedLaps = frame.session?.plannedLaps?.takeIf { it > 0 }
        lastSessionIsTimedRace = frame.session?.isTimedRace

        logger.atDebug(RATE_LIMITED) {
            message = "SessionStarted id=$sessionId type=$sessionType " +
                "idx=$currentSessionIndex online=${frame.session?.isOnline} " +
                "track=$track car=$car carId=$carId (source=$source)"
        }

        emit(TelemetryLifecycleEvent.SessionStarted(currentSession!!))
    }

    private fun updateSessionFromFrame(
        frame: TelemetryFrame,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ) {
        val cur = currentSession ?: return
        if (restartForSessionIndexBoundary(frame, source, emit)) return
        syncCurrentSessionIndex(frame)
        if (restartForSessionTypeBoundary(cur, frame, source, emit)) return

        var updated = cur
        val changed = linkedSetOf<SessionField>()
        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (newType != SessionType.UNKNOWN && newType != cur.sessionType) {
            logger.atDebug(RATE_LIMITED) {
                message = "SessionType changed: ${cur.sessionType} -> $newType (source=$source)"
            }
            updated = updated.copy(sessionType = newType)
            changed += SessionField.SESSION_TYPE
        }

        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        if (newCar.isNotBlank() && newCar != cur.carModel) {
            logger.atDebug(RATE_LIMITED) {
                message = "CarModel changed: ${cur.carModel} -> $newCar (source=$source)"
            }
            updated = updated.copy(carModel = newCar)
            changed += SessionField.CAR_MODEL
        }

        val newCarId = frame.session?.car?.carId?.takeIf { it > 0 }
        if (newCarId != null && newCarId != cur.carId) {
            logger.atDebug(RATE_LIMITED) {
                message = "CarId changed: ${cur.carId} -> $newCarId (source=$source)"
            }
            updated = updated.copy(carId = newCarId)
            changed += SessionField.CAR_ID
        }

        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        if (newTrack.isNotBlank() && newTrack != cur.trackId) {
            logger.atDebug(RATE_LIMITED) {
                message = "TrackId changed: ${cur.trackId} -> $newTrack (source=$source)"
            }
            updated = updated.copy(trackId = newTrack)
            changed += SessionField.TRACK_ID
        }

        if (changed.isNotEmpty()) {
            currentSession = updated
            emit(TelemetryLifecycleEvent.SessionUpdated(updated, changed.toSet()))
        }
    }

    private fun restartForSessionIndexBoundary(
        frame: TelemetryFrame,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ): Boolean {
        val cur = currentSession ?: return false
        val newSessionIndex = frame.session?.sessionIndex?.takeIf { it >= 0 }
        val previousSessionIndex = currentSessionIndex
        val hasBoundary = newSessionIndex != null &&
            previousSessionIndex != null &&
            newSessionIndex != previousSessionIndex
        if (!hasBoundary) return false

        if (!hasTrustedSessionIndexBoundary(cur, frame)) {
            logger.atDebug(RATE_LIMITED) {
                message = "SessionIndex change ignored: $previousSessionIndex -> $newSessionIndex, " +
                    "keeping current session (source=$source)"
            }
            currentSessionIndex = newSessionIndex
            return false
        }

        logger.atDebug(RATE_LIMITED) {
            message = "SessionIndex boundary: $previousSessionIndex -> $newSessionIndex, " +
                "starting new session (source=$source)"
        }
        startNewSessionFromFrame(frame, source, emit, replacedOld = true)
        return true
    }

    private fun syncCurrentSessionIndex(frame: TelemetryFrame) {
        val newSessionIndex = frame.session?.sessionIndex?.takeIf { it >= 0 }
        if (newSessionIndex != null && currentSessionIndex == null) {
            currentSessionIndex = newSessionIndex
        }
    }

    private fun restartForSessionTypeBoundary(
        cur: SessionInfo,
        frame: TelemetryFrame,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ): Boolean {
        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (!shouldRestartForSessionTypeChange(cur.sessionType, newType)) return false

        logger.atDebug(RATE_LIMITED) {
            message = "SessionType boundary: ${cur.sessionType} -> $newType, starting new session (source=$source)"
        }
        startNewSessionFromFrame(frame, source, emit, replacedOld = true)
        return true
    }

    private fun processLapIndex(
        frame: TelemetryFrame,
        newLapIndex: Int?,
        source: DataSourceType,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ) {
        val prevLap = lastLapIndex

        when {
            newLapIndex != null && prevLap == null -> {
                logger.atDebug(RATE_LIMITED) {
                    message = "LapStarted lap=$newLapIndex (source=$source)"
                }
                emit(LapStarted(newLapIndex))
            }

            newLapIndex != null && prevLap != null && newLapIndex != prevLap -> {
                val lastLapTime = frame.lap?.lastLapTimeMs
                logger.atDebug(RATE_LIMITED) {
                    message = "LapFinished lap=$prevLap validity=$lastLapValidity " +
                        "lastLapTimeMs=$lastLapTime (source=$source)"
                }
                emit(LapFinished(prevLap, lastLapValidity))

                logger.atDebug(RATE_LIMITED) {
                    message = "LapStarted lap=$newLapIndex (source=$source)"
                }
                emit(LapStarted(newLapIndex))
            }
        }

        lastLapIndex = newLapIndex
    }

    private fun logInferredBoundaries(frame: TelemetryFrame, source: DataSourceType) {
        val s = frame.session ?: return

        val idx = s.sessionIndex
        if (idx != null && idx >= 0) {
            if (lastSessionIndex != null && idx != lastSessionIndex) {
                logger.atDebug(RATE_LIMITED) {
                    message = "[boundary] sessionIndex: $lastSessionIndex -> $idx " +
                        "type=${s.sessionType} track=${s.track?.trackId} car=${s.car?.carModel} (source=$source)"
                }
            }
            lastSessionIndex = idx
        }

        val plannedLaps = s.plannedLaps?.takeIf { it > 0 }
        if (plannedLaps != null) {
            if (lastSessionPlannedLaps != null && plannedLaps != lastSessionPlannedLaps) {
                logger.atDebug(RATE_LIMITED) {
                    message = "[boundary] plannedLaps: $lastSessionPlannedLaps -> $plannedLaps " +
                        "idx=${s.sessionIndex} type=${s.sessionType} (source=$source)"
                }
            }
            lastSessionPlannedLaps = plannedLaps
        }

        val timedRace = s.isTimedRace
        if (timedRace != null) {
            if (lastSessionIsTimedRace != null && timedRace != lastSessionIsTimedRace) {
                logger.atDebug(RATE_LIMITED) {
                    message = "[boundary] isTimedRace: $lastSessionIsTimedRace -> $timedRace " +
                        "idx=${s.sessionIndex} type=${s.sessionType} (source=$source)"
                }
            }
            lastSessionIsTimedRace = timedRace
        }

        val left = s.sessionTimeLeftSec
        val prevLeft = lastSessionTimeLeftSec
        if (prevLeft != null) {
            val jumpUp = left?.let { (it - prevLeft) > 120f }
            if (jumpUp == true) {
                logger.atDebug(RATE_LIMITED) {
                    message = "[boundary] sessionTimeLeft jump up: $prevLeft -> $left " +
                        "idx=${s.sessionIndex} type=${s.sessionType} (source=$source)"
                }
            }
        }
        if (left != null && left.isFinite()) {
            lastSessionTimeLeftSec = left
        }
    }

    private fun frameKeySummary(frame: TelemetryFrame): String {
        val s = frame.session
        val track = s?.track?.trackId
        val car = s?.car?.carModel
        val idx = s?.sessionIndex
        val type = s?.sessionType
        val laps = s?.completedLaps
        val planned = s?.plannedLaps
        val timed = s?.isTimedRace
        val left = s?.sessionTimeLeftSec
        val lap = frame.lap?.currentLapIndex
        return "track=$track car=$car type=$type idx=$idx laps=$laps planned=$planned " +
            "timed=$timed left=$left lap=$lap"
    }

    private fun resetSessionRuntime() {
        lastLapIndex = null
        lastLapValidity = LapValidity.UNKNOWN
        currentSessionIndex = null
        lastRawLapIndex = null
        lastRawCompletedLaps = null
        lastSessionPlannedLaps = null
        lastSessionIsTimedRace = null
    }

    private fun handleRestartHint(
        frame: TelemetryFrame,
        source: DataSourceType,
        restartHint: AcSessionRestartHint,
        emit: (TelemetryLifecycleEvent) -> Unit,
    ) {
        val hasExistingSession = sessionState != SessionState.NONE && sessionId > 0L
        val replacementReason = when (restartHint) {
            AcSessionRestartHint.NONE -> SessionEndReason.REPLACED_BY_NEW_SESSION
            AcSessionRestartHint.PRESERVE_GROUP -> SessionEndReason.REPLACED_BY_NEW_SESSION
            AcSessionRestartHint.NEW_GROUP_AFTER_MAIN_MENU -> SessionEndReason.REPLACED_AFTER_MAIN_MENU
        }

        logger.atDebug(RATE_LIMITED) {
            message = "forced restart hint=$restartHint existing=$hasExistingSession " +
                "(source=$source) ${frameKeySummary(frame)}"
        }

        startNewSessionFromFrame(
            frame = frame,
            source = source,
            emit = emit,
            replacedOld = hasExistingSession,
            replacementReason = replacementReason,
        )
    }

    private fun shouldRestartForSessionTypeChange(current: SessionType, next: SessionType): Boolean {
        if (current == SessionType.UNKNOWN) return false
        if (next == SessionType.UNKNOWN) return false
        return current != next
    }

    private fun computeResumeMismatch(cur: SessionInfo, frame: TelemetryFrame): ResumeMismatch {
        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        val newCarId = frame.session?.car?.carId?.takeIf { it > 0 }
        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        val newSessionIndex = frame.session?.sessionIndex
        val newRawLap = frame.lap?.currentLapIndex
        val newRawCompleted = rawCompletedLaps(frame)
        val lapCounterBoundary = shouldRestartForLapCounterBoundary(newRawLap, newRawCompleted)
        val sessionIndexBoundary = hasTrustedSessionIndexBoundary(cur, frame)

        return ResumeMismatch(
            carChanged = newCar.isNotBlank() && cur.carModel.isNotBlank() && newCar != cur.carModel,
            carIdChanged = newCarId != null && cur.carId != null && newCarId != cur.carId,
            trackChanged = newTrack.isNotBlank() && cur.trackId.isNotBlank() && newTrack != cur.trackId,
            sessionTypeBoundary = shouldRestartForSessionTypeChange(cur.sessionType, newType),
            sessionIndexBoundary = sessionIndexBoundary,
            lapCounterBoundary = lapCounterBoundary,
            newCar = newCar,
            newCarId = newCarId,
            newTrack = newTrack,
            newType = newType,
            newSessionIndex = newSessionIndex,
            newRawLap = newRawLap,
            newRawCompleted = newRawCompleted,
        )
    }

    private fun rawCompletedLaps(frame: TelemetryFrame): Int? = listOfNotNull(
        frame.lap?.completedLaps,
        frame.session?.completedLaps,
    ).firstOrNull { it >= 0 }

    private fun hasTrustedSessionIndexBoundary(cur: SessionInfo, frame: TelemetryFrame): Boolean {
        val newSessionIndex = frame.session?.sessionIndex?.takeIf { it >= 0 } ?: return false
        val prevSessionIndex = currentSessionIndex ?: return false
        if (newSessionIndex == prevSessionIndex) return false

        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (shouldRestartForSessionTypeChange(cur.sessionType, newType)) return true
        if (shouldRestartForLapCounterBoundary(frame.lap?.currentLapIndex, rawCompletedLaps(frame))) return true
        if (hasSessionClockJumpBoundary(frame.session?.sessionTimeLeftSec)) return true
        if (hasSessionShapeBoundary(frame.session?.plannedLaps, frame.session?.isTimedRace)) return true
        return false
    }

    private fun hasSessionClockJumpBoundary(newTimeLeftSec: Float?): Boolean {
        val prevTimeLeftSec = lastSessionTimeLeftSec ?: return false
        val nextTimeLeftSec = newTimeLeftSec ?: return false
        if (!prevTimeLeftSec.isFinite() || !nextTimeLeftSec.isFinite()) return false
        return (nextTimeLeftSec - prevTimeLeftSec) > SESSION_CLOCK_JUMP_BOUNDARY_SEC
    }

    private fun hasSessionShapeBoundary(newPlannedLaps: Int?, newIsTimedRace: Boolean?): Boolean {
        val plannedLapsChanged = newPlannedLaps != null &&
            newPlannedLaps > 0 &&
            lastSessionPlannedLaps != null &&
            newPlannedLaps != lastSessionPlannedLaps
        val timedRaceChanged = newIsTimedRace != null &&
            lastSessionIsTimedRace != null &&
            newIsTimedRace != lastSessionIsTimedRace
        return plannedLapsChanged || timedRaceChanged
    }

    private fun shouldRestartForLapCounterBoundary(newRawLap: Int?, newRawCompleted: Int?): Boolean {
        val prevLap = lastRawLapIndex ?: return false
        val prevCompleted = lastRawCompletedLaps ?: return false
        val lap = newRawLap ?: return false
        val completed = newRawCompleted ?: return false

        val lapDrop = prevLap - lap
        val completedDrop = prevCompleted - completed
        val hasLargeDrop = lapDrop >= RESUME_BOUNDARY_LAP_DROP_MIN ||
            completedDrop >= RESUME_BOUNDARY_COMPLETED_DROP_MIN
        if (!hasLargeDrop) return false

        return lap <= RESUME_BOUNDARY_NEW_LAP_MAX &&
            completed <= RESUME_BOUNDARY_NEW_COMPLETED_MAX
    }

    private fun updateRawLapSnapshot(frame: TelemetryFrame) {
        frame.lap?.currentLapIndex
            ?.takeIf { it > 0 }
            ?.let { lastRawLapIndex = it }

        rawCompletedLaps(frame)?.let { lastRawCompletedLaps = it }
    }

    private companion object {

        const val RESUME_BOUNDARY_LAP_DROP_MIN = 2
        const val RESUME_BOUNDARY_COMPLETED_DROP_MIN = 2
        const val RESUME_BOUNDARY_NEW_LAP_MAX = 2
        const val RESUME_BOUNDARY_NEW_COMPLETED_MAX = 1
        const val SESSION_CLOCK_JUMP_BOUNDARY_SEC = 120f
    }
}
