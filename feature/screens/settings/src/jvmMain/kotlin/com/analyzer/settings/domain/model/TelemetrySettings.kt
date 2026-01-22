package com.analyzer.settings.domain.model

data class TelemetrySettings(
    val samplingRateHz: Int = DEFAULT_SAMPLING_RATE_HZ,
    val storageLocation: String = ""
) {

    internal companion object {

        const val MIN_SAMPLING_RATE_HZ = 10
        const val MAX_SAMPLING_RATE_HZ = 100
        const val DEFAULT_SAMPLING_RATE_HZ = 70
    }
}
