package com.project.analyzer.ac.telemetry.impl.internal.mapper.common

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.toBoolean
import com.project.analyzer.ac.telemetry.impl.shm.ac.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.DriverInfo
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import com.project.analyzer.utils.TelemetryIdentityFormatter
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcSessionCache {

    private val logger = logger()

    var trackInfo: TrackInfo? = null
        private set
    var carInfo: CarInfo? = null
        private set
    var driverInfo: DriverInfo? = null
        private set
    var sectorCount: Int = 3
        private set

    private var lastSessionIndex: Int = -1
    private var lastTrackId: String = ""
    private var lastCarModel: String = ""

    var tyreCompound: String = ""
        private set

    private val trackCache = CharArrayCache()
    private val layoutCache = CharArrayCache()
    private val carModelCache = CharArrayCache()
    private val tyreCompoundCache = CharArrayCache()

    private val listeners = mutableListOf<() -> Unit>()

    fun addSessionChangeListener(listener: () -> Unit) {
        listeners += listener
    }

    internal fun notifySessionChange() {
        listeners.forEach { it() }
    }

    fun updateIfNeeded(graphics: SPageFileGraphics, statics: SPageFileStatic): Boolean {
        val sessionIndex = graphics.sessionIndex
        val sessionChanged = sessionIndex != lastSessionIndex

        val rawTrack = trackCache.update(statics.track)
        val rawLayout = layoutCache.update(statics.trackConfiguration)
        val carModelNow = carModelCache.update(statics.carModel)
        val trackChanged = trackCache.changed || layoutCache.changed
        val normalizedTrackIdNow = if (trackChanged) {
            TrackIdNormalizer.normalize(rawTrack, rawLayout.takeIf { it.isNotBlank() })
        } else {
            lastTrackId
        }

        val sectorCountNow = statics.sectorCount.coerceIn(1, 10)

        val trackIdChanged =
            normalizedTrackIdNow.isNotBlank() && normalizedTrackIdNow != lastTrackId

        val carModelChanged =
            carModelNow.isNotBlank() && carModelNow != lastCarModel

        val sectorChanged = sectorCountNow != sectorCount

        val staticsIdentityChanged = trackIdChanged || carModelChanged || sectorChanged

        if (sessionChanged || staticsIdentityChanged) {
            lastSessionIndex = sessionIndex
            rebuildCache(statics, rawTrack, rawLayout, normalizedTrackIdNow, carModelNow)
            notifySessionChange()
        }

        val compound = tyreCompoundCache.update(graphics.tyreCompound)
        if (tyreCompoundCache.changed && compound != tyreCompound) {
            tyreCompound = compound
        }

        return sessionChanged || staticsIdentityChanged
    }

    private fun rebuildCache(
        statics: SPageFileStatic,
        rawTrack: String,
        rawLayout: String,
        normalizedTrackId: String,
        carModelNow: String,
    ) {
        lastTrackId = normalizedTrackId
        lastCarModel = carModelNow
        sectorCount = statics.sectorCount.coerceIn(1, 10)

        val maxRpm = statics.maxRpm.sanitizeOrNull(0, MAX_RPM)
        val maxTorque = statics.maxTorque.sanitizeOrNull(0f, MAX_TORQUE_NM)
        val maxPower = statics.maxPower.sanitizeOrNull(0f, MAX_POWER_W)
        val maxFuel = statics.maxFuel.sanitizeOrNull(0f, MAX_FUEL_LITERS)
        val maxTurboBoost = statics.maxTurboBoost.sanitizeOrNull(0f, MAX_TURBO_BOOST)
        val trackLength = statics.trackSPlineLength.sanitizeOrNull(0f, MAX_TRACK_LENGTH_M)

        if (statics.maxRpm != (maxRpm ?: 0) || statics.maxTorque != (maxTorque ?: 0f)) {
            logger.atWarn(RATE_LIMITED) {
                message = "statics sanity: maxRpm=${statics.maxRpm}→$maxRpm " +
                    "maxTorque=${statics.maxTorque}→$maxTorque " +
                    "maxPower=${statics.maxPower}→$maxPower " +
                    "maxFuel=${statics.maxFuel}→$maxFuel"
            }
        }

        trackInfo = TrackInfo(
            trackId = lastTrackId,
            trackName = TelemetryIdentityFormatter.formatTrackName(
                trackName = rawTrack,
                trackId = lastTrackId,
                layoutId = rawLayout,
            ),
            layoutId = TrackIdNormalizer.normalizeLayoutId(rawLayout),
            sectorCount = sectorCount,
            lengthMeters = trackLength,
        )

        carInfo = CarInfo(
            carModel = lastCarModel,
            carName = TelemetryIdentityFormatter.formatCarName(carModel = lastCarModel),
            carSkin = statics.carSkin.toKString().takeIf { it.isNotBlank() },
            maxTorqueNm = maxTorque,
            maxPowerW = maxPower,
            maxRpm = maxRpm,
            maxFuelLiters = maxFuel,
            maxTurboBoost = maxTurboBoost,
            tyreRadius = statics.tyreRadius.copyOf(),
            suspensionMaxTravel = statics.suspensionMaxTravel.copyOf(),
            dryTyresName = statics.dryTyresName.toKString().takeIf { it.isNotBlank() },
            wetTyresName = statics.wetTyresName.toKString().takeIf { it.isNotBlank() },
            hasDRS = statics.hasDRS.toBoolean(),
            hasERS = statics.hasERS.toBoolean(),
            hasKERS = statics.hasKERS.toBoolean(),
            engineBrakeSettingsCount = statics.engineBrakeSettingsCount,
        )

        driverInfo = DriverInfo(
            firstName = statics.playerName.toKString().takeIf { it.isNotBlank() },
            lastName = statics.playerSurname.toKString().takeIf { it.isNotBlank() },
            nickname = statics.playerNick.toKString().takeIf { it.isNotBlank() },
        )
    }

    private class CharArrayCache {

        var changed: Boolean = false
            private set
        private var raw: CharArray? = null
        private var value: String = ""

        fun update(source: CharArray): String {
            val current = raw
            if (current != null && source.contentEquals(current)) {
                changed = false
                return value
            }

            val newValue = source.toKString()
            raw = source.copyOf()
            value = newValue
            changed = true
            return newValue
        }
    }

    private companion object {

        const val MAX_RPM = 25_000
        const val MAX_TORQUE_NM = 5_000f
        const val MAX_POWER_W = 2_000_000f // 2MW ≈ 2700hp
        const val MAX_FUEL_LITERS = 500f
        const val MAX_TURBO_BOOST = 10f
        const val MAX_TRACK_LENGTH_M = 100_000f // 100km — longest circuits are ~25km

        fun Int.sanitizeOrNull(min: Int, max: Int): Int? = if (this in min..max) this else null

        fun Float.sanitizeOrNull(min: Float, max: Float): Float? =
            if (this.isFinite() && this in min..max) this else null
    }
}
