package com.project.analyzer.fuel.data

import com.project.analyzer.fuel.data.model.SavedFuelData
import com.project.analyzer.fuel.domain.model.FuelIdentityKey
import com.project.analyzer.fuel.domain.repository.FuelRepository
import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.SessionPref
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Inject
@SingleIn(HudScope::class)
internal class FuelRepositoryImpl(
    @param:SessionPref
    private val preference: Preference,
    private val json: Json
) : FuelRepository {

    override suspend fun updateIfBetter(
        carModel: String,
        trackId: String,
        peakLitersPerLap: Double?,
        bestValidLapTimeMs: Int?
    ) {
        if (peakLitersPerLap == null && bestValidLapTimeMs == null) return
        if (peakLitersPerLap?.isFinite() == false) return

        val existing = load(carModel, trackId)

        val newPeak = when {
            peakLitersPerLap == null || peakLitersPerLap <= 0 -> existing?.peakLitersPerLap
            existing?.peakLitersPerLap == null -> peakLitersPerLap
            peakLitersPerLap > existing.peakLitersPerLap -> peakLitersPerLap
            else -> existing.peakLitersPerLap
        }

        val newBestLapTime = when {
            bestValidLapTimeMs == null || bestValidLapTimeMs <= 0 -> existing?.bestValidLapTimeMs
            existing?.bestValidLapTimeMs == null -> bestValidLapTimeMs
            bestValidLapTimeMs < existing.bestValidLapTimeMs -> bestValidLapTimeMs
            else -> existing.bestValidLapTimeMs
        }

        if (newPeak == null && newBestLapTime == null) return

        val improvedPeak = newPeak != existing?.peakLitersPerLap
        val improvedBest = newBestLapTime != existing?.bestValidLapTimeMs
        if (!improvedPeak && !improvedBest) return

        val dto = FuelDataDto(
            peakLitersPerLap = newPeak,
            bestValidLapTimeMs = newBestLapTime,
            savedAtEpochMs = System.currentTimeMillis()
        )

        val key = buildKey(carModel, trackId) ?: return
        logger.info { "FuelRepositoryImpl save fuel consumption data: $dto by key: ${key.name}" }
        preference.put(key to json.encodeToString(FuelDataDto.serializer(), dto))
    }

    override suspend fun load(carModel: String, trackId: String): SavedFuelData? {
        val key = buildKey(carModel, trackId) ?: return null
        val jsonString = preference.getOrNull(key) ?: return null

        return try {
            val dto = json.decodeFromString(FuelDataDto.serializer(), jsonString)
            SavedFuelData(
                peakLitersPerLap = dto.peakLitersPerLap,
                bestValidLapTimeMs = dto.bestValidLapTimeMs,
                savedAtEpochMs = dto.savedAtEpochMs
            )
        } catch (e: Exception) {
            preference.remove(key)
            null
        }
    }

    override suspend fun clear(carModel: String, trackId: String) {
        val key = buildKey(carModel, trackId) ?: return
        preference.remove(key)
    }

    private fun buildKey(carModel: String, trackId: String): StringPrefKey? {
        val identity = FuelIdentityKey.from(
            carModel = carModel,
            trackId = trackId
        ) ?: return null
        return StringPrefKey("${KEY_PREFIX}${identity.carModel}_${identity.trackId}")
    }

    @Serializable
    private data class FuelDataDto(
        val peakLitersPerLap: Double?,
        val bestValidLapTimeMs: Int?,
        val savedAtEpochMs: Long
    )

    private companion object {

        const val KEY_PREFIX = "fuel_"
    }
}
