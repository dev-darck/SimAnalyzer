package com.project.analyzer.inputs.domain.model

sealed interface InputsResult {

    data class SessionStarted(val sessionId: Long) : InputsResult
    data class SessionResumed(val sessionId: Long) : InputsResult
    data class SessionPaused(val sessionId: Long) : InputsResult
    data class SessionEnded(val sessionId: Long) : InputsResult

    data class Sample(
        val throttle: Float,
        val brake: Float,
        val clutch: Float,
        val steerRadians: Float,
        val timestampNs: Long,
    ) : InputsResult
}
