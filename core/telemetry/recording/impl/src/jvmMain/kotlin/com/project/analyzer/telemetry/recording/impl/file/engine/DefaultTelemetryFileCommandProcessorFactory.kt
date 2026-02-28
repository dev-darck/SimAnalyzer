package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.session.TelemetryFileSessionStore

internal class DefaultTelemetryFileCommandProcessorFactory(
    private val sessionStore: TelemetryFileSessionStore,
    private val activeSessionsFactory: TelemetryFileActiveSessionsFactory,
) : TelemetryFileCommandProcessorFactory {

    override fun create(onCompressionReady: (ActiveSession) -> Unit): TelemetryFileCommandProcessor =
        DefaultTelemetryFileCommandProcessor(
            sessionStore = sessionStore,
            sessions = activeSessionsFactory.create(),
            onCompressionReady = onCompressionReady,
        )
}
