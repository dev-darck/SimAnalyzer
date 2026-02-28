package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.telemetry.recording.impl.file.model.SessionCompressionTask

internal interface TelemetrySessionCompressionService {
    fun compress(task: SessionCompressionTask)
}
