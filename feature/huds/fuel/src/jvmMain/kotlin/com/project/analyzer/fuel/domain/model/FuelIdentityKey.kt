package com.project.analyzer.fuel.domain.model

import java.util.Locale

internal data class FuelIdentityKey(
    val carModel: String,
    val trackId: String,
) {

    val composite: String
        get() = "$carModel|$trackId"

    companion object {

        val UNKNOWN: FuelIdentityKey = FuelIdentityKey(
            carModel = "__unknown_car__",
            trackId = "__unknown_track__"
        )

        fun from(carModel: String?, trackId: String?): FuelIdentityKey? {
            val normalizedCar = normalize(carModel)
            val normalizedTrack = normalize(trackId)
            if (normalizedCar.isBlank() || normalizedTrack.isBlank()) return null
            return FuelIdentityKey(
                carModel = normalizedCar,
                trackId = normalizedTrack
            )
        }

        private fun normalize(value: String?): String {
            return value
                .orEmpty()
                .trim()
                .lowercase(Locale.US)
                .replace(Regex("\\s+"), "_")
                .replace(Regex("[^a-z0-9_-]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
        }
    }
}
