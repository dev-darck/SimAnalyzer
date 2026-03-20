package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.api.contract.SessionEndReason
import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycleEvent
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.recording.TelemetryRecordingSample
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingConfigInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingEventInput
import com.project.analyzer.telemetry.recording.impl.controller.TelemetryRecordingInput.TelemetryRecordingSampleInput
import com.project.analyzer.telemetry.recording.impl.index.TelemetryFrameIndexBuilder
import dev.zacsweers.metro.Inject
import java.util.UUID
import kotlin.math.abs

@Inject
internal class TelemetryRecordingSessionCoordinator(
    private val recorder: TelemetryRecorder,
    private val frameIndexBuilder: TelemetryFrameIndexBuilder,
) {

    private var state: ControllerState = ControllerState()
    private var pendingPreStartSample: TelemetryRecordingSample? = null

    suspend fun handle(input: TelemetryRecordingInput) {
        when (input) {
            is TelemetryRecordingConfigInput -> handleConfig(input.config)
            is TelemetryRecordingEventInput -> handleEvent(input.event)
            is TelemetryRecordingSampleInput -> handleSample(input.sample)
        }
    }

    suspend fun shutdownAndCloseRecorder() {
        val activeGameId = state.currentGameId
        val activeSessionId = state.startedSessionId
        if (activeGameId != null && activeSessionId != null) {
            recorder.endSession(activeGameId, activeSessionId, "shutdown")
        }
        recorder.close()
        reset()
    }

    fun reset() {
        state = ControllerState()
        pendingPreStartSample = null
    }

    private suspend fun handleConfig(config: TelemetryAcquisitionConfig) {
        state = state.withRecordingConfig(
            recordingEnabled = config.recordingEnabled,
            maxRecordedLaps = config.maxRecordedLaps,
        )

        if (!config.recordingEnabled) {
            val activeGameId = state.currentGameId
            val activeSessionId = state.startedSessionId
            if (activeGameId != null && activeSessionId != null) {
                recorder.endSession(activeGameId, activeSessionId, "disabled")
            }
            state = state.clearSessionState()
        }
    }

    private suspend fun handleEvent(event: TelemetryLifecycleEvent) {
        when (event) {
            is TelemetryLifecycleEvent.SessionStarted -> onSessionStarted(event.session)
            is TelemetryLifecycleEvent.SessionUpdated -> onSessionUpdated(event.session)
            is TelemetryLifecycleEvent.SessionPaused -> onSessionPaused(event)
            is TelemetryLifecycleEvent.SessionResumed -> onSessionResumed(event)
            is TelemetryLifecycleEvent.SessionEnded -> onSessionEnded(event)
            is TelemetryLifecycleEvent.SimDisconnected -> onSimDisconnected()
            else -> Unit
        }
    }

    private suspend fun onSessionStarted(incoming: SessionInfo) {
        val current = state
        val currentStartedSessionId = current.startedSessionId
        val currentGameId = current.currentGameId

        if (currentStartedSessionId == incoming.sessionId) {
            state = current.withSessionInfo(incoming)
            return
        }

        if (currentStartedSessionId != null && currentGameId != null) {
            recorder.endSession(
                gameId = currentGameId,
                sessionId = currentStartedSessionId,
                reason = "replaced_by_session_started",
            )
        }

        state = current.withObservedSession(incoming)

        drainPendingPreStartSample(incoming.sessionId)
    }

    private suspend fun onSessionUpdated(updated: SessionInfo) {
        if (state.sessionInfo?.sessionId == updated.sessionId) {
            state = state.withSessionInfo(updated)
        }
        if (state.startedSessionId != updated.sessionId) return

        val gameId = state.currentGameId ?: return
        recorder.updateSession(
            TelemetrySessionUpdate(
                sessionId = updated.sessionId,
                gameId = gameId,
                sessionType = updated.sessionType.asSessionTypeString(),
                carModel = updated.carModel.ifBlank { null },
                carId = updated.carId,
                trackId = updated.trackId.ifBlank { null },
                layoutId = updated.layoutId?.trim()?.takeIf { it.isNotBlank() },
                airTempC = state.lastAirTempC,
                trackTempC = state.lastTrackTempC,
                dataSource = state.currentDataSource,
            ),
        )
    }

    private suspend fun onSessionPaused(event: TelemetryLifecycleEvent.SessionPaused) {
        val gameId = state.currentGameId ?: return
        if (state.startedSessionId != event.sessionId) return
        recorder.pauseSession(gameId = gameId, sessionId = event.sessionId, reason = event.reason.name)
    }

    private suspend fun onSessionResumed(event: TelemetryLifecycleEvent.SessionResumed) {
        val gameId = state.currentGameId ?: return
        if (state.startedSessionId != event.sessionId) return
        recorder.resumeSession(gameId = gameId, sessionId = event.sessionId)
    }

    private suspend fun onSessionEnded(event: TelemetryLifecycleEvent.SessionEnded) {
        val gameId = state.currentGameId
        val endedSessionType = state.sessionInfo
            ?.sessionType
            ?.takeUnless { it == SessionType.UNKNOWN }
        val endedSessionIdentity = gameId?.let { activeGameId ->
            state.sessionInfo?.toGroupingIdentity(activeGameId)
        }
        if (state.startedSessionId == event.sessionId && gameId != null) {
            recorder.endSession(gameId = gameId, sessionId = event.sessionId, reason = event.reason.name)
        }
        val preserveWeekendGroup = event.reason == SessionEndReason.REPLACED_BY_NEW_SESSION
        state = state.clearSessionState(
            preserveSessionGroupId = preserveWeekendGroup,
            preservedLastEndedSessionType = endedSessionType.takeIf { preserveWeekendGroup },
            preservedLastEndedSessionIdentity = endedSessionIdentity.takeIf { preserveWeekendGroup },
        )
        pendingPreStartSample = null
    }

    private suspend fun onSimDisconnected() {
        val gameId = state.currentGameId
        val sessionId = state.startedSessionId
        if (gameId != null && sessionId != null) {
            recorder.endSession(
                gameId = gameId,
                sessionId = sessionId,
                reason = SessionEndReason.SIM_DISCONNECTED.name,
            )
        }
        state = state.clearSessionState()
        pendingPreStartSample = null
    }

    private suspend fun drainPendingPreStartSample(incomingSessionId: Long) {
        val pending = pendingPreStartSample ?: return
        pendingPreStartSample = null
        if (pending.sessionId == incomingSessionId) {
            handleSample(pending)
        }
    }

    private suspend fun handleSample(sample: TelemetryRecordingSample) {
        val snapshot = state
        if (!snapshot.recordingEnabled) return

        val sessionInfo = snapshot.sessionInfo ?: run {
            bufferPreStartSample(sample)
            return
        }
        if (sample.sessionId != sessionInfo.sessionId) return
        if (snapshot.blockedSessionId == sessionInfo.sessionId) return
        pendingPreStartSample = null

        if (snapshot.startedSessionId == null) {
            startSession(sessionInfo, sample)
        } else if (snapshot.currentDataSource != sample.dataSource) {
            state = state.withCurrentDataSource(sample.dataSource)
            recorder.updateSession(
                TelemetrySessionUpdate(
                    sessionId = sessionInfo.sessionId,
                    gameId = snapshot.currentGameId ?: sample.gameId,
                    airTempC = state.lastAirTempC,
                    trackTempC = state.lastTrackTempC,
                    dataSource = sample.dataSource,
                ),
            )
        }

        if (state.startedSessionId != sample.sessionId) return

        maybeUpdateIdentityLabels(sample.frame)
        maybeUpdateTemperatures(sample)
        val activeGameId = state.currentGameId ?: sample.gameId

        val index = frameIndexBuilder.build(sample.frame)
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = sample.sessionId,
                gameId = activeGameId,
                timestampNs = sample.timestampNs,
                frameId = sample.frameId,
                payloadType = sample.payloadType,
                dataSourceId = sample.dataSourceId,
                payload = sample.payload,
                index = index,
            ),
        )

        if (shouldStopOnLapLimit(sample.frame)) {
            recorder.endSession(activeGameId, sample.sessionId, "lap_limit")
            state = state.withBlockedSession(sample.sessionId)
        }
    }

    private fun bufferPreStartSample(sample: TelemetryRecordingSample) {
        val previous = pendingPreStartSample
        if (previous == null) {
            pendingPreStartSample = sample
            return
        }
        if (previous.sessionId != sample.sessionId || sample.timestampNs >= previous.timestampNs) {
            pendingPreStartSample = sample
        }
    }

    private suspend fun startSession(session: SessionInfo, sample: TelemetryRecordingSample) {
        val startSnapshot = buildSessionStartSnapshot(session, sample.frame)
        val sessionGroupId = resolveSessionGroupId(session = session, gameId = sample.gameId)
        recorder.startSession(startSnapshot.toDescriptor(sample, sessionGroupId))

        state = state.withActiveSession(
            sessionId = session.sessionId,
            gameId = sample.gameId,
            dataSource = sample.dataSource,
            baseCompletedLaps = startSnapshot.baseCompletedLaps,
            carName = startSnapshot.carName,
            trackName = startSnapshot.trackName,
            airTempC = startSnapshot.airTempC,
            trackTempC = startSnapshot.trackTempC,
            sessionGroupId = sessionGroupId,
        )
    }

    private fun buildSessionStartSnapshot(session: SessionInfo, frame: TelemetryFrame): SessionStartSnapshot =
        SessionStartSnapshot(
            baseCompletedLaps = completedLaps(frame),
            sessionType = session.sessionType,
            carModel = session.carModel
                .ifBlank { frame.session?.car?.carModel.orEmpty() }
                .ifBlank { null },
            carId = session.carId ?: frame.session?.car?.carId?.takeIf { it > 0 },
            trackId = session.trackId
                .ifBlank { frame.session?.track?.trackId.orEmpty() }
                .ifBlank { null },
            layoutId = session.layoutId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() },
            carName = frame.session?.car?.carName.normalizeLabel(),
            trackName = frame.session?.track?.trackName.normalizeLabel(),
            airTempC = normalizeTemperature(frame.environment?.airTempC),
            trackTempC = normalizeTemperature(frame.environment?.roadTempC),
        )

    private fun resolveSessionGroupId(session: SessionInfo, gameId: String): String {
        val previousSessionType = state.lastEndedSessionType
        val sameWeekendIdentity = sameWeekendIdentity(
            previous = state.lastEndedSessionIdentity,
            current = session.toGroupingIdentity(gameId),
        )
        val shouldStartNewGroup = when {
            state.sessionGroupId == null -> true
            !sameWeekendIdentity -> true
            session.sessionType != SessionType.PRACTICE -> false
            previousSessionType == null -> true
            previousSessionType == SessionType.PRACTICE -> false
            else -> true
        }
        return if (shouldStartNewGroup) {
            newSessionGroupId(gameId, session.sessionId)
        } else {
            checkNotNull(state.sessionGroupId)
        }
    }

    private fun sameWeekendIdentity(
        previous: SessionGroupingIdentity?,
        current: SessionGroupingIdentity,
    ): Boolean {
        if (previous == null) return false
        return normalizeGameId(previous.gameId) == normalizeGameId(current.gameId) &&
            normalizeIdentityLabel(previous.trackId) == normalizeIdentityLabel(current.trackId) &&
            normalizeIdentityLabel(previous.layoutId) == normalizeIdentityLabel(current.layoutId) &&
            normalizeCarIdentity(previous.carId, previous.carModel) ==
            normalizeCarIdentity(current.carId, current.carModel)
    }

    private suspend fun maybeUpdateIdentityLabels(frame: TelemetryFrame) {
        val sessionId = state.startedSessionId ?: return
        val gameId = state.currentGameId ?: return

        val carName = frame.session?.car?.carName.normalizeLabel()
        val trackName = frame.session?.track?.trackName.normalizeLabel()
        val layoutId = frame.session?.track?.layoutId?.trim()?.takeIf { it.isNotBlank() }
        val currentSessionInfo = state.sessionInfo

        val carChanged = carName != null && carName != state.lastCarName
        val trackChanged = trackName != null && trackName != state.lastTrackName
        val layoutChanged = layoutId != null && layoutId != currentSessionInfo?.layoutId
        if (!carChanged && !trackChanged && !layoutChanged) return

        recorder.updateSession(
            TelemetrySessionUpdate(
                sessionId = sessionId,
                gameId = gameId,
                carName = carName,
                trackName = trackName,
                layoutId = layoutId,
            ),
        )

        var updatedState = state.withUpdatedLabels(
            carName = carName,
            trackName = trackName,
        )
        if (layoutChanged && currentSessionInfo != null) {
            updatedState = updatedState.withSessionInfo(
                currentSessionInfo.copy(layoutId = layoutId),
            )
        }
        state = updatedState
    }

    private suspend fun maybeUpdateTemperatures(sample: TelemetryRecordingSample) {
        val sessionId = state.startedSessionId ?: return
        if (sessionId != sample.sessionId) return

        val airTempC = normalizeTemperature(sample.frame.environment?.airTempC)
        val trackTempC = normalizeTemperature(sample.frame.environment?.roadTempC)

        val prevAir = state.lastAirTempC
        val prevTrack = state.lastTrackTempC
        val airChanged = hasMeaningfulTempChange(prevAir, airTempC)
        val trackChanged = hasMeaningfulTempChange(prevTrack, trackTempC)
        if (!airChanged && !trackChanged) return

        val gameId = state.currentGameId ?: sample.gameId
        recorder.updateSession(
            TelemetrySessionUpdate(
                sessionId = sessionId,
                gameId = gameId,
                airTempC = airTempC,
                trackTempC = trackTempC,
            ),
        )

        state = state.withUpdatedTemperatures(
            airTempC = airTempC,
            trackTempC = trackTempC,
        )
    }

    private fun shouldStopOnLapLimit(frame: TelemetryFrame): Boolean {
        val limit = state.maxRecordedLaps
        if (limit <= 0) return false

        val currentCompleted = completedLaps(frame) ?: return false
        val baseline = state.baseCompletedLaps ?: currentCompleted.also {
            state = state.withLapBaseline(it)
        }
        return currentCompleted - baseline >= limit
    }

    private fun completedLaps(frame: TelemetryFrame): Int? = frame.session?.completedLaps ?: frame.lap?.completedLaps

    private fun SessionType.asSessionTypeString(): String? = if (this == SessionType.UNKNOWN) null else name

    private fun normalizeTemperature(value: Float?): Float? {
        if (value == null || !value.isFinite()) return null
        return value.coerceIn(MIN_REASONABLE_TEMP_C, MAX_REASONABLE_TEMP_C)
    }

    private fun hasMeaningfulTempChange(previous: Float?, current: Float?): Boolean {
        if (current == null) return false
        if (previous == null) return true
        return abs(previous - current) >= TEMP_UPDATE_EPSILON_C
    }

    private fun String?.normalizeLabel(): String? = this?.trim()?.takeIf { it.isNotBlank() }

    private fun normalizeGameId(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeIdentityLabel(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeCarIdentity(carId: Int?, carModel: String?): String = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: normalizeIdentityLabel(carModel)

    private fun SessionInfo.toGroupingIdentity(gameId: String): SessionGroupingIdentity = SessionGroupingIdentity(
        gameId = gameId,
        trackId = trackId,
        layoutId = layoutId?.trim()?.takeIf { it.isNotBlank() },
        carId = carId,
        carModel = carModel,
    )

    private fun SessionStartSnapshot.toDescriptor(
        sample: TelemetryRecordingSample,
        sessionGroupId: String,
    ): TelemetrySessionDescriptor = TelemetrySessionDescriptor(
        sessionId = sample.sessionId,
        gameId = sample.gameId,
        sessionGroupId = sessionGroupId,
        sessionType = sessionType.asSessionTypeString(),
        carModel = carModel,
        carName = carName,
        carId = carId,
        trackId = trackId,
        trackName = trackName,
        layoutId = layoutId,
        airTempC = airTempC,
        trackTempC = trackTempC,
        startedAtMs = System.currentTimeMillis(),
        dataSource = sample.dataSource,
        payloadType = sample.payloadType,
        payloadSize = sample.payload.size,
    )

    private fun newSessionGroupId(gameId: String, sessionId: Long): String {
        val normalizedGameId = gameId.trim().lowercase().ifBlank { "unknown" }
        val suffix = UUID.randomUUID()
            .toString()
            .substring(0, 8)
        return "$normalizedGameId-${System.currentTimeMillis()}-$sessionId-$suffix"
    }

    private data class SessionStartSnapshot(
        val baseCompletedLaps: Int?,
        val sessionType: SessionType,
        val carModel: String?,
        val carId: Int?,
        val trackId: String?,
        val layoutId: String?,
        val carName: String?,
        val trackName: String?,
        val airTempC: Float?,
        val trackTempC: Float?,
    )

    private companion object {
        const val TEMP_UPDATE_EPSILON_C = 0.25f
        const val MIN_REASONABLE_TEMP_C = -80f
        const val MAX_REASONABLE_TEMP_C = 120f
    }
}
