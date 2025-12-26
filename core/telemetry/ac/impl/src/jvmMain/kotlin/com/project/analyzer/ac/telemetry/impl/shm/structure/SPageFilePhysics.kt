package com.project.analyzer.ac.telemetry.impl.shm.structure

import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Assetto Corsa / ACC shared memory page: PHYSICS
 * Notes:
 * - FieldOrder names MUST match @JvmField var names EXACTLY (case-sensitive).
 * - Most "bool" values are stored as int (0/1).
 */
@Structure.FieldOrder(
    "packetId",
    "gas",
    "brake",
    "fuel",
    "gear",
    "rpm",
    "steerAngle",
    "speedKmh",
    "velocity",
    "accG",
    "wheelSlip",
    "wheelLoad",
    "wheelsPressure",
    "wheelAngularSpeed",
    "tyreWear",
    "tyreDirtyLevel",
    "tyreCoreTemperature",
    "camberRad",
    "suspensionTravel",
    "drs",
    "tc",
    "heading",
    "pitch",
    "roll",
    "cgHeight",
    "carDamage",
    "numberOfTyresOut",
    "pitLimiterOn",
    "abs",
    "kersCharge",
    "kersInput",
    "autoShifterOn",
    "rideHeight",
    "turboBoost",
    "ballast",
    "airDensity",
    "airTemp",
    "roadTemp",
    "localAngularVel",
    "finalFF",
    "performanceMeter",
    "engineBrake",
    "ersRecovery",
    "ersPower",
    "ersHeatCharging",
    "kersCurrentKJ",
    "drsAvailable",
    "drsEnabled",
    "brakeTemp",
    "clutch",
    "tyreTempI",
    "tyreTempM",
    "tyreTempO",
    "isAIControlled",
    "tyreContactPoint",
    "tyreContactNormal",
    "tyreContactHeading",
    "brakeBias",
    "localVelocity",
    "P2PActivations",
    "P2PStatus",
    "currentMaxRPM",
    "mz",
    "fx",
    "fy",
    "tyreSlipRatio",
    "tyreSlipAngle",
    "tcInAction",
    "absInAction",
    "suspensionDamage",
    "tyreTemp",
    "waterTemp",
    "brakePressure",
    "frontBrakeCompound",
    "rearBrakeCompound",
    "padLife",
    "discLife",
    "ignitionOn",
    "starterEngineOn",
    "isEngineRunning",
    "kerbVibration",
    "slipVibrations",
    "gVibrations",
    "absVibrations",
)
@Suppress(
    "MagicNumber",
    "VariableNaming",
    "TooManyFunctions",
    "SpacingBetweenDeclarationsWithAnnotations",
)
class SPageFilePhysics(p: Pointer? = null) : Structure(p) {

    /**
     * Re-attach this Structure to a new native memory region (mapped file pointer).
     * Safe to call repeatedly.
     */
    fun attach(ptr: Pointer? = null) {
        if (ptr == null) return
        useMemory(ptr)
        read()
    }

    /** Packet counter / sequence id. */
    @JvmField var packetId: Int = 0

    /** Throttle (0..1). */
    @JvmField var gas: Float = 0f

    /** Brake (0..1). */
    @JvmField var brake: Float = 0f

    /** Fuel (liters). */
    @JvmField var fuel: Float = 0f

    /** Gear (implementation-specific). */
    @JvmField var gear: Int = 0

    /** Engine RPM. */
    @JvmField var rpm: Int = 0

    /** Steering angle (radians). */
    @JvmField var steerAngle: Float = 0f

    /** Speed (km/h). */
    @JvmField var speedKmh: Float = 0f

    /** World velocity vector (m/s). */
    @JvmField var velocity: FloatArray = FloatArray(3)

    /** Acceleration in G (x/y/z). */
    @JvmField var accG: FloatArray = FloatArray(3)

    /** Wheel slip (4 wheels). */
    @JvmField var wheelSlip: FloatArray = FloatArray(4)

    /** Wheel load (N or game-specific). */
    @JvmField var wheelLoad: FloatArray = FloatArray(4)

    /** Wheel pressure (psi/bar depending on game). */
    @JvmField var wheelsPressure: FloatArray = FloatArray(4)

    /** Wheel angular speed (rad/s). */
    @JvmField var wheelAngularSpeed: FloatArray = FloatArray(4)

    /** Tyre wear (0..1). */
    @JvmField var tyreWear: FloatArray = FloatArray(4)

    /** Tyre dirty level (0..1). */
    @JvmField var tyreDirtyLevel: FloatArray = FloatArray(4)

    /** Tyre core temperature (C). */
    @JvmField var tyreCoreTemperature: FloatArray = FloatArray(4)

    /** Camber (radians). */
    @JvmField var camberRad: FloatArray = FloatArray(4)

    /** Suspension travel (meters). */
    @JvmField var suspensionTravel: FloatArray = FloatArray(4)

    /** DRS (0..1). */
    @JvmField var drs: Float = 0f

    /** Traction control (0..). */
    @JvmField var tc: Float = 0f

    /** Heading (radians). */
    @JvmField var heading: Float = 0f

    /** Pitch (radians). */
    @JvmField var pitch: Float = 0f

    /** Roll (radians). */
    @JvmField var roll: Float = 0f

    /** Center of gravity height (meters). */
    @JvmField var cgHeight: Float = 0f

    /** Car damage array (game-specific indices). */
    @JvmField var carDamage: FloatArray = FloatArray(5)

    /** Number of tyres out of track limits. */
    @JvmField var numberOfTyresOut: Int = 0

    /** Pit limiter on/off (0/1). */
    @JvmField var pitLimiterOn: Int = 0

