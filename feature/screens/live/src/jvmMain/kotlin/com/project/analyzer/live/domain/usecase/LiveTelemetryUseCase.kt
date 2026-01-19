package com.project.analyzer.live.domain.usecase

import com.project.analyzer.live.domain.model.LiveTelemetryResult
import kotlinx.coroutines.flow.Flow

internal interface LiveTelemetryUseCase {

    val telemetryFlow: Flow<LiveTelemetryResult>
}
