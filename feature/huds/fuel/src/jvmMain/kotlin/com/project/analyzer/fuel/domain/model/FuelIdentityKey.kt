package com.project.analyzer.fuel.domain.model

import java.util.Locale

internal data class FuelIdentityKey(val carId: Int, val trackId: String) {

    val composite: String
        get() = "$carId|$trackId"

    companion object {

        val UNKNOWN: FuelIdentityKey = FuelIdentityKey(
            carId = -1,
            trackId = "__unknown_track__",
        )

        fun from(carId: Int?, trackId: String?): FuelIdentityKey? {
            val normalizedTrack = normalizeTrackId(trackId)
            val normalizedCarId = carId?.takeIf { it > 0 } ?: return null
            if (normalizedTrack.isBlank()) return null
            return FuelIdentityKey(
                carId = normalizedCarId,
                trackId = normalizedTrack,
            )
        }

        private fun normalizeTrackId(value: String?): String = value
            .orEmpty()
            .trim()
            .lowercase(Locale.US)
            .replace(WHITESPACE_REGEX, "_")
            .replace(NON_SLUG_CHARS_REGEX, "_")
            .replace(MULTIPLE_UNDERSCORES_REGEX, "_")
            .trim('_')

        private val WHITESPACE_REGEX = Regex("\\s+")
        private val NON_SLUG_CHARS_REGEX = Regex("[^a-z0-9_-]")
        private val MULTIPLE_UNDERSCORES_REGEX = Regex("_+")
    }
}
