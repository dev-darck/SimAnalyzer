package com.project.analyzer.devsettings.presentation

import com.project.analyzer.math.Vec3
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.telemetry.api.model.car.AssistsFrame
import com.project.analyzer.telemetry.api.model.car.CarFrame
import com.project.analyzer.telemetry.api.model.car.ControlsFrame
import com.project.analyzer.telemetry.api.model.car.EngineFrame
import com.project.analyzer.telemetry.api.model.car.FuelFrame
import com.project.analyzer.telemetry.api.model.car.damage.DamageFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelFrame
import com.project.analyzer.telemetry.api.model.car.wheels.WheelsFrame
import com.project.analyzer.telemetry.api.model.environment.EnvironmentFrame
import com.project.analyzer.telemetry.api.model.lap.LapFrame
import com.project.analyzer.telemetry.api.model.lap.SectorFrame
import com.project.analyzer.telemetry.api.model.opponents.OpponentFrame
import com.project.analyzer.telemetry.api.model.session.CarInfo
import com.project.analyzer.telemetry.api.model.session.DriverInfo
import com.project.analyzer.telemetry.api.model.session.Flags
import com.project.analyzer.telemetry.api.model.session.Penalty
import com.project.analyzer.telemetry.api.model.session.PitState
import com.project.analyzer.telemetry.api.model.session.SessionFrame
import com.project.analyzer.telemetry.api.model.session.TrackInfo
import com.project.analyzer.telemetry.api.model.value.TelemetryValue
import java.util.Locale

internal object TelemetryInspectorMapper {

    fun map(frame: TelemetryFrame): Map<String, String> {
        val out = Entries()

        out.long("frame.frameId", frame.frameId)
        out.long("frame.timestampNs", frame.timestampNs)

        frame.session?.let { mapSession(it, "session", out) }
        frame.lap?.let { mapLap(it, "lap", out) }
        frame.car?.let { mapCar(it, "car", out) }
        frame.wheels?.let { mapWheels(it, "wheels", out) }
        frame.damage?.let { mapDamage(it, "damage", out) }
        frame.environment?.let { mapEnvironment(it, "env", out) }

        frame.opponents.forEachIndexed { i, opp ->
            mapOpponent(opp, "opp[$i]", out)
        }

        frame.extras.forEach { (key, value) ->
            out.str("extras.$key", fmtTelemetryValue(value))
        }

        return out.build()
    }

    private fun mapSession(s: SessionFrame, p: String, out: Entries) {
        out.enum("$p.status", s.status)
        out.enum("$p.sessionType", s.sessionType)
        out.enum("$p.phase", s.phase)

        s.track?.let { mapTrackInfo(it, "$p.track", out) }
        s.car?.let { mapCarInfo(it, "$p.car", out) }
        s.driver?.let { mapDriverInfo(it, "$p.driver", out) }

        out.float("$p.sessionTimeLeftSec", s.sessionTimeLeftSec)
        out.float("$p.sessionTimeElapsedSec", s.sessionTimeElapsedSec)
        out.int("$p.completedLaps", s.completedLaps)
        out.int("$p.plannedLaps", s.plannedLaps)
        out.int("$p.position", s.position)

        s.flags?.let { mapFlags(it, "$p.flags", out) }
        s.penalty?.let { mapPenalty(it, "$p.penalty", out) }
        s.pit?.let { mapPitState(it, "$p.pit", out) }

        out.int("$p.sessionIndex", s.sessionIndex)
        out.int("$p.numberOfSessions", s.numberOfSessions)
        out.bool("$p.isAiControlled", s.isAiControlled)
        out.bool("$p.isOnline", s.isOnline)
        out.bool("$p.isTimedRace", s.isTimedRace)
        out.bool("$p.hasExtraLap", s.hasExtraLap)
        out.int("$p.activeCars", s.activeCars)
        out.int("$p.numCars", s.numCars)
        out.int("$p.gapAheadMs", s.gapAheadMs)
        out.int("$p.gapBehindMs", s.gapBehindMs)
        out.bool("$p.isSetupMenuVisible", s.isSetupMenuVisible)
        out.bool("$p.isPaused", s.isPaused)
        out.int("$p.mainDisplayIndex", s.mainDisplayIndex)
        out.int("$p.secondaryDisplayIndex", s.secondaryDisplayIndex)
    }

