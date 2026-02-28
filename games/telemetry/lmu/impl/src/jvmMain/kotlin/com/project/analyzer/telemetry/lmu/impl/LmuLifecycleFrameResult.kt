package com.project.analyzer.telemetry.lmu.impl

import com.project.analyzer.telemetry.api.model.TelemetryFrame

internal data class LmuLifecycleFrameResult(val frame: TelemetryFrame, val sampleSessionId: Long?)
