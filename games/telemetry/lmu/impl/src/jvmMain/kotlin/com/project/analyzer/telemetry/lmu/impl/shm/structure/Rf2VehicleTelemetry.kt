package com.project.analyzer.telemetry.lmu.impl.shm.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "id",
    "deltaTime",
    "elapsedTime",
    "lapNumber",
    "lapStartEt",
    "vehicleName",
    "trackName",
    "pos",
    "localVel",
    "localAccel",
    "ori",
    "localRot",
    "localRotAccel",
    "gear",
    "engineRpm",
    "engineWaterTemp",
    "engineOilTemp",
    "clutchRpm",
    "unfilteredThrottle",
    "unfilteredBrake",
    "unfilteredSteering",
    "unfilteredClutch",
    "filteredThrottle",
    "filteredBrake",
    "filteredSteering",
    "filteredClutch",
    "steeringShaftTorque",
    "front3rdDeflection",
    "rear3rdDeflection",
    "frontWingHeight",
    "frontRideHeight",
    "rearRideHeight",
    "drag",
    "frontDownforce",
    "rearDownforce",
    "fuel",
    "engineMaxRpm",
    "scheduledStops",
    "overheating",
    "detached",
    "headlights",
    "dentSeverity",
    "lastImpactEt",
    "lastImpactMagnitude",
    "lastImpactPos",
    "engineTorque",
    "currentSector",
    "speedLimiter",
    "maxGears",
    "frontTireCompoundIndex",
    "rearTireCompoundIndex",
    "fuelCapacity",
    "frontFlapActivated",
    "rearFlapActivated",
    "rearFlapLegalStatus",
    "ignitionStarter",
    "frontTireCompoundName",
    "rearTireCompoundName",
    "speedLimiterAvailable",
    "antiStallActivated",
    "unused",
    "visualSteeringWheelRange",
    "rearBrakeBias",
    "turboBoostPressure",
    "physicsToGraphicsOffset",
    "physicalSteeringWheelRange",
    "deltaBest",
    "batteryChargeFraction",
    "electricBoostMotorTorque",
    "electricBoostMotorRpm",
    "electricBoostMotorTemperature",
    "electricBoostWaterTemperature",
    "electricBoostMotorState",
    "lapInvalidated",
    "absActive",
    "tcActive",
    "speedLimiterActive",
    "wiperState",
    "tc",
    "tcMax",
    "tcSlip",
    "tcSlipMax",
    "tcCut",
    "tcCutMax",
    "abs",
    "absMax",
    "motorMap",
    "motorMapMax",
    "migration",
    "migrationMax",
    "frontAntiSway",
    "frontAntiSwayMax",
    "rearAntiSway",
    "rearAntiSwayMax",
    "liftAndCoastProgress",
    "trackLimitsSteps",
    "regen",
    "stateOfCharge",
    "virtualEnergy",
    "timeGapCarAhead",
    "timeGapCarBehind",
    "timeGapPlaceAhead",
    "timeGapPlaceBehind",
    "vehicleModel",
    "vehicleClass",
    "vehicleChampionship",
    "expansion",
    "wheels",
)
internal class Rf2VehicleTelemetry : Pack4Structure() {

    fun attach(p: Pointer, offset: Int) {
        useMemory(p, offset)
    }

    @JvmField
    var id: Int = 0

    @JvmField
    var deltaTime: Double = 0.0

    @JvmField
    var elapsedTime: Double = 0.0

    @JvmField
    var lapNumber: Int = 0

    @JvmField
    var lapStartEt: Double = 0.0

    @JvmField
    var vehicleName: ByteArray = ByteArray(64)

    @JvmField
    var trackName: ByteArray = ByteArray(64)

    @JvmField
    var pos: DoubleArray = DoubleArray(3)

    @JvmField
    var localVel: DoubleArray = DoubleArray(3)

    @JvmField
    var localAccel: DoubleArray = DoubleArray(3)

    @JvmField
    var ori: DoubleArray = DoubleArray(9)

    @JvmField
    var localRot: DoubleArray = DoubleArray(3)

    @JvmField
    var localRotAccel: DoubleArray = DoubleArray(3)

    @JvmField
    var gear: Int = 0

    @JvmField
    var engineRpm: Double = 0.0

    @JvmField
    var engineWaterTemp: Double = 0.0

    @JvmField
    var engineOilTemp: Double = 0.0

    @JvmField
    var clutchRpm: Double = 0.0

    @JvmField
    var unfilteredThrottle: Double = 0.0

    @JvmField
    var unfilteredBrake: Double = 0.0

    @JvmField
    var unfilteredSteering: Double = 0.0

    @JvmField
    var unfilteredClutch: Double = 0.0

    @JvmField
    var filteredThrottle: Double = 0.0

    @JvmField
    var filteredBrake: Double = 0.0

    @JvmField
    var filteredSteering: Double = 0.0

    @JvmField
    var filteredClutch: Double = 0.0

    @JvmField
    var steeringShaftTorque: Double = 0.0

    @JvmField
    var front3rdDeflection: Double = 0.0

    @JvmField
    var rear3rdDeflection: Double = 0.0

