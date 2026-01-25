package com.project.analyzer.live.domain.model

import com.project.analyzer.live.presentation.LiveScreenState

internal sealed interface LiveTelemetryResult {

    data class SessionEnded(val sessionId: Long) : LiveTelemetryResult

    data object SessionReset : LiveTelemetryResult

    data class Data(val state: LiveScreenState) : LiveTelemetryResult
}
