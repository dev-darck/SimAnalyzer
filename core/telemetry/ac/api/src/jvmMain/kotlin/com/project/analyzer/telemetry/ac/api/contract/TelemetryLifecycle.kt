package com.project.analyzer.telemetry.ac.api.contract

import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

public interface TelemetryLifecycle {

    /**
     * Shared flow of telemetry frames. Runs once, shared among all subscribers.
     * Automatically starts when first subscriber appears.
     */
    public val frames: SharedFlow<TelemetryFrame>

    /**
     * Flow of lifecycle events (SimConnected, SessionStarted, LapStarted, etc.)
     */
    public val events: Flow<TelemetryLifecycleEvent>

    /**
     * Stops telemetry collection and releases resources.
     */
    public suspend fun finishTelemetry()
}

public sealed interface TelemetryLifecycleEvent {

    public data object SimConnected : TelemetryLifecycleEvent
    public data object SimDisconnected : TelemetryLifecycleEvent

    public data class SessionStarted(val sessionType: SessionType) : TelemetryLifecycleEvent
    public data object SessionEnded : TelemetryLifecycleEvent
    public data class LapStarted(val lapNumber: Int) : TelemetryLifecycleEvent
    public data class LapFinished(val lapNumber: Int, val validity: LapValidity) : TelemetryLifecycleEvent
}
