package com.project.analyzer.fuel.domain.model

import com.project.analyzer.utils.toSlugId

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

        private fun normalizeTrackId(value: String?): String = value.orEmpty().toSlugId(allowDash = true)
    }
}
