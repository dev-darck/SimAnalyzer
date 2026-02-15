package com.project.analyzer.telemetry.recording.impl.file.model

import com.project.analyzer.telemetry.recording.impl.file.FLUSH_INTERVAL_NS
import com.project.analyzer.utils.NsRateLimiter
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

    var headerWritten: Boolean = false
    var isPaused: Boolean = false
    var frameCount: Long = 0
    var receivedFrames: Long = 0
    var droppedFrames: Long = 0
    var framesBytesWritten: Long = 0
    var firstFrameTimestampNs: Long? = null
    var lastFrameTimestampNs: Long? = null
    var payloadSizeMismatchLogged: Boolean = false
    var isClosed: Boolean = false

    val flushLimiter = NsRateLimiter(FLUSH_INTERVAL_NS)

    fun markReceived() {
        receivedFrames += 1
    }

    fun markDropped() {
        droppedFrames += 1
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
}
