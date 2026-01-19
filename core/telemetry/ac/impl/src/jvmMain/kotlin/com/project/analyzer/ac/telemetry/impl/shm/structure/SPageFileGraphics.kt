package com.project.analyzer.ac.telemetry.impl.shm.structure

import com.project.analyzer.utils.ext.fmt
import com.project.analyzer.utils.ext.formatFloatArray
import com.project.analyzer.utils.ext.formatIntArray
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * Assetto Corsa / ACC shared memory page: GRAPHICS
 * Notes:
 * - Strings are wchar_t (UTF-16 on Windows) -> CharArray in JVM.
 * - FieldOrder names MUST match @JvmField var names EXACTLY (case-sensitive).
 */
@Structure.FieldOrder(
    "packetId",
    "status",
    "session",
    "currentTime",
    "lastTime",
    "bestTime",
    "split",
    "completedLaps",
    "position",
    "iCurrentTime",
    "iLastTime",
    "iBestTime",
    "sessionTimeLeft",
    "distanceTraveled",
    "isInPit",
    "currentSectorIndex",
    "lastSectorTime",
    "numberOfLaps",
    "tyreCompound",
    "replayTimeMultiplier",
    "normalizedCarPosition",
    "activeCars",
    "carCoordinates",
    "carID",
    "playerCarID",
    "penaltyTime",
    "flag",
    "penalty",
    "idealLineOn",
    "isInPitLane",
    "surfaceGrip",
    "mandatoryPitDone",
    "windSpeed",
    "windDirection",
    "isSetupMenuVisible",
    "mainDisplayIndex",
    "secondaryDisplayIndex",
    "tc",
    "tcCut",
    "engineMap",
    "abs",
    "fuelXLap",
    "rainLights",
    "flashingLights",
    "lightsStage",
    "exhaustTemperature",
    "wiperLV",
    "driverStintTotalTimeLeft",
    "driverStintTimeLeft",
    "rainTyres",
    "sessionIndex",
    "usedFuel",
    "deltaLapTime",
    "iDeltaLapTime",
    "estimatedLapTime",
    "iEstimatedLapTime",
    "isDeltaPositive",
    "iSplit",
    "isValidLap",
    "fuelEstimatedLaps",
    "trackStatus",
    "missingMandatoryPits",
    "clock",
    "directionLightsLeft",
    "directionLightsRight",
    "globalYellow",
    "globalYellow1",
    "globalYellow2",
    "globalYellow3",
    "globalWhite",
    "globalGreen",
    "globalChequered",
    "globalRed",
    "mfdTyreSet",
    "mfdFuelToAdd",
    "mfdTyrePressureLF",
    "mfdTyrePressureRF",
    "mfdTyrePressureLR",
    "mfdTyrePressureRR",
    "trackGripStatus",
    "rainIntensity",
    "rainIntensityIn10min",
    "rainIntensityIn30min",
    "currentTyreSet",
    "strategyTyreSet",
    "gapAhead",
    "gapBehind",
)
@Suppress(
    "MagicNumber",
    "VariableNaming",
    "TooManyFunctions",
    "SpacingBetweenDeclarationsWithAnnotations",
)
class SPageFileGraphics(p: Pointer? = null) : Structure(p) {

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
    @JvmField
    var packetId: Int = 0

    /** Game status (implementation-specific; often 0=off). */
    @JvmField
    var status: Int = 0

    /** Session type (practice/qualify/race/etc). */
    @JvmField
    var session: Int = 0

    /** Current lap time as string (wchar_t[15], e.g. "1:23.456"). */
    @JvmField
    var currentTime: CharArray = CharArray(15)

    /** Last lap time as string (wchar_t[15]). */
    @JvmField
    var lastTime: CharArray = CharArray(15)

    /** Best lap time as string (wchar_t[15]). */
    @JvmField
    var bestTime: CharArray = CharArray(15)

    /** Split time as string (wchar_t[15]). */
    @JvmField
    var split: CharArray = CharArray(15)

    /** Completed laps count. */
    @JvmField
    var completedLaps: Int = 0

    /** Player position. */
    @JvmField
    var position: Int = 0

    /** Current lap time in milliseconds (often). */
    @JvmField
    var iCurrentTime: Int = 0

    /** Last lap time in milliseconds (often). */
    @JvmField
    var iLastTime: Int = 0

    /** Best lap time in milliseconds (often). */
    @JvmField
    var iBestTime: Int = 0

    /** Session time left (seconds). */
    @JvmField
    var sessionTimeLeft: Float = 0f

    /** Distance traveled (meters). */
    @JvmField
    var distanceTraveled: Float = 0f

