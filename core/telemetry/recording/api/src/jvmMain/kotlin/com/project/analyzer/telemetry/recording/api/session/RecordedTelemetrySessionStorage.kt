package com.project.analyzer.telemetry.recording.api.session

import java.io.File

public interface RecordedTelemetrySessionStorage {

    public suspend fun loadBundles(forceRefresh: Boolean = false): List<RecordedTelemetrySessionBundle>

    public suspend fun findBundle(sessionId: Long, forceRefresh: Boolean = false): RecordedTelemetrySessionBundle?

    public suspend fun saveSession(sessionId: Long): Boolean

    public suspend fun deleteSession(sessionId: Long): Boolean

    public fun resolveFramesFile(location: RecordedTelemetrySessionLocation): File?

    public fun resolveIndexFile(location: RecordedTelemetrySessionLocation): File?
}
