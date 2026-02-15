package com.project.analyzer.telemetry.recording.api.recording

import kotlinx.coroutines.flow.SharedFlow

public interface TelemetryRecordingSource {

    public val samples: SharedFlow<TelemetryRecordingSample>

    public fun close()
}
