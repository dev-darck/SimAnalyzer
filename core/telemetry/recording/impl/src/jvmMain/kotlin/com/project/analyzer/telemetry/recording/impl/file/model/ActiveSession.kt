package com.project.analyzer.telemetry.recording.impl.file.model

import com.project.analyzer.telemetry.recording.impl.file.FLUSH_INTERVAL_NS
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File

internal class ActiveSession(
    var metadata: SessionMetadata,
    val metaFile: File,
    val dataOut: DataOutputStream,
    val indexOut: DataOutputStream,
    val eventsWriter: BufferedWriter,
) {

    val sampleIntervalNs: Long = if (metadata.samplingRateHz > 0) {
        1_000_000_000L / metadata.samplingRateHz
    } else {
        0L
    }

    var headerWritten: Boolean = false
    var isPaused: Boolean = false
    var frameCount: Long = 0
    var receivedFrames: Long = 0
    var droppedFrames: Long = 0
    var skippedFrames: Long = 0
    var framesBytesWritten: Long = 0
    var firstFrameTimestampNs: Long? = null
    var lastFrameTimestampNs: Long? = null
    var payloadSizeMismatchLogged: Boolean = false
    var isClosed: Boolean = false

    private var lastSampledTimestampNs: Long = 0L

    val flushLimiter = NsRateLimiter(FLUSH_INTERVAL_NS)

    fun shouldSample(timestampNs: Long): Boolean {
        if (sampleIntervalNs <= 0L) return true
        if (lastSampledTimestampNs == 0L) {
            lastSampledTimestampNs = timestampNs
            return true
        }
        val elapsed = timestampNs - lastSampledTimestampNs
        if (elapsed >= sampleIntervalNs) {
            lastSampledTimestampNs = timestampNs
            return true
        }
        return false
    }

    fun markReceived() {
        receivedFrames += 1
    }

    fun markDropped() {
        droppedFrames += 1
    }

    fun markSkipped() {
        skippedFrames += 1
    }

    fun markWritten(timestampNs: Long, bytesWritten: Long) {
        frameCount += 1
        framesBytesWritten += bytesWritten
        if (firstFrameTimestampNs == null) {
            firstFrameTimestampNs = timestampNs
        }
        lastFrameTimestampNs = timestampNs
    }

    fun shouldFlush(nowNs: Long): Boolean = flushLimiter.shouldLog(nowNs)

    fun recordingDurationSec(): Double {
        val first = firstFrameTimestampNs ?: return 0.0
        val last = lastFrameTimestampNs ?: return 0.0
        return (last - first) / 1_000_000_000.0
    }
}

class NsRateLimiter(private val intervalNs: Long) {

    private var nextNs: Long = 0L

    fun shouldLog(nowNs: Long): Boolean {
        if (nowNs >= nextNs) {
            nextNs = nowNs + intervalNs
            return true
        }
        return false
    }
}