    @JvmField
    var frontWingHeight: Double = 0.0

    @JvmField
    var frontRideHeight: Double = 0.0

    @JvmField
    var rearRideHeight: Double = 0.0

    @JvmField
    var drag: Double = 0.0

    @JvmField
    var frontDownforce: Double = 0.0

    @JvmField
    var rearDownforce: Double = 0.0

    @JvmField
    var fuel: Double = 0.0

    @JvmField
    var engineMaxRpm: Double = 0.0

    @JvmField
    var scheduledStops: Byte = 0

    @JvmField
    var overheating: Byte = 0

    @JvmField
    var detached: Byte = 0

    @JvmField
    var headlights: Byte = 0

    @JvmField
    var dentSeverity: ByteArray = ByteArray(8)

    @JvmField
    var lastImpactEt: Double = 0.0

    @JvmField
    var lastImpactMagnitude: Double = 0.0

    @JvmField
    var lastImpactPos: DoubleArray = DoubleArray(3)

    @JvmField
    var engineTorque: Double = 0.0

    @JvmField
    var currentSector: Int = 0

    @JvmField
    var speedLimiter: Byte = 0

    @JvmField
    var maxGears: Byte = 0

    @JvmField
    var frontTireCompoundIndex: Byte = 0

    @JvmField
    var rearTireCompoundIndex: Byte = 0

    @JvmField
    var fuelCapacity: Double = 0.0

    @JvmField
    var frontFlapActivated: Byte = 0

    @JvmField
    var rearFlapActivated: Byte = 0

    @JvmField
    var rearFlapLegalStatus: Byte = 0

    @JvmField
    var ignitionStarter: Byte = 0

    @JvmField
    var frontTireCompoundName: ByteArray = ByteArray(18)

    @JvmField
    var rearTireCompoundName: ByteArray = ByteArray(18)

    @JvmField
    var speedLimiterAvailable: Byte = 0

    @JvmField
    var antiStallActivated: Byte = 0

    @JvmField
    var unused: ByteArray = ByteArray(2)

    @JvmField
    var visualSteeringWheelRange: Float = 0f

    @JvmField
    var rearBrakeBias: Double = 0.0

    @JvmField
    var turboBoostPressure: Double = 0.0

    @JvmField
    var physicsToGraphicsOffset: FloatArray = FloatArray(3)

    @JvmField
    var physicalSteeringWheelRange: Float = 0f

    @JvmField
    var deltaBest: Double = 0.0

    @JvmField
    var batteryChargeFraction: Double = 0.0

    @JvmField
    var electricBoostMotorTorque: Double = 0.0

    @JvmField
    var electricBoostMotorRpm: Double = 0.0

    @JvmField
    var electricBoostMotorTemperature: Double = 0.0

    @JvmField
    var electricBoostWaterTemperature: Double = 0.0

    @JvmField
    var electricBoostMotorState: Byte = 0

    @JvmField
    var lapInvalidated: Byte = 0

    @JvmField
    var absActive: Byte = 0

    @JvmField
    var tcActive: Byte = 0

    @JvmField
    var speedLimiterActive: Byte = 0

    @JvmField
    var wiperState: Byte = 0

    @JvmField
    var tc: Byte = 0

    @JvmField
    var tcMax: Byte = 0

    @JvmField
    var tcSlip: Byte = 0

    @JvmField
    var tcSlipMax: Byte = 0

    @JvmField
    var tcCut: Byte = 0

    @JvmField
    var tcCutMax: Byte = 0

    @JvmField
    var abs: Byte = 0

    @JvmField
    var absMax: Byte = 0

    @JvmField
    var motorMap: Byte = 0

    @JvmField
    var motorMapMax: Byte = 0

    @JvmField
    var migration: Byte = 0

    @JvmField
    var migrationMax: Byte = 0

    @JvmField
    var frontAntiSway: Byte = 0

    @JvmField
    var frontAntiSwayMax: Byte = 0

    @JvmField
    var rearAntiSway: Byte = 0

    @JvmField
    var rearAntiSwayMax: Byte = 0

    @JvmField
    var liftAndCoastProgress: Byte = 0

    @JvmField
    var trackLimitsSteps: Byte = 0

    @JvmField
    var regen: Float = 0f

    @JvmField
    var stateOfCharge: Float = 0f

    @JvmField
    var virtualEnergy: Float = 0f

    @JvmField
    var timeGapCarAhead: Float = 0f

    @JvmField
    var timeGapCarBehind: Float = 0f

    @JvmField
    var timeGapPlaceAhead: Float = 0f

    @JvmField
    var timeGapPlaceBehind: Float = 0f

    @JvmField
    var vehicleModel: ByteArray = ByteArray(30)

    @JvmField
    var vehicleClass: Byte = 0

    @JvmField
    var vehicleChampionship: Byte = 0

    @JvmField
    var expansion: ByteArray = ByteArray(20)

    @JvmField
    @Suppress("UNCHECKED_CAST")
    var wheels: Array<Rf2Wheel> = Rf2Wheel().toArray(4) as Array<Rf2Wheel>

    public companion object {

        public const val SIZE: Int = 1888
        public const val OFFSET: Int = 16
    }
}
