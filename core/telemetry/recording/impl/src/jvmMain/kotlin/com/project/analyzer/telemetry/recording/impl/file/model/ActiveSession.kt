package com.project.analyzer.telemetry.recording.impl.file.model

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.impl.file.FLUSH_INTERVAL_NS
import com.project.analyzer.telemetry.recording.impl.file.METADATA_PERSIST_INTERVAL_NS
import com.project.analyzer.telemetry.recording.impl.file.codec.FrameStorageSessionCodec
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.File

internal class ActiveSession(
    var metadata: RecordedTelemetrySessionMetadata,
    val metaFile: File,
    val frameStorageCodec: FrameStorageSessionCodec,
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
    var firstReceivedTimestampNs: Long? = null
    var lastReceivedTimestampNs: Long? = null
    var firstFrameTimestampNs: Long? = null
    var lastFrameTimestampNs: Long? = null
    var duplicateFrames: Long = 0
    var outOfOrderFrames: Long = 0
    var payloadSizeMismatchLogged: Boolean = false
    var isClosed: Boolean = false
    private var metadataDirty: Boolean = true

    private var lastSampledTimestampNs: Long = 0L
    private var lastWrittenFrameId: Long? = null
    private var lastWrittenDataSourceId: Int? = null

    private val outputFlushLimiter = NsRateLimiter(FLUSH_INTERVAL_NS)
    private val metadataPersistLimiter = NsRateLimiter(METADATA_PERSIST_INTERVAL_NS)

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

    fun markReceived(timestampNs: Long) {
        receivedFrames += 1
        metadataDirty = true
        if (firstReceivedTimestampNs == null) {
            firstReceivedTimestampNs = timestampNs
        }
        val previousLast = lastReceivedTimestampNs
        if (previousLast == null || timestampNs >= previousLast) {
            lastReceivedTimestampNs = timestampNs
        }
    }

    fun markDropped() {
        droppedFrames += 1
        metadataDirty = true
    }

    fun markDuplicate() {
        duplicateFrames += 1
        droppedFrames += 1
        metadataDirty = true
    }

    fun markOutOfOrder() {
        outOfOrderFrames += 1
        droppedFrames += 1
        metadataDirty = true
    }

    fun markSkipped() {
        skippedFrames += 1
        metadataDirty = true
    }

    fun markWritten(timestampNs: Long, frameId: Long, dataSourceId: Int, bytesWritten: Long) {
        frameCount += 1
        framesBytesWritten += bytesWritten
        metadataDirty = true
        if (firstFrameTimestampNs == null) {
            firstFrameTimestampNs = timestampNs
        }
        lastFrameTimestampNs = timestampNs
        lastWrittenFrameId = frameId
        lastWrittenDataSourceId = dataSourceId
    }

    fun classifyFrame(timestampNs: Long, frameId: Long, dataSourceId: Int): FrameClass {
        val lastTs = lastFrameTimestampNs ?: return FrameClass.Accepted
        if (timestampNs < lastTs) return FrameClass.OutOfOrder
        if (timestampNs == lastTs) {
            val sameFingerprint = lastWrittenFrameId == frameId && lastWrittenDataSourceId == dataSourceId
            return if (sameFingerprint) FrameClass.Duplicate else FrameClass.OutOfOrder
        }
        return FrameClass.Accepted
    }

    fun markMetadataDirty() {
        metadataDirty = true
    }

    fun markMetadataPersisted() {
        metadataDirty = false
    }

    fun shouldFlushOutputs(nowNs: Long): Boolean = outputFlushLimiter.shouldLog(nowNs)

    fun shouldPersistMetadata(nowNs: Long): Boolean = metadataDirty && metadataPersistLimiter.shouldLog(nowNs)

    fun recordingDurationSec(): Double {
        val first = firstFrameTimestampNs ?: return 0.0
        val last = lastFrameTimestampNs ?: return 0.0
        return (last - first) / 1_000_000_000.0
    }

    fun sourceDurationSec(): Double {
        val first = firstReceivedTimestampNs ?: return 0.0
        val last = lastReceivedTimestampNs ?: return 0.0
        return (last - first) / 1_000_000_000.0
    }
}
