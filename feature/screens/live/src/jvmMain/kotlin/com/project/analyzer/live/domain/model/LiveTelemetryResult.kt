package com.project.analyzer.live.domain.model

import com.project.analyzer.live.presentation.LiveScreenState

internal sealed interface LiveTelemetryResult {
    data object SessionEnded : LiveTelemetryResult
    data object SessionReset : LiveTelemetryResult
    data object NoData : LiveTelemetryResult
    data class Data(val state: LiveScreenState) : LiveTelemetryResult
}
