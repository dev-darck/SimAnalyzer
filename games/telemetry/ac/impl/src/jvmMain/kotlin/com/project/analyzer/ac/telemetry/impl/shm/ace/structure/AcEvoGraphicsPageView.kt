package com.project.analyzer.ac.telemetry.impl.shm.ace.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "packetId",
    "statusRaw",
    "focusedCarIdA",
    "focusedCarIdB",
    "playerCarIdA",
    "playerCarIdB",
    "rpmRaw",
    "isRpmLimiterOnRaw",
    "isChangeUpRpmRaw",
    "isChangeDownRpmRaw",
    "tcActiveRaw",
    "absActiveRaw",
    "escActiveRaw",
    "launchActiveRaw",
    "isIgnitionOnRaw",
    "isEngineRunningRaw",
    "kersIsChargingRaw",
    "isWrongWayRaw",
    "isDrsAvailableRaw",
    "batteryIsChargingRaw",
    "isMaxKjPerLapReachedRaw",
    "isMaxChargeKjPerLapReachedRaw",
    "padding0",
    "displaySpeedKmhRaw",
    "displaySpeedMphRaw",
    "displaySpeedMsRaw",
    "pitspeedingDeltaRaw",
    "gearIntRaw",
    "padding1",
    "rpmPercentRaw",
    "gasPercentRaw",
    "brakePercentRaw",
    "handbrakePercentRaw",
    "clutchPercentRaw",
    "steeringPercentRaw",
    "ffbStrengthRaw",
    "carFfbMultiplierRaw",
    "waterTemperaturePercentRaw",
    "waterPressureBarRaw",
    "fuelPressureBarRaw",
    "waterTemperatureCRaw",
    "airTemperatureCRaw",
    "padding2",
    "oilTemperatureCRaw",
    "oilPressureBarRaw",
    "exhaustTemperatureCRaw",
    "gForcesXRaw",
    "gForcesYRaw",
    "gForcesZRaw",
    "turboBoostRaw",
    "turboBoostLevelRaw",
    "turboBoostPercRaw",
    "steerDegreesRaw",
    "currentKmRaw",
    "totalKmRaw",
    "totalDrivingTimeSRaw",
    "timeOfDayHoursRaw",
    "timeOfDayMinutesRaw",
    "timeOfDaySecondsRaw",
    "deltaTimeMsRaw",
    "currentLapTimeMsRaw",
    "predictedLapTimeMsRaw",
    "fuelLiterCurrentQuantityRaw",
    "fuelLiterCurrentQuantityPercentRaw",
    "fuelLiterPerKmRaw",
    "kmPerFuelLiterRaw",
    "currentTorqueRaw",
    "currentBhpRaw",
    "tyreLf",
    "tyreRf",
    "tyreLr",
    "tyreRr",
    "nposRaw",
    "kersChargePercRaw",
    "kersCurrentPercRaw",
    "controlLockTimeRaw",
    "carDamage",
    "carLocationRaw",
    "pitInfo",
    "fuelLiterUsedRaw",
    "fuelLiterPerLapRaw",
    "lapsPossibleWithFuelRaw",
    "batteryTemperatureRaw",
    "batteryVoltageRaw",
    "instantaneousFuelLiterPerKmRaw",
    "instantaneousKmPerFuelLiterRaw",
    "gearRpmWindowRaw",
    "instrumentation",
    "instrumentationMinLimit",
    "instrumentationMaxLimit",
    "electronics",
    "electronicsMinLimit",
    "electronicsMaxLimit",
    "electronicsIsModifiable",
    "totalLapCountRaw",
    "currentPosRaw",
    "totalDriversRaw",
    "lastLaptimeMsRaw",
    "bestLaptimeMsRaw",
    "flagRaw",
    "globalFlagRaw",
    "maxGearsRaw",
    "engineTypeRaw",
    "hasKersRaw",
    "isLastLapRaw",
    "performanceModeNameRaw",
    "padding4",
    "diffCoastRawValueRaw",
    "diffPowerRawValueRaw",
    "raceCutGainedTimeMsRaw",
    "distanceToDeadlineRaw",
    "raceCutCurrentDeltaRaw",
    "sessionState",
    "timingState",
    "playerPingRaw",
    "playerLatencyRaw",
    "playerCpuUsageRaw",
    "playerCpuUsageAvgRaw",
    "playerQosRaw",
    "playerQosAvgRaw",
    "playerFpsRaw",
    "playerFpsAvgRaw",
    "driverNameRaw",
    "driverSurnameRaw",
    "carModelRaw",
    "isInPitBoxRaw",
    "isInPitLaneRaw",
    "isValidLapRaw",
    "padding5",
    "carCoordinatesRaw",
    "gapAheadRaw",
    "gapBehindRaw",
    "activeCarsRaw",
    "padding6",
    "fuelPerLapRaw",
    "fuelEstimatedLapsRaw",
    "assistsState",
    "maxFuelRaw",
    "maxTurboBoostRaw",
    "useSingleCompoundRaw",
    "padding7",
)
public class AcEvoGraphicsPageView : Pack4Structure(Pointer.NULL) {

