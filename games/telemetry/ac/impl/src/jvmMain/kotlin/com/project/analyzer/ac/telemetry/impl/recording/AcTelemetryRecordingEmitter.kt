package com.project.analyzer.ac.telemetry.impl.recording

import com.project.analyzer.ac.telemetry.impl.internal.AcRawSnapshot
import com.project.analyzer.ac.telemetry.impl.internal.DataSourceType
import com.project.analyzer.telemetry.api.model.TelemetryFrame

interface AcTelemetryRecordingEmitter {

    suspend fun emitSample(
        sessionId: Long,
        snapshot: AcRawSnapshot,
        frame: TelemetryFrame,
        dataSource: DataSourceType,
    )
}
