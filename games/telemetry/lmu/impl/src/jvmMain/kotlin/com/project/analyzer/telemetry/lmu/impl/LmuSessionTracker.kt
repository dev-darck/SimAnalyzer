package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.telemetry.api.contract.LapValidity
import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionField
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapFinished
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent.LapStarted
import com.project.analyzer.telemetry.api.model.TelemetryFrame

internal class LmuSessionTracker {
    private var connected: Boolean = false
    private var sessionId: Long = 0L
    private var lastLapIndex: Int? = null
    private var currentSession: SessionInfo? = null

    val isConnected: Boolean
        get() = connected

    suspend fun onFrame(
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ): LmuLifecycleFrameResult {
        if (!connected) {
            connected = true
            sessionId += 1L
            val session = createSessionInfo(frame)
            currentSession = session
            emit(TelemetryLifecycleEvent.SimConnected)
            emit(TelemetryLifecycleEvent.SessionStarted(session))
        }

        updateSessionFromFrame(frame, emit)
        processLapIndex(frame, emit)
        val sampleSessionId = sessionId.takeIf { connected && it > 0L }
        return LmuLifecycleFrameResult(frame = frame, sampleSessionId = sampleSessionId)
    }

    suspend fun onDisconnected(reason: SessionEndReason, emit: suspend (TelemetryLifecycleEvent) -> Unit) {
        if (sessionId > 0L) {
            emit(TelemetryLifecycleEvent.SessionEnded(sessionId, reason))
        }
        emit(TelemetryLifecycleEvent.SimDisconnected)
        reset()
    }

    fun reset() {
        connected = false
        lastLapIndex = null
        currentSession = null
    }

    private fun createSessionInfo(frame: TelemetryFrame): SessionInfo {
        val sessionType = frame.session?.sessionType ?: SessionType.UNKNOWN
        return SessionInfo(
            sessionId = sessionId,
            sessionType = sessionType,
            carModel = frame.session?.car?.carModel.orEmpty(),
            trackId = frame.session?.track?.trackId.orEmpty(),
            carId = frame.session?.car?.carId?.takeIf { it > 0 },
            layoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private suspend fun processLapIndex(frame: TelemetryFrame, emit: suspend (TelemetryLifecycleEvent) -> Unit) {
        val newLapIndex = frame.lap?.currentLapIndex
        val prevLap = lastLapIndex

        when {
            newLapIndex != null && prevLap == null -> emit(LapStarted(newLapIndex))

            newLapIndex != null && prevLap != null && newLapIndex != prevLap -> {
                emit(LapFinished(prevLap, LapValidity.UNKNOWN))
                emit(LapStarted(newLapIndex))
            }
        }

        lastLapIndex = newLapIndex
    }

    private suspend fun updateSessionFromFrame(
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ) {
        val cur = currentSession ?: return
        val restart = restartForSessionTypeBoundary(cur, frame, emit)
        if (restart) return
        if (restartForCarIdentityBoundary(cur, frame, emit)) return
        if (restartForTrackIdentityBoundary(cur, frame, emit)) return

        var updated = cur
        val changed = linkedSetOf<SessionField>()

        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (newType != SessionType.UNKNOWN && newType != cur.sessionType) {
            updated = updated.copy(sessionType = newType)
            changed += SessionField.SESSION_TYPE
        }

        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        if (newCar.isNotBlank() && newCar != cur.carModel) {
            updated = updated.copy(carModel = newCar)
            changed += SessionField.CAR_MODEL
        }

        val newCarId = frame.session?.car?.carId?.takeIf { it > 0 }
        if (newCarId != null && newCarId != cur.carId) {
            updated = updated.copy(carId = newCarId)
            changed += SessionField.CAR_ID
        }

        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        if (newTrack.isNotBlank() && newTrack != cur.trackId) {
            updated = updated.copy(trackId = newTrack)
            changed += SessionField.TRACK_ID
        }

        val newLayoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() }
        if (newLayoutId != cur.layoutId) {
            updated = updated.copy(layoutId = newLayoutId)
            changed += SessionField.TRACK_LAYOUT_ID
        }

        if (changed.isNotEmpty()) {
            currentSession = updated
            emit(TelemetryLifecycleEvent.SessionUpdated(updated, changed.toSet()))
        }
    }

    private suspend fun restartForSessionTypeBoundary(
        cur: SessionInfo,
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ): Boolean {
        val newType = frame.session?.sessionType ?: SessionType.UNKNOWN
        if (!shouldRestartForSessionTypeChange(cur.sessionType, newType)) return false

        emit(TelemetryLifecycleEvent.SessionEnded(cur.sessionId, SessionEndReason.REPLACED_BY_NEW_SESSION))
        sessionId += 1L
        lastLapIndex = null
        val restarted = cur.copy(
            sessionId = sessionId,
            sessionType = newType,
            carModel = frame.session?.car?.carModel.orEmpty().trim().ifBlank { cur.carModel },
            trackId = frame.session?.track?.trackId.orEmpty().trim().ifBlank { cur.trackId },
            carId = frame.session?.car?.carId?.takeIf { it > 0 } ?: cur.carId,
            layoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() } ?: cur.layoutId,
        )
        currentSession = restarted
        emit(TelemetryLifecycleEvent.SessionStarted(restarted))
        return true
    }

    private suspend fun restartForCarIdentityBoundary(
        cur: SessionInfo,
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ): Boolean {
        val newCar = frame.session?.car?.carModel.orEmpty().trim()
        val newCarId = frame.session?.car?.carId?.takeIf { it > 0 }
        val carChanged = newCar.isNotBlank() && cur.carModel.isNotBlank() && newCar != cur.carModel
        val carIdChanged = newCarId != null && cur.carId != null && newCarId != cur.carId
        if (!carChanged && !carIdChanged) return false

        restartSession(
            current = cur,
            frame = frame,
            emit = emit,
        )
        return true
    }

    private suspend fun restartForTrackIdentityBoundary(
        cur: SessionInfo,
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ): Boolean {
        val newTrack = frame.session?.track?.trackId.orEmpty().trim()
        val newLayoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() }
        val trackChanged = newTrack.isNotBlank() && cur.trackId.isNotBlank() && newTrack != cur.trackId
        val layoutChanged = newLayoutId != cur.layoutId
        if (!trackChanged && !layoutChanged) return false

        restartSession(
            current = cur,
            frame = frame,
            emit = emit,
        )
        return true
    }

    private suspend fun restartSession(
        current: SessionInfo,
        frame: TelemetryFrame,
        emit: suspend (TelemetryLifecycleEvent) -> Unit,
    ) {
        emit(TelemetryLifecycleEvent.SessionEnded(current.sessionId, SessionEndReason.REPLACED_BY_NEW_SESSION))
        sessionId += 1L
        lastLapIndex = null
        val restarted = current.copy(
            sessionId = sessionId,
            sessionType = frame.session?.sessionType ?: current.sessionType,
            carModel = frame.session?.car?.carModel.orEmpty().trim().ifBlank { current.carModel },
            trackId = frame.session?.track?.trackId.orEmpty().trim().ifBlank { current.trackId },
            carId = frame.session?.car?.carId?.takeIf { it > 0 } ?: current.carId,
            layoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() } ?: current.layoutId,
        )
        currentSession = restarted
        emit(TelemetryLifecycleEvent.SessionStarted(restarted))
    }

    private fun shouldRestartForSessionTypeChange(current: SessionType, next: SessionType): Boolean {
        if (current == SessionType.UNKNOWN) return false
        if (next == SessionType.UNKNOWN) return false
        return current != next
    }
}