    @JvmField
    var packetId: Int = 0

    @JvmField
    var statusRaw: Int = 0

    @JvmField
    var focusedCarIdA: Long = 0L

    @JvmField
    var focusedCarIdB: Long = 0L

    @JvmField
    var playerCarIdA: Long = 0L

    @JvmField
    var playerCarIdB: Long = 0L

    @JvmField
    var rpmRaw: Short = 0

    @JvmField
    var isRpmLimiterOnRaw: Byte = 0

    @JvmField
    var isChangeUpRpmRaw: Byte = 0

    @JvmField
    var isChangeDownRpmRaw: Byte = 0

    @JvmField
    var tcActiveRaw: Byte = 0

    @JvmField
    var absActiveRaw: Byte = 0

    @JvmField
    var escActiveRaw: Byte = 0

    @JvmField
    var launchActiveRaw: Byte = 0

    @JvmField
    var isIgnitionOnRaw: Byte = 0

    @JvmField
    var isEngineRunningRaw: Byte = 0

    @JvmField
    var kersIsChargingRaw: Byte = 0

    @JvmField
    var isWrongWayRaw: Byte = 0

    @JvmField
    var isDrsAvailableRaw: Byte = 0

    @JvmField
    var batteryIsChargingRaw: Byte = 0

    @JvmField
    var isMaxKjPerLapReachedRaw: Byte = 0

    @JvmField
    var isMaxChargeKjPerLapReachedRaw: Byte = 0

    @JvmField
    var padding0: Byte = 0

    @JvmField
    var displaySpeedKmhRaw: Short = 0

    @JvmField
    var displaySpeedMphRaw: Short = 0

    @JvmField
    var displaySpeedMsRaw: Short = 0

    @JvmField
    var pitspeedingDeltaRaw: Float = 0f

    @JvmField
    var gearIntRaw: Short = 0

    @JvmField
    var padding1: ByteArray = ByteArray(2)

    @JvmField
    var rpmPercentRaw: Float = 0f

    @JvmField
    var gasPercentRaw: Float = 0f

    @JvmField
    var brakePercentRaw: Float = 0f

    @JvmField
    var handbrakePercentRaw: Float = 0f

    @JvmField
    var clutchPercentRaw: Float = 0f

    @JvmField
    var steeringPercentRaw: Float = 0f

    @JvmField
    var ffbStrengthRaw: Float = 0f

    @JvmField
    var carFfbMultiplierRaw: Float = 0f

    @JvmField
    var waterTemperaturePercentRaw: Float = 0f

    @JvmField
    var waterPressureBarRaw: Float = 0f

    @JvmField
    var fuelPressureBarRaw: Float = 0f

    @JvmField
    var waterTemperatureCRaw: Byte = 0

    @JvmField
    var airTemperatureCRaw: Byte = 0

    @JvmField
    var padding2: ByteArray = ByteArray(2)

    @JvmField
    var oilTemperatureCRaw: Float = 0f

    @JvmField
    var oilPressureBarRaw: Float = 0f

    @JvmField
    var exhaustTemperatureCRaw: Float = 0f

    @JvmField
    var gForcesXRaw: Float = 0f

