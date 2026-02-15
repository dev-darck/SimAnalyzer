package com.project.analyzer.ac.telemetry.impl.shm.structure

import com.project.analyzer.utils.shm.Pack4Structure
import com.sun.jna.Pointer
import com.sun.jna.Structure

@Structure.FieldOrder(
    "smVersion",
    "acVersion",
    "numberOfSessions",
    "numCars",
    "carModel",
    "track",
    "playerName",
    "playerSurname",
    "playerNick",
    "sectorCount",
    "maxTorque",
    "maxPower",
    "maxRpm",
    "maxFuel",
    "suspensionMaxTravel",
    "tyreRadius",
    "maxTurboBoost",
    "deprecated1",
    "deprecated2",
    "penaltiesEnabled",
    "aidFuelRate",
    "aidTireRate",
    "aidMechanicalDamage",
    "aidAllowTyreBlankets",
    "aidStability",
    "aidAutoClutch",
    "aidAutoBlip",
    "hasDRS",
    "hasERS",
    "hasKERS",
    "kersMaxJ",
    "engineBrakeSettingsCount",
    "ersPowerControllerCount",
    "trackSPlineLength",
    "trackConfiguration",
    "ersMaxJ",
    "isTimedRace",
    "hasExtraLap",
    "carSkin",
    "reversedGridPositions",
    "pitWindowStart",
    "pitWindowEnd",
    "isOnline",
    "dryTyresName",
    "wetTyresName",
)
@Suppress(
    "MagicNumber",
    "VariableNaming",
    "TooManyFunctions",
    "SpacingBetweenDeclarationsWithAnnotations",
)
class SPageFileStatic(p: Pointer? = null) : Pack4Structure(p) {

    /**
     * Re-attach this Structure to a new native memory region (mapped file pointer).
     * Safe to call repeatedly.
     */
    fun attach(ptr: Pointer? = null) {
        if (ptr == null) return
        useMemory(ptr)
        read()
    }

    /** Shared memory version string (wchar_t[15]). */
    @JvmField
    var smVersion: CharArray = CharArray(15)

    /** Game version string (wchar_t[15]). */
    @JvmField
    var acVersion: CharArray = CharArray(15)

    /** Number of sessions in current event. */
    @JvmField
    var numberOfSessions: Int = 0

    /** Number of cars in session. */
    @JvmField
    var numCars: Int = 0

    /** Player car model id/name (wchar_t[33]). */
    @JvmField
    var carModel: CharArray = CharArray(33)

    /** Track id/name (wchar_t[33]). */
    @JvmField
    var track: CharArray = CharArray(33)

    /** Player first name (wchar_t[33]). */
    @JvmField
    var playerName: CharArray = CharArray(33)

    /** Player last name (wchar_t[33]). */
    @JvmField
    var playerSurname: CharArray = CharArray(33)

    /** Player nickname (wchar_t[33]). */
    @JvmField
    var playerNick: CharArray = CharArray(33)

    /** Number of sectors on track. */
    @JvmField
    var sectorCount: Int = 0

    /** Max engine torque (Nm). */
    @JvmField
    var maxTorque: Float = 0f

    /** Max engine power (kW or hp depending on game). */
    @JvmField
    var maxPower: Float = 0f

    /** Max RPM. */
    @JvmField
    var maxRpm: Int = 0

    /** Max fuel capacity (liters). */
    @JvmField
    var maxFuel: Float = 0f

    /** Suspension max travel for 4 wheels (meters). */
    @JvmField
    var suspensionMaxTravel: FloatArray = FloatArray(4)

    /** Tyre radius for 4 wheels (meters). */
    @JvmField
    var tyreRadius: FloatArray = FloatArray(4)

    /** Max turbo boost (bar or game-specific). */
    @JvmField
    var maxTurboBoost: Float = 0f

    /** Deprecated field (keep for binary layout compatibility). */
    @JvmField
    var deprecated1: Float = 0f

    /** Deprecated field (keep for binary layout compatibility). */
    @JvmField
    var deprecated2: Float = 0f

    /** Penalties enabled (0/1). */
    @JvmField
    var penaltiesEnabled: Int = 0

    /** Aid fuel rate multiplier. */
    @JvmField
    var aidFuelRate: Float = 0f

    /** Aid tyre rate multiplier. */
    @JvmField
    var aidTireRate: Float = 0f

    /** Aid mechanical damage multiplier. */
    @JvmField
    var aidMechanicalDamage: Float = 0f

    /** Tyre blankets allowed (0/1). */
    @JvmField
    var aidAllowTyreBlankets: Int = 0

    /** Stability aid (0..1). */
    @JvmField
    var aidStability: Float = 0f

    /** Auto clutch (0/1). */
    @JvmField
    var aidAutoClutch: Int = 0

    /** Auto blip (0/1). */
    @JvmField
    var aidAutoBlip: Int = 0

    /** DRS supported (0/1). */
    @JvmField
    var hasDRS: Int = 0

    /** ERS supported (0/1). */
    @JvmField
    var hasERS: Int = 0

    /** KERS supported (0/1). */
    @JvmField
    var hasKERS: Int = 0

    /** KERS max energy (J). */
    @JvmField
    var kersMaxJ: Float = 0f

    /** Engine brake settings count. */
    @JvmField
    var engineBrakeSettingsCount: Int = 0

