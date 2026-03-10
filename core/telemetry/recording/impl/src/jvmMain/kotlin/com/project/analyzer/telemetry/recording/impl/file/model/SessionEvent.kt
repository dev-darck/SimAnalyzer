package com.project.analyzer.telemetry.recording.impl.file.model

import kotlinx.serialization.Serializable

@Serializable
internal data class SessionEvent(
    val type: String,
    val atMs: Long,
    val sessionId: Long,
    val reason: String? = null,
    val sessionType: String? = null,
    val carModel: String? = null,
    val carName: String? = null,
    val carId: Int? = null,
    val trackId: String? = null,
    val trackName: String? = null,
    val layoutId: String? = null,
    val payloadType: String? = null,
    val payloadSize: Int? = null,
)