    /** 1 if in pit, 0 otherwise. */
    @JvmField
    var isInPit: Int = 0

    /** Current sector index (0..N-1). */
    @JvmField
    var currentSectorIndex: Int = 0

    /** Last sector time (ms). */
    @JvmField
    var lastSectorTime: Int = 0

    /** Total number of laps in session (if known). */
    @JvmField
    var numberOfLaps: Int = 0

    /** Current tyre compound name (wchar_t[33]). */
    @JvmField
    var tyreCompound: CharArray = CharArray(33)

    /** Replay time multiplier. */
    @JvmField
    var replayTimeMultiplier: Float = 0f

    /** Normalized track position (0..1). */
    @JvmField
    var normalizedCarPosition: Float = 0f

    /** Count of active cars. */
    @JvmField
    var activeCars: Int = 0

    /** Car world coordinates: 60 cars * 3 floats = 180 floats. */
    @JvmField
    var carCoordinates: FloatArray = FloatArray(180)

    /** Car ids for up to 60 cars. */
    @JvmField
    var carID: IntArray = IntArray(60)

    /** Player car id. */
    @JvmField
    var playerCarID: Int = 0

    /** Penalty time (seconds). */
    @JvmField
    var penaltyTime: Float = 0f

    /** Flag state (game-specific enum). */
    @JvmField
    var flag: Int = 0

    /** Penalty state (game-specific enum). */
    @JvmField
    var penalty: Int = 0

    /** Ideal line on/off. */
    @JvmField
    var idealLineOn: Int = 0

    /** 1 if in pit lane, 0 otherwise. */
    @JvmField
    var isInPitLane: Int = 0

    /** Surface grip (0..1 or game-specific). */
    @JvmField
    var surfaceGrip: Float = 0f

    /** Mandatory pit done (bool int). */
    @JvmField
    var mandatoryPitDone: Int = 0

    /** Wind speed (m/s). */
    @JvmField
    var windSpeed: Float = 0f

    /** Wind direction (degrees or radians depending on game). */
    @JvmField
    var windDirection: Float = 0f

    /** Setup menu visibility (bool int). */
    @JvmField
    var isSetupMenuVisible: Int = 0

    /** Main HUD page index. */
    @JvmField
    var mainDisplayIndex: Int = 0

    /** Secondary HUD page index. */
    @JvmField
    var secondaryDisplayIndex: Int = 0

    /** Traction control level. */
    @JvmField
    var tc: Int = 0

    /** Traction control cut level. */
    @JvmField
    var tcCut: Int = 0

    /** Engine map index. */
    @JvmField
    var engineMap: Int = 0

    /** ABS level. */
    @JvmField
    var abs: Int = 0

    /** Fuel per lap (estimated). */
    @JvmField
    var fuelXLap: Float = 0f

    /** Rain lights on/off. */
    @JvmField
    var rainLights: Int = 0

    /** Flashing lights on/off. */
    @JvmField
    var flashingLights: Int = 0

    /** Lights stage (game-specific). */
    @JvmField
    var lightsStage: Int = 0

    /** Exhaust temperature (C). */
    @JvmField
    var exhaustTemperature: Float = 0f

    /** Wiper level. */
    @JvmField
    var wiperLV: Int = 0

    /** Total stint time left (seconds). */
    @JvmField
    var driverStintTotalTimeLeft: Int = 0

    /** Stint time left (seconds). */
    @JvmField
    var driverStintTimeLeft: Int = 0

    /** Rain tyres active (bool int). */
    @JvmField
    var rainTyres: Int = 0

    /** Session index (0..). */
    @JvmField
    var sessionIndex: Int = 0

    /** Used fuel since session start (liters). */
    @JvmField
    var usedFuel: Float = 0f

    /** Delta lap time as string (wchar_t[15]). */
    @JvmField
    var deltaLapTime: CharArray = CharArray(15)

    /** Delta lap time in ms (often). */
    @JvmField
    var iDeltaLapTime: Int = 0

    /** Estimated lap time as string (wchar_t[15]). */
    @JvmField
    var estimatedLapTime: CharArray = CharArray(15)

    /** Estimated lap time in ms (often). */
    @JvmField
    var iEstimatedLapTime: Int = 0

    /** 1 if delta is positive, 0 otherwise. */
    @JvmField
    var isDeltaPositive: Int = 0

    /** Split time in ms (often). */
    @JvmField
    var iSplit: Int = 0

    /** Current lap validity (bool int). */
    @JvmField
    var isValidLap: Int = 0

    /** Estimated laps left based on fuel. */
    @JvmField
    var fuelEstimatedLaps: Float = 0f

    /** Track status string (wchar_t[33]). */
    @JvmField
    var trackStatus: CharArray = CharArray(33)

