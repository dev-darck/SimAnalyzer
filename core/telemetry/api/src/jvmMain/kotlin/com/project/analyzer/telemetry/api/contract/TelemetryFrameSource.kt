package com.project.analyzer.telemetry.api.contract

import com.project.analyzer.telemetry.api.model.TelemetryFrame
import kotlinx.coroutines.flow.SharedFlow

public interface TelemetryFrameSource {

    public val frames: SharedFlow<TelemetryFrame>
}
