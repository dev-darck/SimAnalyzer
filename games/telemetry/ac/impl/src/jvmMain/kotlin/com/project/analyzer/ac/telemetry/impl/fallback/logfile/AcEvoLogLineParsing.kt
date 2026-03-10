package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.CarSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Parsed
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Penalty
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.SessionTypeSource
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

internal data class CandidateTrack(val id: String? = null, val layout: String? = null)

internal data class AcEvoParsedLinesResult(
    val parsed: Parsed,
    val playerCarUuid: String?,
    val penaltyGroupId: String?,
    val penaltyGroupMs: Long,
)

internal class AcEvoLogParseAccumulator(
    private val includePenalties: Boolean,
    playerCarUuid: String?,
    private val uuidToCarMap: MutableMap<String, String>,
    penaltyGroupId: String?,
    penaltyGroupMs: Long,
) {

    private var currentPlayerCarUuid: String? = playerCarUuid
    private var currentPenaltyGroupId: String? = penaltyGroupId
    private var currentPenaltyGroupMs: Long = penaltyGroupMs

    private var hardBoundary = false
    private var hardBoundaryTs: Long? = null
    private var gameStarted = false
    private var gameStartedTs: Long? = null
    private var mainMenuEntered = false
    private var mainMenuTs: Long? = null
    private var physicsTrackName: String? = null
    private var gameStartedTrackName: String? = null
    private var slugBase: String? = null
    private var slugLayout: String? = null
    private var containerFolder: String? = null
    private var containerLayout: String? = null
    private var dynamicTrackFolder: String? = null
    private var dynamicTrackLayout: String? = null
    private var layoutFileFolder: String? = null
    private var layoutFileLayout: String? = null
    private var bestCar: String? = null
    private var bestCarSource: CarSource = CarSource.NONE
    private var driverName: String? = null
    private var driverSteamId: String? = null
    private var penalty: Penalty? = null
    private var sessionType: EvoSessionType? = null
    private var sessionTypeSource: SessionTypeSource = SessionTypeSource.NONE

    fun parse(lines: List<String>): AcEvoParsedLinesResult {
        lines.forEach(::processLine)
        return AcEvoParsedLinesResult(
            parsed = Parsed(
                hardBoundary = hardBoundary,
                hardBoundaryTimestampMs = hardBoundaryTs,
                gameStarted = gameStarted,
                gameStartedTimestampMs = gameStartedTs,
                mainMenuEntered = mainMenuEntered,
                mainMenuTimestampMs = mainMenuTs,
                physicsTrackName = physicsTrackName,
                gameStartedTrackName = gameStartedTrackName,
                slugBase = slugBase,
                slugLayout = slugLayout,
                containerFolder = containerFolder,
                containerLayout = containerLayout,
                dynamicTrackFolder = dynamicTrackFolder,
                dynamicTrackLayout = dynamicTrackLayout,
                layoutFileFolder = layoutFileFolder,
                layoutFileLayout = layoutFileLayout,
                carModel = bestCar,
                carSource = bestCarSource,
                driverName = driverName,
                driverSteamId = driverSteamId,
                penalty = penalty,
                sessionType = sessionType,
                sessionTypeSource = sessionTypeSource,
            ),
            playerCarUuid = currentPlayerCarUuid,
            penaltyGroupId = currentPenaltyGroupId,
            penaltyGroupMs = currentPenaltyGroupMs,
        )
    }

    private fun processLine(line: String) {
        captureBoundaries(line)
        captureDriverIdentity(line)
        captureSessionType(line)
        captureGameStarted(line)
        captureTrackIdentity(line)
        captureCarModel(line)
        capturePenalty(line)
    }

    private fun captureBoundaries(line: String) {
        if (isHardBoundary(line)) {
            hardBoundary = true
            if (hardBoundaryTs == null) {
                hardBoundaryTs = parseTimestamp(line)?.let(::parseTimestampMs)
            }
        }

        if (isMainMenuTransition(line)) {
            mainMenuEntered = true
            if (mainMenuTs == null) {
                mainMenuTs = parseTimestamp(line)?.let(::parseTimestampMs)
            }
        }
    }

    private fun captureDriverIdentity(line: String) {
        parseConnectingGamecar(line)?.let { (uuid, name, steamId) ->
            if (uuid.isNotBlank()) {
                currentPlayerCarUuid = uuid
            }
            driverName = name
            driverSteamId = steamId
        }

        parseConnectedOnCar(line)?.let { (carModel, uuid) ->
            if (uuid.isNotBlank() && carModel.isNotBlank()) {
                uuidToCarMap[uuid] = carModel
                if (uuid == currentPlayerCarUuid) {
                    updateBestCar(candidate = carModel, source = CarSource.STRONG_CONNECTED)
                }
            }
        }

        parseAiDriverEvo(line)?.let { (carModel, uuid) ->
            if (uuid.isNotBlank() && carModel.isNotBlank()) {
                uuidToCarMap[uuid] = carModel
            }
        }

        if (driverName == null) {
            parseDriverConnection(line)?.let { (name, steamId) ->
                driverName = name
                driverSteamId = steamId
            }
        }
    }

    private fun captureSessionType(line: String) {
        parseRemoteCreatedSessionType(line)?.let { type ->
            updateSessionType(candidate = type, source = SessionTypeSource.REMOTE_CREATED)
        }
        parseSelectedSessionType(line)?.let { type ->
            updateSessionType(candidate = type, source = SessionTypeSource.SELECTED_SESSION)
        }
        parseGotoLoadingPageSessionType(line)?.let { type ->
            updateSessionType(candidate = type, source = SessionTypeSource.GOTO_LOADING_PAGE)
        }
    }

    private fun captureGameStarted(line: String) {
        if (!line.contains("Game Started!", ignoreCase = true)) return

        gameStarted = true
        if (gameStartedTs == null) {
            gameStartedTs = parseTimestamp(line)?.let(::parseTimestampMs)
        }

        extractGameModeType(line)?.let { mode ->
            updateSessionType(
                candidate = EvoSessionType.fromLogString(mode),
                source = SessionTypeSource.GAME_STARTED,
            )
        }

        val parts = line.split("|").map { it.trim() }
        parts.getOrNull(1)?.takeIf(String::isNotBlank)?.let { trackName ->
            gameStartedTrackName = cleanGameStartedTrack(trackName)
        }
        parts.getOrNull(2)?.takeIf(String::isNotBlank)?.let { carId ->
            updateBestCar(candidate = cleanCarId(carId), source = CarSource.STRONG_GAME_STARTED)
        }
    }

    private fun captureTrackIdentity(line: String) {
        extractPhysicsTrackName(line)?.let { physicsTrackName = it }
        parseTrackSlug(line)?.let { (base, layout) ->
            slugBase = base
            slugLayout = layout
        }
        if (containerFolder == null) {
            parseContainerTrack(line)?.let { (folder, layout) ->
                containerFolder = folder
                containerLayout = layout
            }
        }
        if (dynamicTrackFolder == null) {
            parseDynamicTrackPreset(line)?.let { (folder, layout) ->
                dynamicTrackFolder = folder
                dynamicTrackLayout = layout
            }
        }
        if (layoutFileFolder == null) {
            parseLayoutTrackFile(line)?.let { (folder, layout) ->
                layoutFileFolder = folder
                layoutFileLayout = layout
            }
        }
    }

    private fun captureCarModel(line: String) {
        parsePlayerCar(line)?.let { (carId, source) ->
            updateBestCar(candidate = carId, source = source)
        }
    }

    private fun capturePenalty(line: String) {
        if (!includePenalties || penalty != null) return
        parsePenalty(line)?.let { penalty = it }
    }

    private fun updateBestCar(candidate: String, source: CarSource) {
        if (candidate.isBlank() || source.ordinal <= bestCarSource.ordinal) return
        bestCar = candidate
        bestCarSource = source
    }

    private fun updateSessionType(candidate: EvoSessionType, source: SessionTypeSource) {
        if (candidate == EvoSessionType.UNKNOWN) return
        if (source.ordinal < sessionTypeSource.ordinal) return
        sessionType = candidate
        sessionTypeSource = source
    }

    private fun parsePenalty(line: String): Penalty? {
        parsePenaltyKeyId(line)?.let { id ->
            val timestamp = parseTimestamp(line) ?: return null
            updatePenaltyGroup(id = id, tsMs = parseTimestampMs(timestamp))
            return Penalty(id, "Penalty added", timestamp)
        }

        val lower = line.lowercase()
        val isUiPenalty = lower.contains("uinotificationtype_sessionpenalty")
        val isPenaltyType = lower.contains("penalty type") && lower.contains("penaltytype_")
        val isLapInvalid = lower.contains("lap invalid") ||
            lower.contains("invalidating lap") ||
            lower.contains("lap cancelled") ||
            lower.contains("lap deleted")
        if (!isUiPenalty && !isPenaltyType && !isLapInvalid) return null

        val timestamp = parseTimestamp(line) ?: return null
        val timestampMs = parseTimestampMs(timestamp)
        val (baseId, reason) = when {
            isLapInvalid -> "lapInvalid" to "Lap invalidated"
            isUiPenalty -> "uiSessionPenalty" to "Session penalty"
            else -> "penaltyType" to "Penalty"
        }
        return Penalty(
            id = groupPenaltyId(baseId = baseId, tsMs = timestampMs),
            reason = reason,
            timestamp = timestamp,
        )
    }

    private fun parsePenaltyKeyId(line: String): String? {
        val marker = "{PENALTY_ADDED_KEY}"
        val start = line.indexOf(marker, ignoreCase = true)
        if (start < 0) return null
        val hash = line.indexOf('#', startIndex = start + marker.length)
        if (hash < 0) return null
        val digits = line.substring(hash + 1).takeWhile(Char::isDigit)
        return digits.takeIf(String::isNotBlank)?.let { "penalty#$it" }
    }

    private fun groupPenaltyId(baseId: String, tsMs: Long?): String {
        if (tsMs == null) return "$baseId@unknown"

        val withinWindow = currentPenaltyGroupId != null && (tsMs - currentPenaltyGroupMs) in 0..PENALTY_GROUP_WINDOW_MS
        return if (withinWindow) {
            currentPenaltyGroupMs = tsMs
            currentPenaltyGroupId.orEmpty()
        } else {
            val newId = "$baseId@$tsMs"
            updatePenaltyGroup(id = newId, tsMs = tsMs)
            newId
        }
    }

    private fun updatePenaltyGroup(id: String, tsMs: Long?) {
        currentPenaltyGroupId = id
        currentPenaltyGroupMs = tsMs ?: 0L
    }
}