    /** Missing mandatory pits count. */
    @JvmField
    var missingMandatoryPits: Int = 0

    /** Clock (seconds). */
    @JvmField
    var clock: Float = 0f

    /** Left direction lights (bool int). */
    @JvmField
    var directionLightsLeft: Int = 0

    /** Right direction lights (bool int). */
    @JvmField
    var directionLightsRight: Int = 0

    /** Global yellow flag (bool int). */
    @JvmField
    var globalYellow: Int = 0

    /** Global yellow sector 1 (bool int). */
    @JvmField
    var globalYellow1: Int = 0

    /** Global yellow sector 2 (bool int). */
    @JvmField
    var globalYellow2: Int = 0

    /** Global yellow sector 3 (bool int). */
    @JvmField
    var globalYellow3: Int = 0

    /** Global white flag (bool int). */
    @JvmField
    var globalWhite: Int = 0

    /** Global green flag (bool int). */
    @JvmField
    var globalGreen: Int = 0

    /** Global chequered flag (bool int). */
    @JvmField
    var globalChequered: Int = 0

    /** Global red flag (bool int). */
    @JvmField
    var globalRed: Int = 0

    /** MFD tyre set (int). */
    @JvmField
    var mfdTyreSet: Int = 0

    /** MFD fuel to add (liters). */
    @JvmField
    var mfdFuelToAdd: Float = 0f

    /** MFD tyre pressure LF (psi/bar depending on game). */
    @JvmField
    var mfdTyrePressureLF: Float = 0f

    /** MFD tyre pressure RF. */
    @JvmField
    var mfdTyrePressureRF: Float = 0f

    /** MFD tyre pressure LR. */
    @JvmField
    var mfdTyrePressureLR: Float = 0f

    /** MFD tyre pressure RR. */
    @JvmField
    var mfdTyrePressureRR: Float = 0f

    /** Track grip status (game-specific enum). */
    @JvmField
    var trackGripStatus: Int = 0

    /** Rain intensity (game-specific). */
    @JvmField
    var rainIntensity: Int = 0

    /** Rain intensity in 10 minutes (forecast). */
    @JvmField
    var rainIntensityIn10min: Int = 0

    /** Rain intensity in 30 minutes (forecast). */
    @JvmField
    var rainIntensityIn30min: Int = 0

    /** Current tyre set index. */
    @JvmField
    var currentTyreSet: Int = 0

    /** Strategy tyre set index. */
    @JvmField
    var strategyTyreSet: Int = 0

    /** Gap ahead (ms or game-specific units). */
    @JvmField
    var gapAhead: Int = 0

    /** Gap behind (ms or game-specific units). */
    @JvmField
    var gapBehind: Int = 0