    private fun mapTrackInfo(t: TrackInfo, p: String, out: Entries) {
        out.str("$p.trackId", t.trackId)
        out.str("$p.trackName", t.trackName)
        out.str("$p.layoutId", t.layoutId)
        out.int("$p.sectorCount", t.sectorCount)
        out.float("$p.lengthMeters", t.lengthMeters)
        out.float("$p.normalizedLapPosition", t.normalizedLapPosition)
        out.float("$p.distanceTraveled", t.distanceTraveled)
        out.floats("$p.worldXyz", t.worldXyz)
        out.str("$p.trackStatus", t.trackStatus)
    }

    private fun mapCarInfo(c: CarInfo, p: String, out: Entries) {
        out.str("$p.carModel", c.carModel)
        out.str("$p.carSkin", c.carSkin)
        out.int("$p.carId", c.carId)
        out.float("$p.maxTorqueNm", c.maxTorqueNm)
        out.float("$p.maxPowerW", c.maxPowerW)
        out.int("$p.maxRpm", c.maxRpm)
        out.float("$p.maxFuelLiters", c.maxFuelLiters)
        out.float("$p.maxTurboBoost", c.maxTurboBoost)
        out.floats("$p.tyreRadius", c.tyreRadius)
        out.floats("$p.suspensionMaxTravel", c.suspensionMaxTravel)
        out.str("$p.dryTyresName", c.dryTyresName)
        out.str("$p.wetTyresName", c.wetTyresName)
        out.bool("$p.hasDRS", c.hasDRS)
        out.bool("$p.hasERS", c.hasERS)
        out.bool("$p.hasKERS", c.hasKERS)
        out.int("$p.engineBrakeSettingsCount", c.engineBrakeSettingsCount)
        out.float("$p.ballast", c.ballast)
    }

    private fun mapDriverInfo(d: DriverInfo, p: String, out: Entries) {
        out.str("$p.firstName", d.firstName)
        out.str("$p.lastName", d.lastName)
        out.str("$p.nickname", d.nickname)
        out.int("$p.stintTotalTimeLeftMs", d.stintTotalTimeLeftMs)
        out.int("$p.stintTimeLeftMs", d.stintTimeLeftMs)
    }

    private fun mapFlags(f: Flags, p: String, out: Entries) {
        out.enum("$p.flag", f.flag)
        out.bool("$p.globalYellow", f.globalYellow)
        out.bool("$p.globalYellowS1", f.globalYellowSector1)
        out.bool("$p.globalYellowS2", f.globalYellowSector2)
        out.bool("$p.globalYellowS3", f.globalYellowSector3)
        out.bool("$p.globalWhite", f.globalWhite)
        out.bool("$p.globalGreen", f.globalGreen)
        out.bool("$p.globalChequered", f.globalChequered)
        out.bool("$p.globalRed", f.globalRed)
    }

    private fun mapPenalty(pen: Penalty, p: String, out: Entries) {
        out.enum("$p.type", pen.type)
        out.float("$p.timeSec", pen.penaltyTimeSec)
    }

    private fun mapPitState(pit: PitState, p: String, out: Entries) {
        out.bool("$p.isInPit", pit.isInPit)
        out.bool("$p.isInPitLane", pit.isInPitLane)
        out.bool("$p.pitLimiterOn", pit.pitLimiterOn)
        out.bool("$p.mandatoryPitDone", pit.mandatoryPitDone)
        out.int("$p.missingMandatoryPits", pit.missingMandatoryPits)
        out.int("$p.pitWindowStart", pit.pitWindowStart)
        out.int("$p.pitWindowEnd", pit.pitWindowEnd)
    }

    private fun mapLap(l: LapFrame, p: String, out: Entries) {
        out.int("$p.currentLapIndex", l.currentLapIndex)
        out.int("$p.completedLaps", l.completedLaps)

        out.int("$p.currentLapTimeMs", l.currentLapTimeMs)
        out.int("$p.lastLapTimeMs", l.lastLapTimeMs)
        out.int("$p.bestLapTimeMs", l.bestLapTimeMs)

        out.int("$p.sectorCount", l.sectorCount)
        out.int("$p.currentSectorIndex", l.currentSectorIndex)
        out.int("$p.lastSectorTimeMs", l.lastSectorTimeMs)

        out.int("$p.deltaLapTimeMs", l.deltaLapTimeMs)
        out.bool("$p.isDeltaPositive", l.isDeltaPositive)
        out.int("$p.estimatedLapTimeMs", l.estimatedLapTimeMs)
        out.int("$p.splitTimeMs", l.splitTimeMs)

        out.enum("$p.validity", l.validity)

        l.sectors.forEachIndexed { i, sector ->
            mapSector(sector, "$p.sectors[$i]", out)
        }
    }

