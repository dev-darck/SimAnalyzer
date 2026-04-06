package com.project.analyzer.telemetry.recording.impl.reader.model

internal data class RecordedTelemetryIndexRecord(
    val timestampNs: Long,
    val frameId: Long,
    val dataSourceId: Int,
    val payloadOffset: Long,
    val payloadSize: Int,
    val positionX: Float?,
    val positionY: Float?,
    val headingRad: Float?,
    val speedKmh: Float?,
    val trackPosition: Float?,
    val lap: Int,
    val sector: Int,
    val flags: Int,
)
