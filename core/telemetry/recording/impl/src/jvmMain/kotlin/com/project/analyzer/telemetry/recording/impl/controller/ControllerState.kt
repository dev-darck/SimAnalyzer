package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.telemetry.api.contract.SessionInfo
import com.project.analyzer.telemetry.api.contract.SessionType

internal data class ControllerState(
    val sessionInfo: SessionInfo? = null,
    val activeSession: ActiveRecordingSession? = null,
    val boundary: SessionBoundaryState = SessionBoundaryState(),
    val recordingConfig: RecordingConfigState = RecordingConfigState(),
) {

    val startedSessionId: Long? get() = activeSession?.sessionId
    val currentGameId: String? get() = activeSession?.gameId
    val currentDataSource: String? get() = activeSession?.dataSource
    val lastCarName: String? get() = activeSession?.labels?.carName
    val lastTrackName: String? get() = activeSession?.labels?.trackName
    val lastAirTempC: Float? get() = activeSession?.temperatures?.airTempC
    val lastTrackTempC: Float? get() = activeSession?.temperatures?.trackTempC
    val baseCompletedLaps: Int? get() = activeSession?.baseCompletedLaps
    val blockedSessionId: Long? get() = boundary.blockedSessionId
    val sessionGroupId: String? get() = boundary.sessionGroupId
    val lastEndedSessionType: SessionType? get() = boundary.lastEndedSessionType
    val recordingEnabled: Boolean get() = recordingConfig.recordingEnabled
    val maxRecordedLaps: Int get() = recordingConfig.maxRecordedLaps

    fun withRecordingConfig(recordingEnabled: Boolean, maxRecordedLaps: Int): ControllerState = copy(
        recordingConfig = this.recordingConfig.copy(
            recordingEnabled = recordingEnabled,
            maxRecordedLaps = maxRecordedLaps,
        ),
    )

    fun withSessionInfo(sessionInfo: SessionInfo): ControllerState = copy(
        sessionInfo = sessionInfo,
        boundary = boundary.copy(
            blockedSessionId = boundary.blockedSessionId.takeIf { it == sessionInfo.sessionId },
        ),
    )

    fun withObservedSession(sessionInfo: SessionInfo): ControllerState = withSessionInfo(sessionInfo).copy(
        activeSession = null,
    )

    fun withActiveSession(
        sessionId: Long,
        gameId: String,
        dataSource: String?,
        baseCompletedLaps: Int?,
        carName: String?,
        trackName: String?,
        airTempC: Float?,
        trackTempC: Float?,
        sessionGroupId: String?,
    ): ControllerState = copy(
        activeSession = ActiveRecordingSession(
            sessionId = sessionId,
            gameId = gameId,
            dataSource = dataSource,
            labels = SessionLabels(
                carName = carName,
                trackName = trackName,
            ),
            temperatures = SessionTemperatures(
                airTempC = airTempC,
                trackTempC = trackTempC,
            ),
            baseCompletedLaps = baseCompletedLaps,
        ),
        boundary = boundary.copy(
            sessionGroupId = sessionGroupId,
            lastEndedSessionType = null,
        ),
    )

    fun withCurrentDataSource(dataSource: String?): ControllerState = copy(
        activeSession = activeSession?.copy(dataSource = dataSource),
    )

    fun withUpdatedLabels(carName: String?, trackName: String?): ControllerState = copy(
        activeSession = activeSession?.let { current ->
            current.copy(
                labels = current.labels.copy(
                    carName = carName ?: current.labels.carName,
                    trackName = trackName ?: current.labels.trackName,
                ),
            )
        },
    )

    fun withUpdatedTemperatures(airTempC: Float?, trackTempC: Float?): ControllerState = copy(
        activeSession = activeSession?.let { current ->
            current.copy(
                temperatures = current.temperatures.copy(
                    airTempC = airTempC ?: current.temperatures.airTempC,
                    trackTempC = trackTempC ?: current.temperatures.trackTempC,
                ),
            )
        },
    )

    fun withLapBaseline(baseCompletedLaps: Int): ControllerState = copy(
        activeSession = activeSession?.copy(baseCompletedLaps = baseCompletedLaps),
    )

    fun clearSessionState(
        preserveSessionGroupId: Boolean = false,
        preservedLastEndedSessionType: SessionType? = null,
    ): ControllerState = copy(
        sessionInfo = null,
        activeSession = null,
        boundary = boundary.copy(
            blockedSessionId = null,
            sessionGroupId = if (preserveSessionGroupId) boundary.sessionGroupId else null,
            lastEndedSessionType = preservedLastEndedSessionType,
        ),
    )

    fun withBlockedSession(sessionId: Long): ControllerState = copy(
        activeSession = null,
        boundary = boundary.copy(
            blockedSessionId = sessionId,
            sessionGroupId = null,
            lastEndedSessionType = null,
        ),
    )
}

internal data class ActiveRecordingSession(
    val sessionId: Long,
    val gameId: String,
    val dataSource: String?,
    val labels: SessionLabels = SessionLabels(),
    val temperatures: SessionTemperatures = SessionTemperatures(),
    val baseCompletedLaps: Int? = null,
)

internal data class SessionLabels(val carName: String? = null, val trackName: String? = null)

internal data class SessionTemperatures(val airTempC: Float? = null, val trackTempC: Float? = null)

internal data class SessionBoundaryState(
    val blockedSessionId: Long? = null,
    val sessionGroupId: String? = null,
    val lastEndedSessionType: SessionType? = null,
)

internal data class RecordingConfigState(val recordingEnabled: Boolean = true, val maxRecordedLaps: Int = 0)
