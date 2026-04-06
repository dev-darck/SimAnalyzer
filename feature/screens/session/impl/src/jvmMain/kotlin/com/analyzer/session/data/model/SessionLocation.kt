package com.analyzer.session.data.model

import com.analyzer.session.data.analysis.IndexAnalysis
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import java.io.File

internal data class SessionLocation(
    val source: RecordedTelemetrySessionLocation,
    val summary: RecordedSessionSummary,
    val analysis: IndexAnalysis? = null,
) {

    val dir: File
        get() = source.dir

    val metadata = source.metadata
}
