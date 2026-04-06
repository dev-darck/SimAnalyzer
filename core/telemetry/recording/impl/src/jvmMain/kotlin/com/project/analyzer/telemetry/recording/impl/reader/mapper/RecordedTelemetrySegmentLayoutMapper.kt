package com.project.analyzer.telemetry.recording.impl.reader.mapper

import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySegment
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetrySegmentLayout

internal fun buildSegmentLayout(
    locations: List<RecordedTelemetrySessionLocation>,
): RecordedTelemetrySegmentLayout {
    if (locations.isEmpty()) {
        return RecordedTelemetrySegmentLayout(
            segments = emptyList(),
            segmentIdByLocationId = emptyMap(),
        )
    }
    val groupedLocations = linkedMapOf<String, MutableList<RecordedTelemetrySessionLocation>>()
    locations
        .sortedBy { location -> location.metadata.startedAtMs }
        .forEach { location ->
            val key = location.metadata.sessionType.normalizedSegmentKey()
                ?: "location:${location.persistedSessionId}"
            groupedLocations.getOrPut(key) { mutableListOf() } += location
        }
    val segments = groupedLocations.values.map(List<RecordedTelemetrySessionLocation>::toDecodedSegment)
    val segmentIdByLocationId = buildMap {
        groupedLocations.values.forEach { group ->
            val segmentId = group.first().persistedSessionId
            group.forEach { location ->
                put(location.persistedSessionId, segmentId)
            }
        }
    }
    return RecordedTelemetrySegmentLayout(
        segments = segments,
        segmentIdByLocationId = segmentIdByLocationId,
    )
}

private fun List<RecordedTelemetrySessionLocation>.toDecodedSegment(): DecodedRecordedTelemetrySegment {
    val latest = maxByOrNull { location -> location.metadata.startedAtMs } ?: first()
    val endedAtMs = maxOfOrNull { location -> location.metadata.endedAtMs ?: Long.MIN_VALUE }
        ?.takeIf { value -> value != Long.MIN_VALUE }
    return DecodedRecordedTelemetrySegment(
        segmentId = first().persistedSessionId,
        sessionType = latest.metadata.sessionType,
        carModel = latest.metadata.carModel,
        carName = asSequence()
            .mapNotNull { location -> location.metadata.carName?.takeIf(String::isNotBlank) }
            .lastOrNull()
            ?: latest.metadata.carName,
        trackId = latest.metadata.trackId,
        trackName = asSequence()
            .mapNotNull { location -> location.metadata.trackName?.takeIf(String::isNotBlank) }
            .lastOrNull()
            ?: latest.metadata.trackName,
        startedAtMs = minOf { location -> location.metadata.startedAtMs },
        endedAtMs = endedAtMs,
    )
}

private fun String?.normalizedSegmentKey(): String? = this
    ?.trim()
    ?.takeIf(String::isNotBlank)
    ?.uppercase()
