package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.CarSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Parsed
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Penalty
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.ResolvedTrack
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.SessionTypeSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.TrackIdSource
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

internal class AcEvoLogParser {

    private var trackIdSource: TrackIdSource = TrackIdSource.NONE
    private var carSource: CarSource = CarSource.NONE
    private var penaltyGroupId: String? = null
    private var penaltyGroupMs: Long = 0L
    private var playerCarUuid: String? = null
    private var sessionTypeSource: SessionTypeSource = SessionTypeSource.NONE
    private val uuidToCarMap = mutableMapOf<String, String>()

    val currentPlayerCarUuid: String?
        get() = playerCarUuid

    fun resetForEpoch() {
        trackIdSource = TrackIdSource.NONE
        carSource = CarSource.NONE
        sessionTypeSource = SessionTypeSource.NONE
        clearPenaltyGroup()
    }

    fun clearPenaltyGroup() {
        penaltyGroupId = null
        penaltyGroupMs = 0L
    }

    fun clear() {
        resetForEpoch()
        playerCarUuid = null
        uuidToCarMap.clear()
    }

    fun parseLines(lines: List<String>, includePenalties: Boolean = true): Parsed {
        var hardBoundary = false
        var hardBoundaryTs: Long? = null
        var gameStarted = false
        var gameStartedTs: Long? = null
        var mainMenuEntered = false
        var mainMenuTs: Long? = null
        var physicsTrackName: String? = null
        var gameStartedTrackName: String? = null
        var slugBase: String? = null
        var slugLayout: String? = null
        var containerFolder: String? = null
        var containerLayout: String? = null
        var dynamicTrackFolder: String? = null
        var dynamicTrackLayout: String? = null
        var layoutFileFolder: String? = null
        var layoutFileLayout: String? = null

        var bestCar: String? = null
        var bestCarSource: CarSource = CarSource.NONE

        var driverName: String? = null
        var driverSteamId: String? = null
        var penalty: Penalty? = null
        var sessionType: EvoSessionType? = null
        var sessionTypeSource: SessionTypeSource = SessionTypeSource.NONE

        fun updateSessionType(candidate: EvoSessionType, source: SessionTypeSource) {
            if (candidate == EvoSessionType.UNKNOWN) return
            if (source.ordinal < sessionTypeSource.ordinal) return
            sessionType = candidate
            sessionTypeSource = source
        }

        for (line in lines) {
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

            parseConnectingGamecar(line)?.let { (uuid, name, steamId) ->
                if (uuid.isNotBlank()) {
                    playerCarUuid = uuid
                }
                driverName = name
                driverSteamId = steamId
            }

            parseConnectedOnCar(line)?.let { (carModel, uuid) ->
                if (uuid.isNotBlank() && carModel.isNotBlank()) {
                    uuidToCarMap[uuid] = carModel
                    if (uuid == playerCarUuid) {
                        if (CarSource.STRONG_CONNECTED.ordinal > bestCarSource.ordinal) {
                            bestCar = carModel
                            bestCarSource = CarSource.STRONG_CONNECTED
                        }
                    }
                }
            }

            parseAiDriverEvo(line)?.let { (carModel, uuid) ->
                if (uuid.isNotBlank() && carModel.isNotBlank()) {
                    uuidToCarMap[uuid] = carModel
                }
            }

            parseRemoteCreatedSessionType(line)?.let { typeStr ->
                updateSessionType(
                    candidate = typeStr,
                    source = SessionTypeSource.REMOTE_CREATED,
                )
            }

            parseSelectedSessionType(line)?.let { type ->
                updateSessionType(
                    candidate = type,
                    source = SessionTypeSource.SELECTED_SESSION,
                )
            }

            parseGotoLoadingPageSessionType(line)?.let { type ->
                updateSessionType(
                    candidate = type,
                    source = SessionTypeSource.GOTO_LOADING_PAGE,
                )
            }

            if (line.contains("Game Started!", ignoreCase = true)) {
                gameStarted = true
                if (gameStartedTs == null) {
                    gameStartedTs = parseTimestamp(line)?.let(::parseTimestampMs)
                }

                extractGameModeType(line)?.let {
                    updateSessionType(
                        candidate = EvoSessionType.fromLogString(it),
                        source = SessionTypeSource.GAME_STARTED,
                    )
                }

                val parts = line.split("|").map { it.trim() }
                parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
                    gameStartedTrackName = cleanGameStartedTrack(it)
                }
                parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.let {
                    val id = cleanCarId(it)
                    if (CarSource.STRONG_GAME_STARTED.ordinal > bestCarSource.ordinal) {
                        bestCar = id
                        bestCarSource = CarSource.STRONG_GAME_STARTED
                    }
                }
            }

            extractPhysicsTrackName(line)?.let {
                physicsTrackName = it
            }

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

            parsePlayerCar(line)?.let { (id, src) ->
                if (src.ordinal > bestCarSource.ordinal) {
                    bestCar = id
                    bestCarSource = src
                }
            }

            if (driverName == null) {
                parseDriverConnection(line)?.let { (name, steamId) ->
                    driverName = name
                    driverSteamId = steamId
                }
            }

            if (includePenalties && penalty == null) {
                parsePenalty(line)?.let { penalty = it }
            }
        }

