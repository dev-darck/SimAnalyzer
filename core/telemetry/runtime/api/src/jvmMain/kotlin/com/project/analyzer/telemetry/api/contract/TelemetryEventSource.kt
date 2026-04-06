package com.project.analyzer.telemetry.api.contract

import kotlinx.coroutines.flow.Flow

public interface TelemetryEventSource {

    /**
     * Flow of lifecycle events (SimConnected, SessionStarted, SessionPaused, LapStarted, etc.)
     */
    public val events: Flow<TelemetryLifecycleEvent>
}
