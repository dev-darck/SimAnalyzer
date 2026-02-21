package com.project.analyzer.telemetry.lmu.impl.recording

import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.lmu.api.model.LmuTelemetrySnapshot

internal interface LmuTelemetryRecordingEmitter {

    suspend fun emitSample(sessionId: Long, snapshot: LmuTelemetrySnapshot, frame: TelemetryFrame)
}
