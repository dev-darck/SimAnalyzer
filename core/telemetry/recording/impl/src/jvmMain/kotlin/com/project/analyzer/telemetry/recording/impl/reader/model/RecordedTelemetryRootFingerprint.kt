package com.project.analyzer.telemetry.recording.impl.reader.model

internal data class RecordedTelemetryRootFingerprint(
    val rootLastModified: Long,
    val directoryCount: Int,
    val directoryHash: Long,
)