    /** ERS power controller settings count. */
    @JvmField
    var ersPowerControllerCount: Int = 0

    /** Track spline length (meters). */
    @JvmField
    var trackSPlineLength: Float = 0f

    /** Track configuration name/id (wchar_t[33]). */
    @JvmField
    var trackConfiguration: CharArray = CharArray(33)

    /** ERS max energy (J). */
    @JvmField
    var ersMaxJ: Float = 0f

    /** Timed race (0/1). */
    @JvmField
    var isTimedRace: Int = 0

    /** Extra lap flag (0/1). */
    @JvmField
    var hasExtraLap: Int = 0

    /** Car skin name/id (wchar_t[33]). */
    @JvmField
    var carSkin: CharArray = CharArray(33)

    /** Reversed grid positions (0/1 or count depending on game). */
    @JvmField
    var reversedGridPositions: Int = 0

    /** Pit window start (minutes or session-specific units). */
    @JvmField
    var pitWindowStart: Int = 0

    /** Pit window end (minutes or session-specific units). */
    @JvmField
    var pitWindowEnd: Int = 0

    /** Online session (0/1). */
    @JvmField
    var isOnline: Int = 0

    /** Dry tyres name (wchar_t[33]). */
    @JvmField
    var dryTyresName: CharArray = CharArray(33)

    /** Wet tyres name (wchar_t[33]). */
    @JvmField
    var wetTyresName: CharArray = CharArray(33)

    override fun toString(): String = buildString {
        fun FloatArray.dump(count: Int = size, d: Int = 2): String =
            take(count).joinToString(", ", "[", "]") { "%.${d}f".format(it) }

        appendLine("SPageFileStatic {")
        appendLine("  smVersion = \"${smVersion.toKString()}\"")
        appendLine("  acVersion = \"${acVersion.toKString()}\"")
        appendLine("  numberOfSessions = $numberOfSessions")
        appendLine("  numCars = $numCars")
        appendLine("  carModel = \"${carModel.toKString()}\" raw=${carModel.rawCodes()}")
        appendLine("  track = \"${track.toKString()}\" raw=${track.rawCodes()}")
        appendLine("  playerName = \"${playerName.toKString()}\"")
        appendLine("  playerSurname = \"${playerSurname.toKString()}\"")
        appendLine("  playerNick = \"${playerNick.toKString()}\"")
        appendLine("  sectorCount = $sectorCount")
        appendLine("  maxTorque = ${"%.1f".format(maxTorque)} Nm")
        appendLine("  maxPower = ${"%.1f".format(maxPower)} (kW/hp?)")
        appendLine("  maxRpm = $maxRpm RPM")
        appendLine("  maxFuel = ${"%.1f".format(maxFuel)} L")
        appendLine("  suspensionMaxTravel = ${suspensionMaxTravel.dump(4, 3)} m")
        appendLine("  tyreRadius = ${tyreRadius.dump(4, 3)} m")
        appendLine("  maxTurboBoost = ${"%.3f".format(maxTurboBoost)}")
        appendLine("  penaltiesEnabled = ${penaltiesEnabled.toBoolean()}")
        appendLine("  aidFuelRate = ${"%.2f".format(aidFuelRate)}")
        appendLine("  aidTireRate = ${"%.2f".format(aidTireRate)}")
        appendLine("  aidMechanicalDamage = ${"%.2f".format(aidMechanicalDamage)}")
        appendLine("  aidAllowTyreBlankets = ${aidAllowTyreBlankets.toBoolean()}")
        appendLine("  aidStability = ${"%.2f".format(aidStability)}")
        appendLine("  aidAutoClutch = ${aidAutoClutch.toBoolean()}")
        appendLine("  aidAutoBlip = ${aidAutoBlip.toBoolean()}")
        appendLine("  hasDRS = ${hasDRS.toBoolean()}")
        appendLine("  hasERS = ${hasERS.toBoolean()}")
        appendLine("  hasKERS = ${hasKERS.toBoolean()}")
        appendLine("  kersMaxJ = ${"%.0f".format(kersMaxJ)} J")
        appendLine("  engineBrakeSettingsCount = $engineBrakeSettingsCount")
        appendLine("  ersPowerControllerCount = $ersPowerControllerCount")
        appendLine("  trackSPlineLength = ${"%.1f".format(trackSPlineLength)} m")
        appendLine("  trackConfiguration = \"${trackConfiguration.toKString()}\" raw=${trackConfiguration.rawCodes()}")
        appendLine("  ersMaxJ = ${"%.0f".format(ersMaxJ)} J")
        appendLine("  isTimedRace = ${isTimedRace.toBoolean()}")
        appendLine("  hasExtraLap = ${hasExtraLap.toBoolean()}")
        appendLine("  carSkin = \"${carSkin.toKString()}\"")
        appendLine("  reversedGridPositions = $reversedGridPositions")
        appendLine("  pitWindowStart = $pitWindowStart")
        appendLine("  pitWindowEnd = $pitWindowEnd")
        appendLine("  isOnline = ${isOnline.toBoolean()}")
        appendLine("  dryTyresName = \"${dryTyresName.toKString()}\"")
        appendLine("  wetTyresName = \"${wetTyresName.toKString()}\"")
        appendLine("}")
    }
}
