package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.CarSource
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Parsed
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Penalty
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_AI_DRIVER_EVO
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_CAR_DISPLAY
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_CONNECTED_ON_CAR_WITH_UUID
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_CONNECTING_GAMECAR
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_CONTAINER
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_DRIVER
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_DRIVER_ON_CAR
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_DYNAMIC_TRACK_PRESET
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_GAME_MODE_TYPE
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_LAYOUT_TRACK_FILE
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_MY_CAR
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PENALTY_KEY
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PHYSICS_TRACK
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PLAYER_COMMAND
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_SESSION_TYPE
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_TIMESTAMP
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_TRACK_SLUG
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.ResolvedTrack
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
    private val uuidToCarMap = mutableMapOf<String, String>()

    val currentPlayerCarUuid: String?
        get() = playerCarUuid

    fun resetForEpoch() {
        trackIdSource = TrackIdSource.NONE
        carSource = CarSource.NONE
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

        for (line in lines) {
            if (isHardBoundary(line)) {
                hardBoundary = true
                if (hardBoundaryTs == null) {
                    hardBoundaryTs = parseTimestamp(line)?.let(::parseTimestampMs)
                }
            }

            RE_CONNECTING_GAMECAR.find(line)?.let { m ->
                val uuid = normalizeUuid(m.groupValues[1])
                if (uuid.isNotBlank()) {
                    playerCarUuid = uuid
                }
                driverName = m.groupValues[2].trim()
                m.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.let {
                    driverSteamId = it
                }
            }

            RE_CONNECTED_ON_CAR_WITH_UUID.find(line)?.let { m ->
                val carModel = cleanCarId(m.groupValues[1])
                val uuid = normalizeUuid(m.groupValues[2])
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

            RE_AI_DRIVER_EVO.find(line)?.let { m ->
                val carModel = cleanCarId(m.groupValues[1])
                val uuid = normalizeUuid(m.groupValues[2])
                if (uuid.isNotBlank() && carModel.isNotBlank()) {
                    uuidToCarMap[uuid] = carModel
                }
            }

            RE_SESSION_TYPE.find(line)?.let { m ->
                val typeStr = m.groupValues[2]
                sessionType = EvoSessionType.fromLogString(typeStr)
            }

            if (line.contains("Game Started!", ignoreCase = true)) {
                gameStarted = true
                if (gameStartedTs == null) {
                    gameStartedTs = parseTimestamp(line)?.let(::parseTimestampMs)
                }

                RE_GAME_MODE_TYPE.find(line)?.groupValues?.getOrNull(1)?.let {
                    sessionType = EvoSessionType.fromLogString(it)
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

            RE_PHYSICS_TRACK.find(line)?.let {
                physicsTrackName = it.groupValues[1].trim()
            }

            RE_TRACK_SLUG.find(line)?.let { m ->
                val tokens = m.groupValues[1].split(Regex("\\s+")).filter { it.isNotBlank() }
                when {
                    tokens.size >= 2 -> {
                        slugBase = tokens.dropLast(1).joinToString("_")
                        slugLayout = tokens.last()
                    }

                    tokens.size == 1 -> {
                        slugBase = tokens.first()
                        slugLayout = null
                    }
                }
            }

            if (containerFolder == null) {
                RE_CONTAINER.find(line)?.let {
                    containerFolder = it.groupValues[1]
                    containerLayout = it.groupValues[2]
                }
            }

            if (dynamicTrackFolder == null) {
                RE_DYNAMIC_TRACK_PRESET.find(line)?.let {
                    dynamicTrackFolder = it.groupValues[1]
                    dynamicTrackLayout = it.groupValues[2]
                }
            }

            if (layoutFileFolder == null) {
                RE_LAYOUT_TRACK_FILE.find(line)?.let {
                    layoutFileFolder = it.groupValues[1]
                    layoutFileLayout = it.groupValues[2]
                }
            }

            parsePlayerCar(line)?.let { (id, src) ->
                if (src.ordinal > bestCarSource.ordinal) {
                    bestCar = id
                    bestCarSource = src
                }
            }

            if (driverName == null) {
                RE_DRIVER.find(line)?.let {
                    driverName = it.groupValues[1].trim()
                    driverSteamId = it.groupValues[2].takeIf { s -> s.isNotBlank() }
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
            sessionType = sessionType
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
            fallbackLayout = layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile
        )
        val containerCandidate = buildTrackCandidate(
            base = baseFromContainer ?: baseFromDynamic,
            preferredLayout = layoutFromContainer ?: layoutFromDynamic,
            fallbackLayout = layoutFromSlug ?: layoutFromLayoutFile
        )
        val layoutFileCandidate = buildTrackCandidate(
            base = baseFromLayoutFile,
            preferredLayout = layoutFromLayoutFile,
            fallbackLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic
        )
        val gameStartedCandidate = buildTrackCandidate(
            base = baseFromGameStarted,
            preferredLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile
        )
        val physicsCandidate = buildTrackCandidate(
            base = baseFromPhysics,
            preferredLayout = layoutFromSlug ?: layoutFromContainer ?: layoutFromDynamic ?: layoutFromLayoutFile
        )

        val (candidateId, candidateLayout, candidateSource) = when {
            slugCandidate.id != null -> Triple(slugCandidate.id, slugCandidate.layout, TrackIdSource.SLUG)
            containerCandidate.id != null -> Triple(
                containerCandidate.id,
                containerCandidate.layout,
                TrackIdSource.CONTAINER
            )

            layoutFileCandidate.id != null -> Triple(
                layoutFileCandidate.id,
                layoutFileCandidate.layout,
                TrackIdSource.CONTAINER
            )

            gameStartedCandidate.id != null -> Triple(
                gameStartedCandidate.id,
                gameStartedCandidate.layout,
                TrackIdSource.GAME_STARTED
            )

            physicsCandidate.id != null -> Triple(physicsCandidate.id, physicsCandidate.layout, TrackIdSource.NONE)
            else -> Triple(null, null, TrackIdSource.NONE)
        }

        val stableId = lastInfo.trackId?.takeIf { it.isNotBlank() }
        val stableSource = trackIdSource
        val stableLayout = lastInfo.layoutId?.takeIf { it.isNotBlank() }

        if (stableId != null && stableLayout != null && candidateLayout.isNullOrBlank()) {
            return ResolvedTrack(
                trackId = stableId,
                layout = stableLayout,
                source = trackIdSource
            )
        }

        val effectiveId = when {
            candidateId.isNullOrBlank() -> stableId
            stableId.isNullOrBlank() -> candidateId
            candidateId == stableId -> stableId
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
            source = effectiveSource
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
        fallbackLayout: String? = null
    ): CandidateTrack {
        val normalizedBase = base?.takeIf { it.isNotBlank() } ?: return CandidateTrack()
        val effectiveLayout = preferredLayout?.takeIf { it.isNotBlank() } ?: fallbackLayout?.takeIf { it.isNotBlank() }
        val normalizedId = TrackIdNormalizer.normalize(track = normalizedBase, layout = effectiveLayout)
            .takeIf { it.isNotBlank() } ?: return CandidateTrack()
        return CandidateTrack(
            id = normalizedId,
            layout = effectiveLayout
        )
    }

    private fun parsePlayerCar(line: String): Pair<String, CarSource>? {
        RE_PLAYER_COMMAND.find(line)?.let { m ->
            val carId = cleanCarId(m.groupValues[1])
            return carId to CarSource.STRONGEST_PLAYER_COMMAND
        }

        RE_DRIVER_ON_CAR.find(line)?.let { m ->
            val carId = cleanCarId(m.groupValues[1])
            return carId to CarSource.STRONGEST_DRIVER_ON_CAR
        }

        RE_MY_CAR.find(line)?.let { m ->
            val carId = cleanCarId(m.groupValues[1])
            return carId to CarSource.STRONGEST_MY_CAR
        }

        RE_CAR_DISPLAY.find(line)?.let { m ->
            val carId = cleanCarId(m.groupValues[1])
            return carId to CarSource.MEDIUM_CAR_DISPLAY
        }

        return null
    }

    private fun normalizeUuid(raw: String): String {
        return raw.replace("-", "").lowercase().trim()
    }

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

    private fun mapContainerLayout(raw: String): String {
        return when (normalizeToken(raw)) {
            "gp_circuit" -> "gp"
            "gp_circuit_shortcut", "gp_circuit_short", "gp_shortcut" -> "gp_short"
            else -> raw
        }
    }

    private fun normalizeToken(raw: String): String =
        raw.lowercase().trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

    private fun cleanGameStartedTrack(raw: String): String {
        val noDate = raw.substringBefore("@").trim()
        val lower = noDate.lowercase()
        val cuts = listOf(" time attack", " practice", " qualifying", " race", " hotlap", " warmup")
        val cutIdx = cuts.mapNotNull { lower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull()
        return (if (cutIdx != null) noDate.substring(0, cutIdx) else noDate)
            .replace(Regex("\\s+"), " ").trim()
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
        RE_PENALTY_KEY.find(line)?.let { m ->
            val ts = parseTimestamp(line) ?: return null
            val id = "penalty#${m.groupValues[1]}"
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

    private fun parseTimestamp(line: String): String? =
        RE_TIMESTAMP.find(line)?.groupValues?.get(1)

    private fun parseTimestampMs(ts: String): Long? = runCatching {
        LocalDateTime.parse(ts, TS_FORMAT)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()

    private companion object {
        data class CandidateTrack(
            val id: String? = null,
            val layout: String? = null
        )

        val PENALTY_GROUP_WINDOW_MS = 3.seconds.inWholeMilliseconds

        val TS_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendLiteral('.')
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false)
            .optionalEnd()
            .toFormatter(Locale.US)
    }
}
