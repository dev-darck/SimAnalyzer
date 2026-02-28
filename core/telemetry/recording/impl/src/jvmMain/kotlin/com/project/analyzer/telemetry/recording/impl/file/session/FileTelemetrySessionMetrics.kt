package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.impl.file.INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession

internal class FileTelemetrySessionMetrics(
    val durationSec: Double,
    val effectiveHz: Double,
    val sourceHz: Double,
    val sourceLimited: Boolean,
    val totalDataBytes: Long,
    val indexBytes: Long,
) {
    val totalBytes: Long = totalDataBytes + indexBytes

    companion object {
        fun from(session: ActiveSession): FileTelemetrySessionMetrics {
            val durationSec = session.recordingDurationSec()
            val sourceDurationSec = session.sourceDurationSec()
            val effectiveHz = if (durationSec > 0) session.frameCount.toDouble() / durationSec else 0.0
            val sourceHz = if (sourceDurationSec > 0) {
                session.receivedFrames.toDouble() / sourceDurationSec
            } else {
                0.0
            }
            val sourceLimited = sourceHz > 0.0 &&
                sourceHz + SOURCE_RATE_EPSILON_HZ < session.metadata.samplingRateHz
            val totalDataBytes = session.framesBytesWritten
            val indexBytes = (session.frameCount * INDEX_RECORD_SIZE) + INDEX_HEADER_SIZE
            return FileTelemetrySessionMetrics(
                durationSec = durationSec,
                effectiveHz = effectiveHz,
                sourceHz = sourceHz,
                sourceLimited = sourceLimited,
                totalDataBytes = totalDataBytes,
                indexBytes = indexBytes,
            )
        }

        private const val INDEX_HEADER_SIZE = 16L
        private const val SOURCE_RATE_EPSILON_HZ = 2.0
    }
}