    @JvmField
    var gForcesYRaw: Float = 0f

    @JvmField
    var gForcesZRaw: Float = 0f

    @JvmField
    var turboBoostRaw: Float = 0f

    @JvmField
    var turboBoostLevelRaw: Float = 0f

    @JvmField
    var turboBoostPercRaw: Float = 0f

    @JvmField
    var steerDegreesRaw: Int = 0

    @JvmField
    var currentKmRaw: Float = 0f

    @JvmField
    var totalKmRaw: Int = 0

    @JvmField
    var totalDrivingTimeSRaw: Int = 0

    @JvmField
    var timeOfDayHoursRaw: Int = 0

    @JvmField
    var timeOfDayMinutesRaw: Int = 0

    @JvmField
    var timeOfDaySecondsRaw: Int = 0

    @JvmField
    var deltaTimeMsRaw: Int = 0

    @JvmField
    var currentLapTimeMsRaw: Int = 0

    @JvmField
    var predictedLapTimeMsRaw: Int = 0

    @JvmField
    var fuelLiterCurrentQuantityRaw: Float = 0f

    @JvmField
    var fuelLiterCurrentQuantityPercentRaw: Float = 0f

    @JvmField
    var fuelLiterPerKmRaw: Float = 0f

    @JvmField
    var kmPerFuelLiterRaw: Float = 0f

    @JvmField
    var currentTorqueRaw: Float = 0f

    @JvmField
    var currentBhpRaw: Int = 0

    @JvmField
    var tyreLf: AcEvoTyreStateView = AcEvoTyreStateView()

    @JvmField
    var tyreRf: AcEvoTyreStateView = AcEvoTyreStateView()

    @JvmField
    var tyreLr: AcEvoTyreStateView = AcEvoTyreStateView()

    @JvmField
    var tyreRr: AcEvoTyreStateView = AcEvoTyreStateView()

    @JvmField
    var nposRaw: Float = 0f

    @JvmField
    var kersChargePercRaw: Float = 0f

    @JvmField
    var kersCurrentPercRaw: Float = 0f

    @JvmField
    var controlLockTimeRaw: Float = 0f

    @JvmField
    var carDamage: AcEvoDamageStateView = AcEvoDamageStateView()

    @JvmField
    var carLocationRaw: Int = 0

    @JvmField
    var pitInfo: AcEvoPitInfoView = AcEvoPitInfoView()

    @JvmField
    var fuelLiterUsedRaw: Float = 0f

    @JvmField
    var fuelLiterPerLapRaw: Float = 0f

    @JvmField
    var lapsPossibleWithFuelRaw: Float = 0f

    @JvmField
    var batteryTemperatureRaw: Float = 0f

    @JvmField
    var batteryVoltageRaw: Float = 0f

    @JvmField
    var instantaneousFuelLiterPerKmRaw: Float = 0f

    @JvmField
    var instantaneousKmPerFuelLiterRaw: Float = 0f

    @JvmField
    var gearRpmWindowRaw: Float = 0f

    @JvmField
    var instrumentation: AcEvoInstrumentationView = AcEvoInstrumentationView()

    @JvmField
    var instrumentationMinLimit: AcEvoInstrumentationView = AcEvoInstrumentationView()

    @JvmField
    var instrumentationMaxLimit: AcEvoInstrumentationView = AcEvoInstrumentationView()

    @JvmField
    var electronics: AcEvoElectronicsView = AcEvoElectronicsView()

    @JvmField
    var electronicsMinLimit: AcEvoElectronicsView = AcEvoElectronicsView()

    @JvmField
    var electronicsMaxLimit: AcEvoElectronicsView = AcEvoElectronicsView()

    @JvmField
    var electronicsIsModifiable: AcEvoElectronicsView = AcEvoElectronicsView()

    @JvmField
    var totalLapCountRaw: Int = 0

    @JvmField
    var currentPosRaw: Int = 0

    @JvmField
    var totalDriversRaw: Int = 0

    @JvmField
    var lastLaptimeMsRaw: Int = 0

    @JvmField
    var bestLaptimeMsRaw: Int = 0

    @JvmField
    var flagRaw: Int = 0