private fun parseConnectingGamecar(line: String): Triple<String, String, String?>? {
    val tail = substringAfterIgnoreCase(line, "connecting gamecar ")?.trimStart() ?: return null
    val uuid = normalizeUuid(tail.takeWhile { !it.isWhitespace() && it != '(' && it != ',' })
    val (name, steamId) = parseDriverConnection(line) ?: return null
    return if (uuid.isNotBlank()) Triple(uuid, name, steamId) else null
}

private fun parseDriverConnection(line: String): Pair<String, String?>? {
    if (!line.contains("connecting gamecar", ignoreCase = true)) return null
    val open = line.indexOf('(')
    val close = line.indexOf(')', startIndex = open + 1)
    if (open < 0 || close <= open + 1) return null
    val inside = line.substring(open + 1, close)
    val separator = inside.lastIndexOf('|')
    val name = if (separator >= 0) inside.substring(0, separator).trim() else inside.trim()
    val steamId = if (separator >= 0) inside.substring(separator + 1).trim().takeIf(String::isNotBlank) else null
    return name.takeIf(String::isNotBlank)?.let { it to steamId }
}

private fun parseConnectedOnCar(line: String): Pair<String, String>? {
    val tail = substringAfterIgnoreCase(line, "connected on car ") ?: return null
    val carModel = cleanCarId(tail.substringBefore(','))
    val uuidTail = substringAfterIgnoreCase(tail, "with new carId ") ?: return null
    val uuid = normalizeUuid(
        uuidTail.takeWhile { !it.isWhitespace() && it != ',' && it != ')' && it != ']' && it != '}' },
    )
    return if (carModel.isNotBlank() && uuid.isNotBlank()) carModel to uuid else null
}

