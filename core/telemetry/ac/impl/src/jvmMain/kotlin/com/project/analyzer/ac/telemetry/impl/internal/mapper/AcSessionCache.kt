package com.project.analyzer.ac.telemetry.impl.internal.mapper

import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileGraphics
import com.project.analyzer.ac.telemetry.impl.shm.structure.SPageFileStatic
import com.project.analyzer.ac.telemetry.impl.shm.structure.toBoolean
import com.project.analyzer.ac.telemetry.impl.shm.structure.toKString
import com.project.analyzer.telemetry.ac.api.model.session.CarInfo
import com.project.analyzer.telemetry.ac.api.model.session.DriverInfo
import com.project.analyzer.telemetry.ac.api.model.session.TrackInfo
import dev.zacsweers.metro.Inject

@Inject
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

    private val sessionChangeListeners = mutableListOf<() -> Unit>()

    fun addSessionChangeListener(listener: () -> Unit) {
        sessionChangeListeners.add(listener)
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
        lastTrackId = statics.track.toKString()
        lastCarModel = statics.carModel.toKString()
        sectorCount = statics.sectorCount.coerceIn(1, 10)

        trackInfo = TrackInfo(
            trackId = lastTrackId,
            trackName = lastTrackId,
            layoutId = statics.trackConfiguration.toKString().takeIf { it.isNotBlank() },
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

    private fun notifySessionChange() {
        sessionChangeListeners.forEach { it.invoke() }
    }
}
