package com.project.analyzer.telemetry.recording.api.index

public object TelemetryFrameIndexFlags {

    public const val IN_PIT: Int = 1
    public const val IN_PIT_LANE: Int = 1 shl 1
    public const val INVALID_LAP: Int = 1 shl 2
}