private fun parseAiDriverEvo(line: String): Pair<String, String>? {
    val tail = substringAfterIgnoreCase(line, "Creating AiDriverEvo for car ") ?: return null
    val open = tail.indexOf('(')
    val close = tail.indexOf(')', startIndex = open + 1)
    if (open < 0 || close <= open + 1) return null
    val carModel = cleanCarId(tail.substring(0, open))
    val uuid = normalizeUuid(tail.substring(open + 1, close))
    return if (carModel.isNotBlank() && uuid.isNotBlank()) carModel to uuid else null
}

private fun parseRemoteCreatedSessionType(line: String): EvoSessionType? {
    val createdIdx = line.lastIndexOf(" created", ignoreCase = true)
    if (createdIdx < 0) return null
    val tokens = tokenizeByWhitespace(line.substring(0, createdIdx))
    if (tokens.size < 2) return null
    val remoteToken = tokens[tokens.lastIndex - 1]
    if (!remoteToken.endsWith("Remote", ignoreCase = true)) return null
    return EvoSessionType.fromLogString(tokens.last())
}

private fun parseSelectedSessionType(line: String): EvoSessionType? {
    val tail = substringAfterIgnoreCase(line, "Selected session ") ?: return null
    val tokens = tokenizeByWhitespace(tail)
    if (tokens.size < 3) return null
    val currentToken = tokens[tokens.lastIndex - 1]
    val trailingToken = tokens.last()
    val isCurrentSession = currentToken.equals("true", ignoreCase = true)
    if (!isBooleanToken(currentToken) || !isBooleanToken(trailingToken) || !isCurrentSession) {
        return null
    }
    return EvoSessionType.fromLogString(tokens.subList(0, tokens.size - 2).joinToString(" "))
}