    @JvmField
    var globalFlagRaw: Int = 0

    @JvmField
    var maxGearsRaw: Int = 0

    @JvmField
    var engineTypeRaw: Int = 0

    @JvmField
    var hasKersRaw: Byte = 0

    @JvmField
    var isLastLapRaw: Byte = 0

    @JvmField
    var performanceModeNameRaw: ByteArray = ByteArray(33)

    @JvmField
    var padding4: Byte = 0

    @JvmField
    var diffCoastRawValueRaw: Float = 0f

    @JvmField
    var diffPowerRawValueRaw: Float = 0f

    @JvmField
    var raceCutGainedTimeMsRaw: Int = 0

    @JvmField
    var distanceToDeadlineRaw: Int = 0

    @JvmField
    var raceCutCurrentDeltaRaw: Float = 0f

    @JvmField
    var sessionState: AcEvoSessionStateView = AcEvoSessionStateView()

    @JvmField
    var timingState: AcEvoTimingStateView = AcEvoTimingStateView()

    @JvmField
    var playerPingRaw: Int = 0

    @JvmField
    var playerLatencyRaw: Int = 0

    @JvmField
    var playerCpuUsageRaw: Int = 0

    @JvmField
    var playerCpuUsageAvgRaw: Int = 0

    @JvmField
    var playerQosRaw: Int = 0

    @JvmField
    var playerQosAvgRaw: Int = 0

    @JvmField
    var playerFpsRaw: Int = 0

    @JvmField
    var playerFpsAvgRaw: Int = 0

    @JvmField
    var driverNameRaw: ByteArray = ByteArray(33)

    @JvmField
    var driverSurnameRaw: ByteArray = ByteArray(33)

    @JvmField
    var carModelRaw: ByteArray = ByteArray(33)

    @JvmField
    var isInPitBoxRaw: Byte = 0

    @JvmField
    var isInPitLaneRaw: Byte = 0

    @JvmField
    var isValidLapRaw: Byte = 0

    @JvmField
    var padding5: ByteArray = ByteArray(2)

    @JvmField
    var carCoordinatesRaw: FloatArray = FloatArray(180)

    @JvmField
    var gapAheadRaw: Float = 0f

    @JvmField
    var gapBehindRaw: Float = 0f

    @JvmField
    var activeCarsRaw: Byte = 0

    @JvmField
    var padding6: ByteArray = ByteArray(3)

    @JvmField
    var fuelPerLapRaw: Float = 0f

    @JvmField
    var fuelEstimatedLapsRaw: Float = 0f

    @JvmField
    var assistsState: AcEvoAssistsStateView = AcEvoAssistsStateView()

    @JvmField
    var maxFuelRaw: Float = 0f

    @JvmField
    var maxTurboBoostRaw: Float = 0f

    @JvmField
    var useSingleCompoundRaw: Byte = 0

    @JvmField
    var padding7: ByteArray = ByteArray(159)

