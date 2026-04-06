package com.project.analyzer.telemetry.recording.impl.reader.model

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle

internal data class RecordedTelemetryBundleIndex(
    val rootPath: String,
    val fingerprint: RecordedTelemetryRootFingerprint,
    val bundles: List<RecordedTelemetrySessionBundle>,
    val sessionIndex: Map<Long, RecordedTelemetrySessionBundle>,
)
