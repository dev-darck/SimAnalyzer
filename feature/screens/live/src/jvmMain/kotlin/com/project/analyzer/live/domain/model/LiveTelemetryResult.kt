package com.project.analyzer.live.domain.model

import com.project.analyzer.telemetry.api.model.TelemetryFrame

internal sealed interface LiveTelemetryResult {

    data class SessionEnded(val sessionId: Long) : LiveTelemetryResult

    data object SessionReset : LiveTelemetryResult

    data class Data(val frame: TelemetryFrame) : LiveTelemetryResult
}
