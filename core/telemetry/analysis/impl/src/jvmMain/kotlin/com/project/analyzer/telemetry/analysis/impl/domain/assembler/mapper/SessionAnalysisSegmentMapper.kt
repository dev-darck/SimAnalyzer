package com.project.analyzer.telemetry.analysis.impl.domain.assembler.mapper

import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySegment

/**
 * Builds immutable segment snapshots so later resolvers can compare laps without touching raw frames again.
 */
internal fun DecodedRecordedTelemetrySegment.toSessionAnalysisSegment(): SessionAnalysisSegment =
    SessionAnalysisSegment(
        segmentId = segmentId,
        sessionTypeLabel = sessionType.orEmpty(),
        sessionLabel = sessionType.orEmpty(),
        carLabel = carName ?: carModel.orEmpty(),
        trackLabel = trackName ?: trackId.orEmpty(),
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
    )
