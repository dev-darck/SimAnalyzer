package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.command.RecordCommand

internal interface TelemetryFileCommandProcessor {
    fun handle(command: RecordCommand)
    fun closeAll(endReason: String)
}