    private fun mapSector(s: SectorFrame, p: String, out: Entries) {
        out.int("$p.index", s.index)
        out.int("$p.timeMs", s.timeMs)
        out.int("$p.bestTimeMs", s.bestTimeMs)
        out.int("$p.deltaToBestMs", s.deltaToBestMs)
        out.enum("$p.status", s.status)
        out.enum("$p.validity", s.validity)
        out.enum("$p.invalidReason", s.invalidReason)
    }

    private fun mapCar(c: CarFrame, p: String, out: Entries) {
        c.controls?.let { mapControls(it, "$p.ctl", out) }
        c.engine?.let { mapEngine(it, "$p.eng", out) }
        c.fuel?.let { mapFuel(it, "$p.fuel", out) }
        c.assists?.let { mapAssists(it, "$p.ast", out) }

        out.float("$p.speedKmh", c.speedKmh)
        out.vec3("$p.velocity", c.velocity)
        out.vec3("$p.localVelocity", c.localVelocity)
        out.vec3("$p.accelerationG", c.accelerationG)
        out.vec3("$p.worldPosition", c.worldPosition)

        out.float("$p.heading", c.heading)
        out.float("$p.pitch", c.pitch)
        out.float("$p.roll", c.roll)

        out.vec3("$p.localAngularVelocity", c.localAngularVelocity)

        out.float("$p.cgHeight", c.cgHeight)
        out.float("$p.rideHeightFront", c.rideHeightFront)
        out.float("$p.rideHeightRear", c.rideHeightRear)

        out.float("$p.finalFF", c.finalFF)

        out.float("$p.kerbVibration", c.kerbVibration)
        out.float("$p.slipVibrations", c.slipVibrations)
        out.float("$p.gVibrations", c.gVibrations)
        out.float("$p.absVibrations", c.absVibrations)

        out.int("$p.lightsStage", c.lightsStage)
        out.bool("$p.rainLightsOn", c.rainLightsOn)
        out.bool("$p.flashingLightsOn", c.flashingLightsOn)
        out.bool("$p.directionLightsLeft", c.directionLightsLeft)
        out.bool("$p.directionLightsRight", c.directionLightsRight)
    }

    private fun mapControls(c: ControlsFrame, p: String, out: Entries) {
        out.float("$p.throttle", c.throttle)
        out.float("$p.brake", c.brake)
        out.float("$p.clutch", c.clutch)
        out.float("$p.steerAngle", c.steerAngle)
        out.float("$p.brakeBias", c.brakeBias)
        out.float("$p.brakePressureFL", c.brakePressureFL)
        out.float("$p.brakePressureFR", c.brakePressureFR)
        out.float("$p.brakePressureRL", c.brakePressureRL)
        out.float("$p.brakePressureRR", c.brakePressureRR)
    }

    private fun mapEngine(e: EngineFrame, p: String, out: Entries) {
        out.int("$p.gear", e.gear)
        out.int("$p.rpm", e.rpm)
        out.int("$p.maxRpm", e.maxRpm)
        out.float("$p.currentMaxRpm", e.currentMaxRpm)
        out.float("$p.turboBoost", e.turboBoost)
        out.float("$p.kersCharge", e.kersCharge)
        out.float("$p.kersInput", e.kersInput)
        out.float("$p.kersCurrentKJ", e.kersCurrentKJ)
        out.bool("$p.ignitionOn", e.ignitionOn)
        out.bool("$p.starterEngineOn", e.starterEngineOn)
        out.bool("$p.isEngineRunning", e.isEngineRunning)
        out.float("$p.waterTempC", e.waterTempC)
        out.float("$p.exhaustTempC", e.exhaustTempC)
        out.int("$p.engineBrake", e.engineBrake)
        out.bool("$p.autoShifterOn", e.autoShifterOn)
    }

    private fun mapFuel(f: FuelFrame, p: String, out: Entries) {
        out.float("$p.fuelLiters", f.fuelLiters)
        out.float("$p.maxFuelLiters", f.maxFuelLiters)
        out.float("$p.fuelPerLapLiters", f.fuelPerLapLiters)
        out.float("$p.fuelUsedLiters", f.fuelUsedLiters)
        out.float("$p.fuelEstimatedLaps", f.fuelEstimatedLaps)
        out.float("$p.mfdFuelToAdd", f.mfdFuelToAdd)
    }

