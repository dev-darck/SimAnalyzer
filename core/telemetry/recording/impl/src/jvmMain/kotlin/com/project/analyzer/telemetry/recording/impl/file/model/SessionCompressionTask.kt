package com.project.analyzer.telemetry.recording.impl.file.model

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import java.io.File

internal data class SessionCompressionTask(val metaFile: File, val metadata: RecordedTelemetrySessionMetadata)