private fun parseGotoLoadingPageSessionType(line: String): EvoSessionType? {
    val type = substringAfterIgnoreCase(line, "goto_loadingpage ")
        ?.takeWhile { !it.isWhitespace() }
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: return null
    return EvoSessionType.fromLogString(type)
}

private fun extractGameModeType(line: String): String? {
    val tail = substringAfterIgnoreCase(line, "GameModeType_") ?: return null
    val identifier = buildString {
        for (char in tail) {
            if (char.isLetterOrDigit() || char == '_') append(char) else break
        }
    }
    return identifier.takeIf(String::isNotBlank)
}

private fun extractPhysicsTrackName(line: String): String? = substringAfterIgnoreCase(line, "Creating physics track:")
    ?.trim()
    ?.takeIf(String::isNotBlank)

private fun parseTrackSlug(line: String): Pair<String, String?>? {
    val tail = substringAfterIgnoreCase(line, "TRACK NAME") ?: return null
    val tokens = tokenizeByWhitespace(tail)
    if (tokens.isEmpty()) return null
    return if (tokens.size >= 2) tokens.dropLast(1).joinToString("_") to tokens.last() else tokens.first() to null
}

private fun parseContainerTrack(line: String): Pair<String, String>? = parseTrackPath(
    line = line,
    folder = "containers",
    fileExtension = ".scene",
    layoutPrefix = "layout_",
    requireLayoutPrefix = true,
)

private fun parseDynamicTrackPreset(line: String): Pair<String, String>? = parseTrackPath(
    line = line,
    folder = "dynamic_track",
    fileExtension = ".dynamictrackpresetcompressed",
)

private fun parseLayoutTrackFile(line: String): Pair<String, String>? = parseTrackPath(
    line = line,
    folder = "layouts",
    fileExtension = ".track_layout",
    layoutPrefix = "layout_",
)

private fun parseTrackPath(
    line: String,
    folder: String,
    fileExtension: String,
    layoutPrefix: String = "",
    requireLayoutPrefix: Boolean = false,
): Pair<String, String>? {
    val tail = substringAfterIgnoreCase(normalizeSlashes(line), "content/tracks/") ?: return null
    val segments = tail.split('/')
    if (segments.size < 3) return null
    if (!segments[1].equals(folder, ignoreCase = true)) return null
    val fileSegment = segments[2]
    val extensionIndex = fileSegment.indexOf(fileExtension, ignoreCase = true)
    if (extensionIndex < 0) return null
    val fileName = fileSegment.substring(0, extensionIndex + fileExtension.length)
    val layoutRaw = fileName.substring(0, fileName.length - fileExtension.length)
    if (requireLayoutPrefix && !layoutRaw.startsWith(layoutPrefix, ignoreCase = true)) return null
    val layout = if (layoutPrefix.isNotEmpty() && layoutRaw.startsWith(layoutPrefix, ignoreCase = true)) {
        layoutRaw.substring(layoutPrefix.length)
    } else {
        layoutRaw
    }
    val track = segments[0].trim()
    return if (track.isNotBlank() && layout.isNotBlank()) track to layout else null
}

private fun parsePlayerCar(line: String): Pair<String, CarSource>? {
    if (line.contains("onSetPlayerCurrentCarCommand:", ignoreCase = true)) {
        extractContentCarId(line)?.let { return it to CarSource.STRONGEST_PLAYER_COMMAND }
    }
    if (line.contains("Driver ", ignoreCase = true) && line.contains(" on car ", ignoreCase = true)) {
        val carId = cleanCarId(substringAfterIgnoreCase(line, " on car ").orEmpty())
        if (carId.isNotBlank()) {
            return carId to CarSource.STRONGEST_DRIVER_ON_CAR
        }
    }
    substringAfterIgnoreCase(line, "my car:")?.let {
        val carId = cleanCarId(it)
        if (carId.isNotBlank()) {
            return carId to CarSource.STRONGEST_MY_CAR
        }
    }
    substringAfterIgnoreCase(line, "CarDisplay.init:")?.let {
        val carId = cleanCarId(it)
        if (carId.isNotBlank()) {
            return carId to CarSource.MEDIUM_CAR_DISPLAY
        }
    }
    return null
}

