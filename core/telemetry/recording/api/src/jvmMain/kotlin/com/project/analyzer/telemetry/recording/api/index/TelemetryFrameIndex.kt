package com.project.analyzer.telemetry.recording.api.index

/**
 * Compact per-frame index payload used for fast timeline and track scrubbing.
 * Any nullable field is encoded as NaN (floats) or -1 (ints) in the index file.
 */
public data class TelemetryFrameIndex(
    val positionX: Float? = null,
    val positionZ: Float? = null,
    val headingRad: Float? = null,
    val speedKmh: Float? = null,
    val trackPosition: Float? = null,
    val lap: Int? = null,
    val sector: Int? = null,
    val flags: Int = 0,
)
