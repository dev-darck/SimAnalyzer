package com.project.analyzer.ac.telemetry.impl

import com.project.analyzer.telemetry.api.model.TelemetryFrame

internal data class AcLifecycleFrameResult(val frame: TelemetryFrame, val sampleSessionId: Long?)