private fun extractContentCarId(line: String): String? {
    val tail = substringAfterIgnoreCase(normalizeSlashes(line), "content/cars/") ?: return null
    val carId = cleanCarId(tail.substringBefore('/'))
    return carId.takeIf(String::isNotBlank)
}

private fun normalizeUuid(raw: String): String = raw.replace("-", "").lowercase().trim()

private fun cleanCarId(raw: String): String = raw.trim()
    .substringBefore(',')
    .substringBefore(')')
    .substringBefore(']')
    .substringBefore('}')
    .substringBefore(' ')
    .trim()

internal fun mapContainerLayout(raw: String): String = when (normalizeToken(raw)) {
    "gp_circuit" -> "gp"
    "gp_circuit_shortcut", "gp_circuit_short", "gp_shortcut" -> "gp_short"
    else -> raw
}

private fun normalizeToken(raw: String): String = raw.lowercase().trim()
    .replace(RE_ONE_OR_MORE_WHITESPACE, "_")
    .replace(RE_NON_ALNUM_UNDERSCORE, "_")
    .replace(RE_ONE_OR_MORE_UNDERSCORES, "_")
    .trim('_')

private fun cleanGameStartedTrack(raw: String): String {
    val noDate = raw.substringBefore("@").trim()
    val lower = noDate.lowercase()
    val cuts = listOf(" time attack", " practice", " qualifying", " race", " hotlap", " warmup")
    val cutIdx = cuts.mapNotNull { lower.indexOf(it).takeIf { index -> index >= 0 } }.minOrNull()
    return (if (cutIdx != null) noDate.substring(0, cutIdx) else noDate)
        .replace(RE_ONE_OR_MORE_WHITESPACE, " ")
        .trim()
}

private fun isMainMenuTransition(line: String): Boolean {
    val normalized = normalizeSlashes(line).lowercase()
    return normalized.contains("goto menu.html,main/main") ||
        normalized.contains("loading page menu.html main/main") ||
        normalized.contains("init: menustate updated /menu.html main main")
}

private fun isHardBoundary(line: String): Boolean {
    val lower = line.lowercase()
    if (lower.contains("reset session")) return true
    if (lower.contains("restart session")) return true
    if (lower.contains("session restart")) return true
    if (lower.contains("end_session")) {
        return lower.contains("all cars pitted")
    }
    if (lower.contains("session ended")) {
        return lower.contains("all cars pitted") || lower.contains("all cars")
    }
    return false
}

private fun parseTimestamp(line: String): String? {
    val trimmed = line.trimStart()
    if (!trimmed.startsWith('[')) return null
    val end = trimmed.indexOf(']')
    if (end <= 1) return null
    return trimmed.substring(1, end).trim().takeIf(String::isNotBlank)
}

private fun parseTimestampMs(timestamp: String): Long? = runCatching {
    LocalDateTime.parse(timestamp, TS_FORMAT)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}.getOrNull()

private fun substringAfterIgnoreCase(value: String, marker: String): String? {
    val index = value.indexOf(marker, ignoreCase = true)
    return if (index >= 0) value.substring(index + marker.length) else null
}

private fun tokenizeByWhitespace(value: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    for (char in value) {
        if (char.isWhitespace()) {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.setLength(0)
            }
        } else {
            current.append(char)
        }
    }
    if (current.isNotEmpty()) {
        tokens += current.toString()
    }
    return tokens
}

private fun isBooleanToken(value: String): Boolean =
    value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)

private fun normalizeSlashes(value: String): String = value.replace('\\', '/')

private val PENALTY_GROUP_WINDOW_MS = 3.seconds.inWholeMilliseconds

private val TS_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd HH:mm:ss")
    .optionalStart()
    .appendLiteral('.')
    .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false)
    .optionalEnd()
    .toFormatter(Locale.US)

private val RE_ONE_OR_MORE_WHITESPACE = Regex("\\s+")
private val RE_NON_ALNUM_UNDERSCORE = Regex("[^a-z0-9_]")
private val RE_ONE_OR_MORE_UNDERSCORES = Regex("_+")
