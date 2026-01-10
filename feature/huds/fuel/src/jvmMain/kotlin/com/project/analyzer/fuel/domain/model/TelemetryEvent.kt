package com.project.analyzer.fuel.domain.model

import com.project.analyzer.telemetry.ac.api.contract.TelemetryLifecycleEvent

internal sealed interface TelemetryEvent {
    data object SessionStarted : TelemetryEvent

    data object SessionEnded : TelemetryEvent
}

internal fun TelemetryLifecycleEvent.toTelemetryEvent(): TelemetryEvent? = when (this) {
    is TelemetryLifecycleEvent.SessionStarted -> TelemetryEvent.SessionStarted
    
    is TelemetryLifecycleEvent.SessionEnded,
    is TelemetryLifecycleEvent.SimDisconnected -> TelemetryEvent.SessionEnded

    else -> null
}