    private fun mapAssists(a: AssistsFrame, p: String, out: Entries) {
        out.int("$p.tcLevel", a.tcLevel)
        out.int("$p.tcCut", a.tcCut)
        out.float("$p.tcValue", a.tcValue)
        out.bool("$p.tcInAction", a.tcInAction)
        out.int("$p.absLevel", a.absLevel)
        out.float("$p.absValue", a.absValue)
        out.bool("$p.absInAction", a.absInAction)
        out.int("$p.engineMap", a.engineMap)
        out.bool("$p.drsAvailable", a.drsAvailable)
        out.bool("$p.drsEnabled", a.drsEnabled)
        out.bool("$p.pitLimiterOn", a.pitLimiterOn)
        out.bool("$p.idealLineOn", a.idealLineOn)
        out.int("$p.wiperLevel", a.wiperLevel)
    }

    private fun mapWheels(w: WheelsFrame, p: String, out: Entries) {
        w.fl?.let { mapWheel(it, "$p.fl", out) }
        w.fr?.let { mapWheel(it, "$p.fr", out) }
        w.rl?.let { mapWheel(it, "$p.rl", out) }
        w.rr?.let { mapWheel(it, "$p.rr", out) }

        out.str("$p.tyreCompound", w.tyreCompound)
        out.int("$p.currentTyreSet", w.currentTyreSet)
        out.int("$p.strategyTyreSet", w.strategyTyreSet)
        out.bool("$p.isRainTyres", w.isRainTyres)

        out.float("$p.mfdPressureLF", w.mfdPressureLF)
        out.float("$p.mfdPressureRF", w.mfdPressureRF)
        out.float("$p.mfdPressureLR", w.mfdPressureLR)
        out.float("$p.mfdPressureRR", w.mfdPressureRR)
        out.int("$p.mfdTyreSet", w.mfdTyreSet)

        out.int("$p.frontBrakeCompound", w.frontBrakeCompound)
        out.int("$p.rearBrakeCompound", w.rearBrakeCompound)
    }

    private fun mapWheel(w: WheelFrame, p: String, out: Entries) {
        out.float("$p.pressurePsi", w.pressurePsi)
        out.float("$p.wear", w.wear)
        out.float("$p.dirtyLevel", w.dirtyLevel)

        out.float("$p.coreTempC", w.coreTempC)
        out.float("$p.innerTempC", w.innerTempC)
        out.float("$p.middleTempC", w.middleTempC)
        out.float("$p.outerTempC", w.outerTempC)
        out.float("$p.avgTempC", w.avgTempC)

        out.float("$p.brakeTempC", w.brakeTempC)
        out.float("$p.brakePressure", w.brakePressure)
        out.float("$p.padLife", w.padLife)
        out.float("$p.discLife", w.discLife)

        out.float("$p.slip", w.slip)
        out.float("$p.load", w.load)
        out.float("$p.angularSpeed", w.angularSpeed)

        out.float("$p.longitudinalForce", w.longitudinalForce)
        out.float("$p.lateralForce", w.lateralForce)
        out.float("$p.selfAligningTorque", w.selfAligningTorque)

        out.float("$p.suspensionTravel", w.suspensionTravel)
        out.float("$p.camberRad", w.camberRad)

        out.vec3("$p.contactPoint", w.contactPoint)
        out.vec3("$p.contactNormal", w.contactNormal)
        out.vec3("$p.contactHeading", w.contactHeading)

        out.float("$p.tyreRadius", w.tyreRadius)
    }

    private fun mapDamage(d: DamageFrame, p: String, out: Entries) {
        out.floats("$p.rawDamage5", d.rawDamage5)

        d.aero?.let { a ->
            out.float("$p.aero.frontWing", a.frontWing)
            out.float("$p.aero.rearWing", a.rearWing)
            out.float("$p.aero.diffuser", a.diffuser)
        }

        d.suspension?.let { s ->
            out.float("$p.susp.fl", s.fl)
            out.float("$p.susp.fr", s.fr)
            out.float("$p.susp.rl", s.rl)
            out.float("$p.susp.rr", s.rr)
        }

        d.body?.let { b ->
            out.float("$p.body.left", b.left)
            out.float("$p.body.right", b.right)
            out.float("$p.body.centre", b.centre)
        }

        d.tyres?.let { t ->
            out.float("$p.tyres.fl", t.fl)
            out.float("$p.tyres.fr", t.fr)
            out.float("$p.tyres.rl", t.rl)
            out.float("$p.tyres.rr", t.rr)
            out.bool("$p.tyres.punctureFl", t.punctureFl)
            out.bool("$p.tyres.punctureFr", t.punctureFr)
            out.bool("$p.tyres.punctureRl", t.punctureRl)
            out.bool("$p.tyres.punctureRr", t.punctureRr)
        }

        out.int("$p.numberOfTyresOut", d.numberOfTyresOut)
    }

