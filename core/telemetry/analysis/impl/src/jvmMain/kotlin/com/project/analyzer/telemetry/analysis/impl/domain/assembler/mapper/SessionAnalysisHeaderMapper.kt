package com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper

import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata

/**
 * Maps raw recording metadata into the compact header model shared by the analysis UI.
 */
internal fun RecordedTelemetrySessionMetadata.toSessionAnalysisHeader(
    firstPayload: RecordedTelemetryPayload?,
): SessionAnalysisHeader = SessionAnalysisHeader(
    gameId = gameId,
    sessionTypeLabel = sessionType?.takeIf { it.isNotBlank() } ?: firstPayload?.sessionTypeLabel.orEmpty(),
    sessionPhaseLabel = firstPayload?.sessionPhaseLabel.orEmpty(),
    carLabel = carName ?: carModel ?: firstPayload?.carLabel.orEmpty(),
    trackLabel = trackName ?: trackId ?: firstPayload?.trackLabel.orEmpty(),
    trackLayoutLabel = firstPayload?.trackLayoutLabel.orEmpty(),
    startedAtMs = startedAtMs,
    airTempC = airTempC ?: firstPayload?.airTempC,
    trackTempC = trackTempC ?: firstPayload?.roadTempC,
)
