package com.project.analyzer.telemetry.api.contract

public interface TelemetryLifecycle : TelemetryReadSource, TelemetryRuntimeController

public sealed interface TelemetryLifecycleEvent {
    public data object SimConnected : TelemetryLifecycleEvent
    public data object SimDisconnected : TelemetryLifecycleEvent

    /**
     * New session instance started.
     * Note: some fields can be empty/UNKNOWN at start and arrive later via SessionUpdated.
     */
    public data class SessionStarted(val session: SessionInfo = SessionInfo(0L, SessionType.UNKNOWN, "", "")) :
        TelemetryLifecycleEvent

    /**
     * Session metadata changed (sticky updates; blanks do not overwrite).
     */
    public data class SessionUpdated(val session: SessionInfo, val changed: Set<SessionField>) :
        TelemetryLifecycleEvent

    /**
     * We left active telemetry (usually ESC/menu or physics stream becomes inactive).
     * This is NOT a final end; it can be resumed.
     */
    public data class SessionPaused(
        val sessionId: Long,
        val reason: SessionPauseReason = SessionPauseReason.NOT_IN_SESSION,
    ) : TelemetryLifecycleEvent

    public data class SessionResumed(val sessionId: Long) : TelemetryLifecycleEvent

    /**
     * Final end of session (disconnect or replaced by a new session).
     */
    public data class SessionEnded(
        val sessionId: Long = 0L,
        val reason: SessionEndReason = SessionEndReason.SIM_DISCONNECTED,
    ) : TelemetryLifecycleEvent

    public data class LapStarted(val lapNumber: Int) : TelemetryLifecycleEvent
    public data class LapFinished(val lapNumber: Int, val validity: LapValidity) : TelemetryLifecycleEvent
}

public data class SessionInfo(
    val sessionId: Long,
    val sessionType: SessionType,
    val carModel: String,
    val trackId: String,
    val carId: Int? = null,
    val layoutId: String? = null,
)

public enum class SessionField {
    SESSION_TYPE,
    CAR_MODEL,
    CAR_ID,
    TRACK_ID,
    TRACK_LAYOUT_ID,
}

public enum class SessionPauseReason {
    /** We are not in active session state (menu / paused / inactive physics) */
    NOT_IN_SESSION,
}

public enum class SessionEndReason {
    /** Game disconnected while session existed (running or paused) */
    SIM_DISCONNECTED,

    /** A new session replaced the previous one (restart / new race / etc) */
    REPLACED_BY_NEW_SESSION,

    /** A real main-menu return replaced the previous session and should start a new recording group */
    REPLACED_AFTER_MAIN_MENU,
}
