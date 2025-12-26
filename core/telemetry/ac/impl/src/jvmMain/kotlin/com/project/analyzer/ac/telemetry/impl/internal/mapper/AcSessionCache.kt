package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.ac.api.model.session.CarInfo
import com.project.analyzer.telemetry.ac.api.model.session.DriverInfo
import com.project.analyzer.telemetry.ac.api.model.session.TrackInfo
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
        if (sessionChanged) {
            lastSessionIndex = sessionIndex
            rebuildCache(statics)
            notifySessionChange()
        }

        val compound = graphics.tyreCompound.toKString()
        if (compound != tyreCompound) {
            tyreCompound = compound
        }

        return sessionChanged
    }

    private fun rebuildCache(statics: SPageFileStatic) {
        val rawTrack = statics.track.toKString()
        val rawLayout = statics.trackConfiguration.toKString()
        lastTrackId = normalizeTrackId(rawTrack, rawLayout)
        lastCarModel = statics.carModel.toKString()
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

    private fun normalizeTrackId(track: String, layout: String?): String {
        val withLayout = if (!layout.isNullOrBlank()) "${track}_$layout" else track
        return withLayout
            .trim()
            .lowercase()
            .replace(Regex("""\s+"""), "_")
            .replace(Regex("""[^a-z0-9_]+"""), "")
            .replace(Regex("""_+"""), "_")
            .trim('_')
    }
}