    /** ABS (0..). */
    @JvmField var abs: Float = 0f

    /** KERS charge (0..1). */
    @JvmField var kersCharge: Float = 0f

    /** KERS input (0..1). */
    @JvmField var kersInput: Float = 0f

    /** Auto shifter enabled (0/1). */
    @JvmField var autoShifterOn: Int = 0

    /** Ride height (front/rear meters). */
    @JvmField var rideHeight: FloatArray = FloatArray(2)

    /** Turbo boost (bar or game-specific). */
    @JvmField var turboBoost: Float = 0f

    /** Ballast (kg). */
    @JvmField var ballast: Float = 0f

    /** Air density (kg/m^3). */
    @JvmField var airDensity: Float = 0f

    /** Air temperature (C). */
    @JvmField var airTemp: Float = 0f

    /** Road temperature (C). */
    @JvmField var roadTemp: Float = 0f

    /** Local angular velocity (rad/s). */
    @JvmField var localAngularVel: FloatArray = FloatArray(3)

    /** Final force feedback value. */
    @JvmField var finalFF: Float = 0f

    /** Performance meter (game-specific). */
    @JvmField var performanceMeter: Float = 0f

    /** Engine brake setting index. */
    @JvmField var engineBrake: Int = 0

    /** ERS recovery level. */
    @JvmField var ersRecovery: Int = 0

    /** ERS power level. */
    @JvmField var ersPower: Int = 0

    /** ERS heat charging state. */
    @JvmField var ersHeatCharging: Int = 0

    /** Current KERS energy (kJ). */
    @JvmField var kersCurrentKJ: Float = 0f

    /** DRS available (0/1). */
    @JvmField var drsAvailable: Int = 0

    /** DRS enabled (0/1). */
    @JvmField var drsEnabled: Int = 0

    /** Brake temperatures (C). */
    @JvmField var brakeTemp: FloatArray = FloatArray(4)

    /** Clutch (0..1). */
    @JvmField var clutch: Float = 0f

    /** Tyre inner temp (C). */
    @JvmField var tyreTempI: FloatArray = FloatArray(4)

    /** Tyre middle temp (C). */
    @JvmField var tyreTempM: FloatArray = FloatArray(4)

    /** Tyre outer temp (C). */
    @JvmField var tyreTempO: FloatArray = FloatArray(4)

    /** Is AI controlled (0/1). */
    @JvmField var isAIControlled: Int = 0

    /** Tyre contact point (4 wheels * 3 floats). */
    @JvmField var tyreContactPoint: FloatArray = FloatArray(12)

    /** Tyre contact normal (4 wheels * 3 floats). */
    @JvmField var tyreContactNormal: FloatArray = FloatArray(12)

    /** Tyre contact heading (4 wheels * 3 floats). */
    @JvmField var tyreContactHeading: FloatArray = FloatArray(12)

    /** Brake bias (0..1 or game-specific). */
    @JvmField var brakeBias: Float = 0f

    /** Local velocity vector (m/s). */
    @JvmField var localVelocity: FloatArray = FloatArray(3)

    /** Push-to-pass activations left (if supported). */
    @JvmField var P2PActivations: Int = 0

    /** Push-to-pass status (if supported). */
    @JvmField var P2PStatus: Int = 0

    /** Current max RPM. */
    @JvmField var currentMaxRPM: Float = 0f

    /** Mz forces (4 wheels). */
    @JvmField var mz: FloatArray = FloatArray(4)

    /** Fx forces (4 wheels). */
    @JvmField var fx: FloatArray = FloatArray(4)

    /** Fy forces (4 wheels). */
    @JvmField var fy: FloatArray = FloatArray(4)

    /** Tyre slip ratio (4 wheels). */
    @JvmField var tyreSlipRatio: FloatArray = FloatArray(4)

    /** Tyre slip angle (4 wheels). */
    @JvmField var tyreSlipAngle: FloatArray = FloatArray(4)

    /** TC in action (0/1 or game-specific). */
    @JvmField var tcInAction: Int = 0

    /** ABS in action (0/1 or game-specific). */
    @JvmField var absInAction: Int = 0

    /** Suspension damage (4 wheels). */
    @JvmField var suspensionDamage: FloatArray = FloatArray(4)

    /** Generic tyre temp (4 wheels, C). */
    @JvmField var tyreTemp: FloatArray = FloatArray(4)

    /** Water temperature (C). */
    @JvmField var waterTemp: Float = 0f

    /** Brake pressure (4 wheels). */
    @JvmField var brakePressure: FloatArray = FloatArray(4)

    /** Front brake compound (game-specific int). */
    @JvmField var frontBrakeCompound: Int = 0

    /** Rear brake compound (game-specific int). */
    @JvmField var rearBrakeCompound: Int = 0

    /** Brake pad life (4 wheels). */
    @JvmField var padLife: FloatArray = FloatArray(4)

    /** Brake disc life (4 wheels). */
    @JvmField var discLife: FloatArray = FloatArray(4)

    /** Ignition on/off (0/1). */
    @JvmField var ignitionOn: Int = 0

    /** Starter on/off (0/1). */
    @JvmField var starterEngineOn: Int = 0

    /** Engine running (0/1). */
    @JvmField var isEngineRunning: Int = 0

    /** Kerb vibration feedback. */
    @JvmField var kerbVibration: Float = 0f

    /** Slip vibration feedback. */
    @JvmField var slipVibrations: Float = 0f

    /** G-force vibration feedback. */
    @JvmField var gVibrations: Float = 0f

    /** ABS vibration feedback. */
    @JvmField var absVibrations: Float = 0f
}
