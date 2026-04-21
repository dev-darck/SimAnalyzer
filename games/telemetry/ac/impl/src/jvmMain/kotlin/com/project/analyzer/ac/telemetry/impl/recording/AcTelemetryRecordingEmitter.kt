package com.project.analyzer.ac.telemetry.impl.recording

import com.project.analyzer.ac.telemetry.impl.internal.poll.DataSourceType
import com.project.analyzer.ac.telemetry.impl.internal.poll.snapshot.AcPollSnapshot
import com.project.analyzer.telemetry.api.model.TelemetryFrame

interface AcTelemetryRecordingEmitter {

    suspend fun emitSample(sessionId: Long, snapshot: AcPollSnapshot, frame: TelemetryFrame, dataSource: DataSourceType)
}