    val status: AcEvoStatus? get() = AcEvoStatus.fromRaw(statusRaw)
    val rpm: Int get() = rpmRaw.toAceUInt16()
    val isRpmLimiterOn: Boolean get() = isRpmLimiterOnRaw.toAceBool()
    val isChangeUpRpm: Boolean get() = isChangeUpRpmRaw.toAceBool()
    val isChangeDownRpm: Boolean get() = isChangeDownRpmRaw.toAceBool()
    val tcActive: Boolean get() = tcActiveRaw.toAceBool()
    val absActive: Boolean get() = absActiveRaw.toAceBool()
    val escActive: Boolean get() = escActiveRaw.toAceBool()
    val launchActive: Boolean get() = launchActiveRaw.toAceBool()
    val isIgnitionOn: Boolean get() = isIgnitionOnRaw.toAceBool()
    val isEngineRunning: Boolean get() = isEngineRunningRaw.toAceBool()
    val kersIsCharging: Boolean get() = kersIsChargingRaw.toAceBool()
    val isWrongWay: Boolean get() = isWrongWayRaw.toAceBool()
    val isDrsAvailable: Boolean get() = isDrsAvailableRaw.toAceBool()
    val batteryIsCharging: Boolean get() = batteryIsChargingRaw.toAceBool()
    val isMaxKjPerLapReached: Boolean get() = isMaxKjPerLapReachedRaw.toAceBool()
    val isMaxChargeKjPerLapReached: Boolean get() = isMaxChargeKjPerLapReachedRaw.toAceBool()
    val displaySpeedKmh: Int get() = displaySpeedKmhRaw.toInt()
    val displaySpeedMph: Int get() = displaySpeedMphRaw.toInt()
    val displaySpeedMs: Int get() = displaySpeedMsRaw.toInt()
    val pitspeedingDelta: Float get() = pitspeedingDeltaRaw
    val gearInt: Int get() = gearIntRaw.toInt()
    val rpmPercent: Float get() = rpmPercentRaw
    val gasPercent: Float get() = gasPercentRaw
    val brakePercent: Float get() = brakePercentRaw
    val handbrakePercent: Float get() = handbrakePercentRaw
    val clutchPercent: Float get() = clutchPercentRaw
    val steeringPercent: Float get() = steeringPercentRaw
    val ffbStrength: Float get() = ffbStrengthRaw
    val carFfbMultiplier: Float get() = carFfbMultiplierRaw
    val waterTemperaturePercent: Float get() = waterTemperaturePercentRaw
    val waterPressureBar: Float get() = waterPressureBarRaw
    val fuelPressureBar: Float get() = fuelPressureBarRaw
    val waterTemperatureC: Int get() = waterTemperatureCRaw.toAceInt8()
    val airTemperatureC: Int get() = airTemperatureCRaw.toAceInt8()
    val oilTemperatureC: Float get() = oilTemperatureCRaw
    val oilPressureBar: Float get() = oilPressureBarRaw
    val exhaustTemperatureC: Float get() = exhaustTemperatureCRaw
    val gForcesX: Float get() = gForcesXRaw
    val gForcesY: Float get() = gForcesYRaw
    val gForcesZ: Float get() = gForcesZRaw
    val turboBoost: Float get() = turboBoostRaw
    val turboBoostLevel: Float get() = turboBoostLevelRaw
    val turboBoostPerc: Float get() = turboBoostPercRaw
    val steerDegrees: Int get() = steerDegreesRaw
    val currentKm: Float get() = currentKmRaw
    val totalKm: Long get() = totalKmRaw.toAceUInt32()
    val totalDrivingTimeS: Long get() = totalDrivingTimeSRaw.toAceUInt32()
    val timeOfDayHours: Int get() = timeOfDayHoursRaw
    val timeOfDayMinutes: Int get() = timeOfDayMinutesRaw
    val timeOfDaySeconds: Int get() = timeOfDaySecondsRaw
    val deltaTimeMs: Int get() = deltaTimeMsRaw
    val currentLapTimeMs: Int get() = currentLapTimeMsRaw
    val predictedLapTimeMs: Int get() = predictedLapTimeMsRaw
    val fuelLiterCurrentQuantity: Float get() = fuelLiterCurrentQuantityRaw
    val fuelLiterCurrentQuantityPercent: Float get() = fuelLiterCurrentQuantityPercentRaw
    val fuelLiterPerKm: Float get() = fuelLiterPerKmRaw
    val kmPerFuelLiter: Float get() = kmPerFuelLiterRaw
    val currentTorque: Float get() = currentTorqueRaw
    val currentBhp: Int get() = currentBhpRaw
    val npos: Float get() = nposRaw
    val kersChargePerc: Float get() = kersChargePercRaw
    val kersCurrentPerc: Float get() = kersCurrentPercRaw
    val controlLockTime: Float get() = controlLockTimeRaw
    val carLocation: AcEvoCarLocation? get() = AcEvoCarLocation.fromRaw(carLocationRaw)
    val fuelLiterUsed: Float get() = fuelLiterUsedRaw
    val fuelLiterPerLap: Float get() = fuelLiterPerLapRaw
    val lapsPossibleWithFuel: Float get() = lapsPossibleWithFuelRaw
    val batteryTemperature: Float get() = batteryTemperatureRaw
    val batteryVoltage: Float get() = batteryVoltageRaw
    val instantaneousFuelLiterPerKm: Float get() = instantaneousFuelLiterPerKmRaw
    val instantaneousKmPerFuelLiter: Float get() = instantaneousKmPerFuelLiterRaw
    val gearRpmWindow: Float get() = gearRpmWindowRaw
    val totalLapCount: Int get() = totalLapCountRaw
    val currentPos: Long get() = currentPosRaw.toAceUInt32()
    val totalDrivers: Long get() = totalDriversRaw.toAceUInt32()
    val lastLaptimeMs: Int get() = lastLaptimeMsRaw
    val bestLaptimeMs: Int get() = bestLaptimeMsRaw
    val flag: AcEvoFlagType? get() = AcEvoFlagType.fromRaw(flagRaw)
    val globalFlag: AcEvoFlagType? get() = AcEvoFlagType.fromRaw(globalFlagRaw)
    val maxGears: Long get() = maxGearsRaw.toAceUInt32()
    val engineType: AcEvoEngineType? get() = AcEvoEngineType.fromRaw(engineTypeRaw)
    val hasKers: Boolean get() = hasKersRaw.toAceBool()
    val isLastLap: Boolean get() = isLastLapRaw.toAceBool()
    val performanceModeName: String get() = performanceModeNameRaw.toAceCString()
    val diffCoastRawValue: Float get() = diffCoastRawValueRaw
    val diffPowerRawValue: Float get() = diffPowerRawValueRaw
    val raceCutGainedTimeMs: Int get() = raceCutGainedTimeMsRaw
    val distanceToDeadline: Int get() = distanceToDeadlineRaw
    val raceCutCurrentDelta: Float get() = raceCutCurrentDeltaRaw
    val playerPing: Int get() = playerPingRaw
    val playerLatency: Int get() = playerLatencyRaw
    val playerCpuUsage: Int get() = playerCpuUsageRaw
    val playerCpuUsageAvg: Int get() = playerCpuUsageAvgRaw
    val playerQos: Int get() = playerQosRaw
    val playerQosAvg: Int get() = playerQosAvgRaw
    val playerFps: Int get() = playerFpsRaw
    val playerFpsAvg: Int get() = playerFpsAvgRaw
    val driverName: String get() = driverNameRaw.toAceCString()
    val driverSurname: String get() = driverSurnameRaw.toAceCString()
    val carModel: String get() = carModelRaw.toAceCString()
    val isInPitBox: Boolean get() = isInPitBoxRaw.toAceBool()
    val isInPitLane: Boolean get() = isInPitLaneRaw.toAceBool()
    val isValidLap: Boolean get() = isValidLapRaw.toAceBool()
    val carCoordinates: FloatArray get() = carCoordinatesRaw
    val gapAhead: Float get() = gapAheadRaw
    val gapBehind: Float get() = gapBehindRaw
    val activeCars: Int get() = activeCarsRaw.toAceUInt8()
    val fuelPerLap: Float get() = fuelPerLapRaw
    val fuelEstimatedLaps: Float get() = fuelEstimatedLapsRaw
    val maxFuel: Float get() = maxFuelRaw
    val maxTurboBoost: Float get() = maxTurboBoostRaw
    val useSingleCompound: Boolean get() = useSingleCompoundRaw.toAceBool()

    val currentTyreCompound: String
        get() {
            val front = tyreLf.tyreCompoundFront
            val rear = tyreLf.tyreCompoundRear
            return when {
                front.isBlank() -> rear
                rear.isBlank() -> front
                front.equals(rear, ignoreCase = true) -> front
                else -> "$front / $rear"
            }
        }

    val isWetTyreCompound: Boolean
        get() {
            val compound = currentTyreCompound.lowercase()
            return compound.contains("wet") || compound.contains("rain") || compound.contains("inter")
        }

    val playerCarStableId: Int
        get() = stableId(playerCarIdA, playerCarIdB)

    public companion object {

        public const val SIZE_BYTES: Int = 4096

        private fun stableId(partA: Long, partB: Long): Int {
            var result = 17
            result = 31 * result + partA.hashCode()
            result = 31 * result + partB.hashCode()
            return if (result == 0) 1 else result
        }
    }
}