    private fun mapEnvironment(e: EnvironmentFrame, p: String, out: Entries) {
        out.float("$p.airTempC", e.airTempC)
        out.float("$p.roadTempC", e.roadTempC)
        out.float("$p.airDensity", e.airDensity)
        out.float("$p.windSpeedMps", e.windSpeedMps)
        out.float("$p.windDirectionDeg", e.windDirectionDeg)
        out.float("$p.rainIntensity", e.rainIntensity)
        out.int("$p.rainIntensityIn10min", e.rainIntensityIn10min)
        out.int("$p.rainIntensityIn30min", e.rainIntensityIn30min)
        out.float("$p.surfaceGrip", e.surfaceGrip)
        out.int("$p.trackGripStatus", e.trackGripStatus)
        out.float("$p.clockSeconds", e.clockSeconds)
    }

    private fun mapOpponent(o: OpponentFrame, p: String, out: Entries) {
        out.int("$p.carIndex", o.carIndex)
        out.str("$p.carModelId", o.carModelId)
        out.str("$p.driverName", o.driverName)
        out.int("$p.position", o.position)
        out.int("$p.gapToPlayerMs", o.gapToPlayerMs)
        out.int("$p.lastLapTimeMs", o.lastLapTimeMs)
        out.int("$p.bestLapTimeMs", o.bestLapTimeMs)
        out.bool("$p.isInPit", o.isInPit)
        out.vec3("$p.worldPosition", o.worldPosition)
        out.float("$p.normalizedLapPosition", o.normalizedLapPosition)
    }

    private fun fmtFloat(v: Float): String = String.format(Locale.US, "%.3f", v)

    private fun fmtVec3(v: Vec3): String = "[${fmtFloat(v.x)}, ${fmtFloat(v.y)}, ${fmtFloat(v.z)}]"

    private fun fmtFloats(arr: FloatArray): String = arr.joinToString(", ", "[", "]") { fmtFloat(it) }

    private fun fmtTelemetryValue(value: TelemetryValue): String = when (value) {
        is TelemetryValue.BoolVal -> value.value.toString()
        is TelemetryValue.IntVal -> value.value.toString()
        is TelemetryValue.LongVal -> value.value.toString()
        is TelemetryValue.FloatVal -> fmtFloat(value.value)
        is TelemetryValue.DoubleVal -> String.format(Locale.US, "%.3f", value.value)
        is TelemetryValue.StringVal -> value.value
        is TelemetryValue.Vec2Val -> "[${fmtFloat(value.value.x)}, ${fmtFloat(value.value.y)}]"
        is TelemetryValue.Vec3Val -> fmtVec3(value.value)
        is TelemetryValue.FloatArrayVal -> fmtFloats(value.value)
    }

    @Suppress("TooManyFunctions")
    private class Entries {

        private val map = LinkedHashMap<String, String>(INITIAL_CAPACITY)

        fun build(): Map<String, String> = map

        fun str(key: String, value: String?) {
            map[key] = value ?: ABSENT
        }

        fun bool(key: String, value: Boolean?) {
            map[key] = value?.toString() ?: ABSENT
        }

        fun int(key: String, value: Int?) {
            map[key] = value?.toString() ?: ABSENT
        }

        fun long(key: String, value: Long?) {
            map[key] = value?.toString() ?: ABSENT
        }

        fun float(key: String, value: Float?) {
            map[key] = if (value != null) fmtFloat(value) else ABSENT
        }

        fun vec3(key: String, value: Vec3?) {
            map[key] = if (value != null) fmtVec3(value) else ABSENT
        }

        fun floats(key: String, value: FloatArray?) {
            map[key] = if (value != null) fmtFloats(value) else ABSENT
        }

        fun enum(key: String, value: Enum<*>?) {
            map[key] = value?.name ?: ABSENT
        }

        private companion object {

            const val ABSENT = "-"
            const val INITIAL_CAPACITY = 256
        }
    }
}