    override fun toString(): String = buildString {
        val maxCoords = (activeCars * 3).coerceAtMost(120)
        val maxIds = activeCars.coerceAtMost(60)

        appendLine("SPageFileGraphics {")
        appendLine("  packetId = $packetId")
        appendLine("  status = $status")
        appendLine("  session = $session")
        appendLine("  currentTime = \"${currentTime.toKString()}\"")
        appendLine("  lastTime = \"${lastTime.toKString()}\"")
        appendLine("  bestTime = \"${bestTime.toKString()}\"")
        appendLine("  split = \"${split.toKString()}\"")
        appendLine("  completedLaps = $completedLaps")
        appendLine("  position = $position")
        appendLine("  iCurrentTime = $iCurrentTime ms")
        appendLine("  iLastTime = $iLastTime ms")
        appendLine("  iBestTime = $iBestTime ms")
        appendLine("  sessionTimeLeft = ${sessionTimeLeft.fmt(1)} s")
        appendLine("  distanceTraveled = ${distanceTraveled.fmt(1)} m")
        appendLine("  isInPit = ${isInPit.toBoolean()}")
        appendLine("  currentSectorIndex = $currentSectorIndex")
        appendLine("  lastSectorTime = $lastSectorTime ms")
        appendLine("  numberOfLaps = $numberOfLaps")
        appendLine("  tyreCompound = \"${tyreCompound.toKString()}\"")
        appendLine("  replayTimeMultiplier = ${replayTimeMultiplier.fmt(2)}")
        appendLine("  normalizedCarPosition = ${normalizedCarPosition.fmt(3)}")
        appendLine("  activeCars = $activeCars")
        appendLine("  carCoordinates = ${carCoordinates.formatFloatArray(max = maxCoords, decimals = 1)}")
        appendLine("  carID = ${carID.formatIntArray(max = maxIds)}")
        appendLine("  playerCarID = $playerCarID")
        appendLine("  penaltyTime = ${penaltyTime.fmt(1)} s")
        appendLine("  flag = $flag")
        appendLine("  penalty = $penalty")
        appendLine("  idealLineOn = ${idealLineOn.toBoolean()}")
        appendLine("  isInPitLane = ${isInPitLane.toBoolean()}")
        appendLine("  surfaceGrip = ${surfaceGrip.fmt(3)}")
        appendLine("  mandatoryPitDone = ${mandatoryPitDone.toBoolean()}")
        appendLine("  windSpeed = ${windSpeed.fmt(1)} m/s")
        appendLine("  windDirection = ${windDirection.fmt(1)}°")
        appendLine("  isSetupMenuVisible = ${isSetupMenuVisible.toBoolean()}")
        appendLine("  mainDisplayIndex = $mainDisplayIndex")
        appendLine("  secondaryDisplayIndex = $secondaryDisplayIndex")
        appendLine("  tc = $tc")
        appendLine("  tcCut = $tcCut")
        appendLine("  engineMap = $engineMap")
        appendLine("  abs = $abs")
        appendLine("  fuelXLap = ${fuelXLap.fmt(2)} L")
        appendLine("  rainLights = ${rainLights.toBoolean()}")
        appendLine("  flashingLights = ${flashingLights.toBoolean()}")
        appendLine("  lightsStage = $lightsStage")
        appendLine("  exhaustTemperature = ${exhaustTemperature.fmt(0)} °C")
        appendLine("  wiperLV = $wiperLV")
        appendLine("  driverStintTotalTimeLeft = $driverStintTotalTimeLeft s")
        appendLine("  driverStintTimeLeft = $driverStintTimeLeft s")
        appendLine("  rainTyres = ${rainTyres.toBoolean()}")
        appendLine("  sessionIndex = $sessionIndex")
        appendLine("  usedFuel = ${usedFuel.fmt(2)} L")
        appendLine("  deltaLapTime = \"${deltaLapTime.toKString()}\"")
        appendLine("  iDeltaLapTime = $iDeltaLapTime ms")
        appendLine("  estimatedLapTime = \"${estimatedLapTime.toKString()}\"")
        appendLine("  iEstimatedLapTime = $iEstimatedLapTime ms")
        appendLine("  isDeltaPositive = ${isDeltaPositive.toBoolean()}")
        appendLine("  iSplit = $iSplit ms")
        appendLine("  isValidLap = ${isValidLap.toBoolean()}")
        appendLine("  fuelEstimatedLaps = ${fuelEstimatedLaps.fmt(1)}")
        appendLine("  trackStatus = \"${trackStatus.toKString()}\"")
        appendLine("  missingMandatoryPits = $missingMandatoryPits")
        appendLine("  clock = ${clock.fmt(1)} s")
        appendLine("  directionLightsLeft = ${directionLightsLeft.toBoolean()}")
        appendLine("  directionLightsRight = ${directionLightsRight.toBoolean()}")
        appendLine("  globalYellow = ${globalYellow.toBoolean()}")
        appendLine("  globalYellow1 = ${globalYellow1.toBoolean()}")
        appendLine("  globalYellow2 = ${globalYellow2.toBoolean()}")
        appendLine("  globalYellow3 = ${globalYellow3.toBoolean()}")
        appendLine("  globalWhite = ${globalWhite.toBoolean()}")
        appendLine("  globalGreen = ${globalGreen.toBoolean()}")
        appendLine("  globalChequered = ${globalChequered.toBoolean()}")
        appendLine("  globalRed = ${globalRed.toBoolean()}")
        appendLine("  mfdTyreSet = $mfdTyreSet")
        appendLine("  mfdFuelToAdd = ${mfdFuelToAdd.fmt(1)} L")
        appendLine("  mfdTyrePressureLF = ${mfdTyrePressureLF.fmt(2)}")
        appendLine("  mfdTyrePressureRF = ${mfdTyrePressureRF.fmt(2)}")
        appendLine("  mfdTyrePressureLR = ${mfdTyrePressureLR.fmt(2)}")
        appendLine("  mfdTyrePressureRR = ${mfdTyrePressureRR.fmt(2)}")
        appendLine("  trackGripStatus = $trackGripStatus")
        appendLine("  rainIntensity = $rainIntensity")
        appendLine("  rainIntensityIn10min = $rainIntensityIn10min")
        appendLine("  rainIntensityIn30min = $rainIntensityIn30min")
        appendLine("  currentTyreSet = $currentTyreSet")
        appendLine("  strategyTyreSet = $strategyTyreSet")
        appendLine("  gapAhead = $gapAhead")
        appendLine("  gapBehind = $gapBehind")
        appendLine("}")
    }
}