        return Parsed(
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
        )
    }

    fun chooseCarModel(p: Parsed, lastInfo: EvoFileInfo): String? {
        val candidate = p.carModel?.takeIf { it.isNotBlank() } ?: return null

        val alreadySet = !lastInfo.carModel.isNullOrBlank()

        if (p.carSource.ordinal > carSource.ordinal) {
            carSource = p.carSource
            return candidate
        }

        if (!alreadySet) {
            carSource = maxOf(carSource, p.carSource)
            return candidate
        }

        return null
    }

    fun chooseSessionType(p: Parsed, lastInfo: EvoFileInfo): EvoSessionType? {
        val candidate = p.sessionType ?: return null
        val candidateSource = p.sessionTypeSource
        val alreadySet = lastInfo.sessionType != EvoSessionType.UNKNOWN

        if (candidateSource.ordinal > sessionTypeSource.ordinal) {
            sessionTypeSource = candidateSource
            return candidate
        }

        if (!alreadySet || candidateSource == sessionTypeSource) {
            sessionTypeSource = maxOf(sessionTypeSource, candidateSource)
            return candidate
        }

        return null
    }

    fun resolveTrackId(p: Parsed, lastInfo: EvoFileInfo): ResolvedTrack {
        val baseFromSlug = p.slugBase?.takeIf { it.isNotBlank() }
        val baseFromContainer = p.containerFolder?.takeIf { it.isNotBlank() }
        val baseFromDynamic = p.dynamicTrackFolder?.takeIf { it.isNotBlank() }
        val baseFromLayoutFile = p.layoutFileFolder?.takeIf { it.isNotBlank() }
        val baseFromGameStarted = p.gameStartedTrackName?.takeIf { it.isNotBlank() }
        val baseFromPhysics = p.physicsTrackName?.takeIf { it.isNotBlank() }

        val layoutFromSlug = p.slugLayout?.takeIf { it.isNotBlank() }
        val layoutFromContainer = p.containerLayout?.takeIf { it.isNotBlank() }?.let(::mapContainerLayout)
        val layoutFromDynamic = p.dynamicTrackLayout?.takeIf { it.isNotBlank() }?.let(::mapContainerLayout)
        val layoutFromLayoutFile = p.layoutFileLayout?.takeIf { it.isNotBlank() }?.let(::mapContainerLayout)

        val slugCandidate = buildTrackCandidate(
            base = baseFromSlug,
            preferredLayout = layoutFromSlug,
            fallbackLayout = layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile,
        )
        val containerCandidate = buildTrackCandidate(
            base = baseFromContainer ?: baseFromDynamic,
            preferredLayout = layoutFromContainer ?: layoutFromDynamic,
            fallbackLayout = layoutFromSlug ?: layoutFromLayoutFile,
        )
        val layoutFileCandidate = buildTrackCandidate(
            base = baseFromLayoutFile,
            preferredLayout = layoutFromLayoutFile,
            fallbackLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic,
        )
        val gameStartedCandidate = buildTrackCandidate(
            base = baseFromGameStarted,
            preferredLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile,
        )
        val physicsCandidate = buildTrackCandidate(
            base = baseFromPhysics,
            preferredLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile,
        )

        val (candidateId, candidateLayout, candidateSource) = when {
            slugCandidate.id != null -> Triple(slugCandidate.id, slugCandidate.layout, TrackIdSource.SLUG)

            containerCandidate.id != null -> Triple(
                containerCandidate.id,
                containerCandidate.layout,
                TrackIdSource.CONTAINER,
            )

            layoutFileCandidate.id != null -> Triple(
                layoutFileCandidate.id,
                layoutFileCandidate.layout,
                TrackIdSource.CONTAINER,
            )

            gameStartedCandidate.id != null -> Triple(
                gameStartedCandidate.id,
                gameStartedCandidate.layout,
                TrackIdSource.GAME_STARTED,
            )

            physicsCandidate.id != null -> Triple(physicsCandidate.id, physicsCandidate.layout, TrackIdSource.NONE)

            else -> Triple(null, null, TrackIdSource.NONE)
        }

        val stableId = lastInfo.trackId?.takeIf { it.isNotBlank() }
        val stableSource = trackIdSource
        val stableLayout = lastInfo.layoutId?.takeIf { it.isNotBlank() }
        val isPrecisionDowngrade = stableId != null &&
            candidateId != null &&
            isTrackPrecisionDowngrade(stableId = stableId, candidateId = candidateId)

        if (stableId != null && stableLayout != null && candidateLayout.isNullOrBlank()) {
            return ResolvedTrack(
                trackId = stableId,
                layout = stableLayout,
                source = trackIdSource,
            )
        }

        val effectiveId = when {
            candidateId.isNullOrBlank() -> stableId
            stableId.isNullOrBlank() -> candidateId
            candidateId == stableId -> stableId
            isPrecisionDowngrade -> stableId
            candidateSource.ordinal < stableSource.ordinal -> stableId
            else -> candidateId
        }

        val effectiveSource = if (effectiveId == stableId) {
            maxOf(stableSource, candidateSource)
        } else {
            candidateSource
        }

        trackIdSource = effectiveSource

        return ResolvedTrack(
            trackId = effectiveId,
            layout = if (effectiveId == candidateId) candidateLayout else lastInfo.layoutId,
            source = effectiveSource,
        )
    }

    fun buildDisplayName(physicsName: String?, layout: String?): String? {
        val n = physicsName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val l = layout?.trim()?.takeIf { it.isNotBlank() } ?: return n
        return "$n ${l.uppercase()}"
    }

    private fun buildTrackCandidate(
        base: String?,
        preferredLayout: String?,
        fallbackLayout: String? = null,
    ): CandidateTrack {
        val normalizedBase = base?.takeIf { it.isNotBlank() } ?: return CandidateTrack()
        val effectiveLayout = preferredLayout?.takeIf { it.isNotBlank() } ?: fallbackLayout?.takeIf { it.isNotBlank() }
        val normalizedId = TrackIdNormalizer.normalize(track = normalizedBase, layout = effectiveLayout)
            .takeIf { it.isNotBlank() } ?: return CandidateTrack()
        return CandidateTrack(
            id = normalizedId,
            layout = effectiveLayout,
        )
    }

    private fun isTrackPrecisionDowngrade(stableId: String, candidateId: String): Boolean {
        if (stableId == candidateId) return false

        val stableLayout = stableId.substringAfterLast('_', missingDelimiterValue = "")
        val candidateLayout = candidateId.substringAfterLast('_', missingDelimiterValue = "")
        if (stableLayout.isBlank() || candidateLayout.isBlank() || stableLayout != candidateLayout) {
            return false
        }

        val stableBase = stableId.removeSuffix("_$stableLayout")
        val candidateBase = candidateId.removeSuffix("_$candidateLayout")
        if (stableBase.isBlank() || candidateBase.isBlank()) return false

        return stableBase == candidateBase || stableBase.startsWith("${candidateBase}_")
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
        val steamId = if (separator >= 0) inside.substring(separator + 1).trim().takeIf { it.isNotBlank() } else null
        return name.takeIf { it.isNotBlank() }?.let { it to steamId }
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
        val type = tokens.subList(0, tokens.size - 2).joinToString(" ")
        return EvoSessionType.fromLogString(type)
    }

    private fun parseGotoLoadingPageSessionType(line: String): EvoSessionType? {
        val type = substringAfterIgnoreCase(line, "goto_loadingpage ")
            ?.takeWhile { !it.isWhitespace() }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        return EvoSessionType.fromLogString(type)
    }

    private fun extractGameModeType(line: String): String? = extractIdentifierAfter(line, "GameModeType_")

    private fun extractPhysicsTrackName(line: String): String? =
        substringAfterIgnoreCase(line, "Creating physics track:")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun parseTrackSlug(line: String): Pair<String, String?>? {
        val tail = substringAfterIgnoreCase(line, "TRACK NAME") ?: return null
        val tokens = tokenizeByWhitespace(tail)
        if (tokens.isEmpty()) return null
        return when {
            tokens.size >= 2 -> tokens.dropLast(1).joinToString("_") to tokens.last()
            else -> tokens.first() to null
        }
    }

    private fun parseContainerTrack(line: String): Pair<String, String>? = parseTrackPath(
        line = line,
        folder = "containers",
        fileExtension = ".scene",
        layoutPrefix = "layout_",
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
    ): Pair<String, String>? {
        val tail = substringAfterIgnoreCase(normalizeSlashes(line), "content/tracks/") ?: return null
        val segments = tail.split('/')
        if (segments.size < 3) return null
        if (!segments[1].equals(folder, ignoreCase = true)) return null
        val fileName = segments[2]
        if (!fileName.endsWith(fileExtension, ignoreCase = true)) return null
        val layoutRaw = fileName.substring(0, fileName.length - fileExtension.length)
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
            val carId = extractContentCarId(line)
            if (!carId.isNullOrBlank()) {
                return carId to CarSource.STRONGEST_PLAYER_COMMAND
            }
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
        return carId.takeIf { it.isNotBlank() }
    }

    private fun normalizeUuid(raw: String): String = raw.replace("-", "").lowercase().trim()

    private fun cleanCarId(raw: String): String {
        val s = raw.trim()
            .substringBefore(',')
            .substringBefore(')')
            .substringBefore(']')
            .substringBefore('}')
            .substringBefore(' ')
            .trim()
        return s
    }

    private fun mapContainerLayout(raw: String): String = when (normalizeToken(raw)) {
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
        val cutIdx = cuts.mapNotNull { lower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull()
        return (if (cutIdx != null) noDate.substring(0, cutIdx) else noDate)
            .replace(RE_ONE_OR_MORE_WHITESPACE, " ").trim()
    }

    private fun isMainMenuTransition(line: String): Boolean {
        val normalized = normalizeSlashes(line).lowercase()
        return normalized.contains("goto menu.html,main/main") ||
            normalized.contains("loading page menu.html main/main") ||
            normalized.contains("init: menustate updated /menu.html main main")
    }

    private fun isHardBoundary(s: String): Boolean {
        val lower = s.lowercase()

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

    private fun parsePenalty(line: String): Penalty? {
        parsePenaltyKeyId(line)?.let { id ->
            val ts = parseTimestamp(line) ?: return null
            updatePenaltyGroup(id, parseTimestampMs(ts))
            return Penalty(id, "Penalty added", ts)
        }

        val lower = line.lowercase()
        val isUiPenalty = lower.contains("uinotificationtype_sessionpenalty")
        val isPenaltyType = lower.contains("penalty type") && lower.contains("penaltytype_")
        val isLapInvalid = lower.contains("lap invalid") ||
            lower.contains("invalidating lap") ||
            lower.contains("lap cancelled") ||
            lower.contains("lap deleted")

        if (!isUiPenalty && !isPenaltyType && !isLapInvalid) return null

        val ts = parseTimestamp(line) ?: return null
        val tsMs = parseTimestampMs(ts)

        val (baseId, reason) = when {
            isLapInvalid -> "lapInvalid" to "Lap invalidated"
            isUiPenalty -> "uiSessionPenalty" to "Session penalty"
            else -> "penaltyType" to "Penalty"
        }

        val groupedId = groupPenaltyId(baseId, tsMs)
        return Penalty(groupedId, reason, ts)
    }

    private fun parsePenaltyKeyId(line: String): String? {
        val marker = "{PENALTY_ADDED_KEY}"
        val start = line.indexOf(marker, ignoreCase = true)
        if (start < 0) return null
        val hash = line.indexOf('#', startIndex = start + marker.length)
        if (hash < 0) return null
        val digits = line.substring(hash + 1).takeWhile { it.isDigit() }
        return digits.takeIf { it.isNotBlank() }?.let { "penalty#$it" }
    }

    private fun groupPenaltyId(baseId: String, tsMs: Long?): String {
        if (tsMs == null) return "$baseId@unknown"

        val currentId = penaltyGroupId
        val withinWindow = currentId != null && (tsMs - penaltyGroupMs) in 0..PENALTY_GROUP_WINDOW_MS

        return if (withinWindow) {
            penaltyGroupMs = tsMs
            currentId
        } else {
            val newId = "$baseId@$tsMs"
            updatePenaltyGroup(newId, tsMs)
            newId
        }
    }

    private fun updatePenaltyGroup(id: String, tsMs: Long?) {
        penaltyGroupId = id
        penaltyGroupMs = tsMs ?: 0L
    }

    private fun parseTimestamp(line: String): String? {
        val trimmed = line.trimStart()
        if (!trimmed.startsWith('[')) return null
        val end = trimmed.indexOf(']')
        if (end <= 1) return null
        return trimmed.substring(1, end).trim().takeIf { it.isNotBlank() }
    }

    private fun parseTimestampMs(ts: String): Long? = runCatching {
        LocalDateTime.parse(ts, TS_FORMAT)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private fun substringAfterIgnoreCase(value: String, marker: String): String? {
        val idx = value.indexOf(marker, ignoreCase = true)
        return if (idx >= 0) value.substring(idx + marker.length) else null
    }

    private fun extractIdentifierAfter(value: String, marker: String): String? {
        val tail = substringAfterIgnoreCase(value, marker) ?: return null
        val identifier = buildString {
            for (ch in tail) {
                if (ch.isLetterOrDigit() || ch == '_') append(ch) else break
            }
        }
        return identifier.takeIf { it.isNotBlank() }
    }

    private fun tokenizeByWhitespace(value: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        for (ch in value) {
            if (ch.isWhitespace()) {
                if (current.isNotEmpty()) {
                    tokens += current.toString()
                    current.setLength(0)
                }
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            tokens += current.toString()
        }
        return tokens
    }

    private fun isBooleanToken(value: String): Boolean = value.equals("true", true) || value.equals("false", true)

    private fun normalizeSlashes(value: String): String = value.replace('\\', '/')

    private companion object {
        data class CandidateTrack(val id: String? = null, val layout: String? = null)

        val PENALTY_GROUP_WINDOW_MS = 3.seconds.inWholeMilliseconds

        val TS_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendLiteral('.')
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false)
            .optionalEnd()
            .toFormatter(Locale.US)

        val RE_ONE_OR_MORE_WHITESPACE = Regex("\\s+")
        val RE_NON_ALNUM_UNDERSCORE = Regex("[^a-z0-9_]")
        val RE_ONE_OR_MORE_UNDERSCORES = Regex("_+")
    }
}
