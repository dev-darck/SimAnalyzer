package com.analyzer.session.data.repository.impl

import kotlinx.serialization.Serializable

@Serializable
internal data class RecordedSessionMetadata(
    val sessionId: Long,
    val gameId: String,
    val sessionGroupId: String? = null,
    val sessionType: String?,
    val carModel: String?,
    val carName: String? = null,
    val carId: Int? = null,
    val trackId: String?,
    val trackName: String? = null,
    val layoutId: String? = null,
    val airTempC: Float? = null,
    val trackTempC: Float? = null,
    val startedAtMs: Long,
    val endedAtMs: Long?,
    val isSaved: Boolean = true,
    val dataSource: String?,
    val payloadType: String,
    val payloadSize: Int,
    val samplingRateHz: Int,
    val frameCount: Long,
    val receivedFrames: Long,
    val droppedFrames: Long,
    val firstTimestampNs: Long?,
    val lastTimestampNs: Long?,
    val fileVersion: Int,
    val indexVersion: Int,
    val indexRecordSize: Int,
    val indexFields: List<String>,
    val framesFile: String,
    val indexFile: String,
    val eventsFile: String,
    val compression: String? = null,
)
