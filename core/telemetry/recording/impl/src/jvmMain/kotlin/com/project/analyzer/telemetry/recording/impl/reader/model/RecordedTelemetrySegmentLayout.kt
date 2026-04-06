package com.project.analyzer.telemetry.recording.impl.reader.model

import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySegment

internal data class RecordedTelemetrySegmentLayout(
    val segments: List<DecodedRecordedTelemetrySegment>,
    val segmentIdByLocationId: Map<Long, Long>,
)
