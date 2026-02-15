package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.DriverInfo
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(SessionScope::class)
class AcSessionCache {

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

        trackInfo = TrackInfo(
            trackId = lastTrackId,
            trackName = listOf(rawTrack, rawLayout)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { rawTrack },
            layoutId = rawLayout.takeIf { it.isNotBlank() },
            sectorCount = sectorCount,
            lengthMeters = statics.trackSPlineLength,
        )

        carInfo = CarInfo(
            carModel = lastCarModel,
            carSkin = statics.carSkin.toKString().takeIf { it.isNotBlank() },
            maxTorqueNm = statics.maxTorque,
            maxPowerW = statics.maxPower,
            maxRpm = statics.maxRpm,
            maxFuelLiters = statics.maxFuel,
            maxTurboBoost = statics.maxTurboBoost.takeIf { it > 0 },
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

}
