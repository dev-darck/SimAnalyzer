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
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_MY_CAR
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PENALTY_KEY
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PHYSICS_TRACK
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_PLAYER_COMMAND
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_SESSION_TYPE
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_TIMESTAMP
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.RegexConst.RE_TRACK_SLUG
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.ResolvedTrack
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.TrackIdSource
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.min

@Inject
@SingleIn(SessionScope::class)
class AcEvoFileInfoExtractor(
    private val locator: AcEvoLogLocator,
) : EvoFileInfoSource {

    private var raf: RandomAccessFile? = null
    private var openedFile: File? = null
    private var openedFileKey: Any? = null
    private var openedLastModified: Long = 0L
    private var lastPos: Long = 0L
    private var pending: String = ""
    private var sessionEpoch: Long = 0L
    private var lastInfo: EvoFileInfo = EvoFileInfo()
    private var trackIdSource: TrackIdSource = TrackIdSource.NONE
    private var carSource: CarSource = CarSource.NONE
    private var penaltyGroupId: String? = null
    private var penaltyGroupMs: Long = 0L
    private var lastGameStartedBumpMs: Long = 0L
    private var lastHardBoundaryBumpMs: Long = 0L
    private var playerCarUuid: String? = null
    private val uuidToCarMap = mutableMapOf<String, String>()

    override fun poll(): EvoFileInfo {
        val file = locator.locateLogFile() ?: return lastInfo

        ensureOpenOrReopenIfRotated(file)

        val lines = readNewLines(file)
        if (lines.isEmpty()) return lastInfo

        val p = parseLines(lines)

        val nowMs = System.currentTimeMillis()

        if (p.hardBoundary) {
            if (nowMs - lastHardBoundaryBumpMs > 1500L) {
                lastHardBoundaryBumpMs = nowMs
                bumpEpoch("hardBoundary")
            }
        }

        if (p.gameStarted) {
            if (nowMs - lastGameStartedBumpMs > 3000L) {
                lastGameStartedBumpMs = nowMs
                bumpEpoch("gameStarted")
            }
        }

        val resolved = resolveTrackId(p)

        val trackName = buildDisplayName(p.physicsTrackName, resolved.layout)
            ?: p.gameStartedTrackName
            ?: lastInfo.trackName
            ?: resolved.trackId

        val newCar = chooseCarModel(p)

        val effectiveSessionType = p.sessionType ?: lastInfo.sessionType

        lastInfo = lastInfo.copy(
            trackName = trackName,
            trackId = resolved.trackId ?: lastInfo.trackId,
            layoutId = resolved.layout ?: lastInfo.layoutId,
            carModel = newCar ?: lastInfo.carModel,
            driverName = p.driverName ?: lastInfo.driverName,
            driverSteamId = p.driverSteamId ?: lastInfo.driverSteamId,
            hasPenalty = p.penalty != null || lastInfo.hasPenalty,
            penaltyId = p.penalty?.id ?: lastInfo.penaltyId,
            penaltyReason = p.penalty?.reason ?: lastInfo.penaltyReason,
            penaltyTimestamp = p.penalty?.timestamp ?: lastInfo.penaltyTimestamp,
            sessionEpoch = sessionEpoch,
            sessionType = effectiveSessionType,
            playerCarUuid = playerCarUuid ?: lastInfo.playerCarUuid
        )

        return lastInfo
    }

    override fun clearPenalty() {
        penaltyGroupId = null
        penaltyGroupMs = 0L
        lastInfo = lastInfo.copy(
            hasPenalty = false,
            penaltyReason = null,
            penaltyId = null,
            penaltyTimestamp = null
        )
    }

    override fun clear() {
        runCatching { raf?.close() }
        raf = null
        openedFile = null
        openedFileKey = null
        openedLastModified = 0L
        lastPos = 0L
        pending = ""
        sessionEpoch = 0L
        lastInfo = EvoFileInfo()
        trackIdSource = TrackIdSource.NONE
        carSource = CarSource.NONE
        penaltyGroupId = null
        penaltyGroupMs = 0L
        lastGameStartedBumpMs = 0L
        lastHardBoundaryBumpMs = 0L
        playerCarUuid = null
        uuidToCarMap.clear()
        locator.clear()
    }

    private fun ensureOpenOrReopenIfRotated(file: File) {
        val currentKey = fileKey(file)
        val currentLm = file.lastModified()

        val samePath = openedFile?.absolutePath == file.absolutePath
        val keyChanged = (openedFileKey != null && currentKey != null && openedFileKey != currentKey)
        val lmWentBack = (openedLastModified != 0L && currentLm != 0L && currentLm < openedLastModified)

        if (raf == null || openedFile == null || !samePath || keyChanged || lmWentBack) {
            reopen(
                file, reason = when {
                    !samePath -> "pathChanged"
                    keyChanged -> "fileKeyChanged"
                    lmWentBack -> "lastModifiedWentBack"
                    else -> "init"
                }
            )
        }
    }

    private fun reopen(file: File, reason: String) {
        runCatching { raf?.close() }

        openedFile = file
        openedFileKey = fileKey(file)
        openedLastModified = file.lastModified()

        raf = RandomAccessFile(file, "r")
        pending = ""
        lastPos = file.length()

        bumpEpoch("reopen:$reason")
        primeFromTail(file)
    }

    private fun fileKey(file: File): Any? = runCatching {
        val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
        attrs.fileKey()
    }.getOrNull()

    private fun primeFromTail(file: File) {
        val r = raf ?: return
        val len = file.length()
        if (len <= 0L) return

        val start = (len - PRIME_TAIL_BYTES).coerceAtLeast(0L)
        r.seek(start)

        val buf = ByteArray((len - start).toInt().coerceAtMost(PRIME_TAIL_BYTES))
        val read = r.read(buf)
        if (read <= 0) return

        val lines = buf.decodeToString(endIndex = read).split('\n').map { it.trimEnd('\r') }
        val p = parseLines(lines, includePenalties = false)
        val resolved = resolveTrackId(p)

        val newCar = chooseCarModel(p)

        lastInfo = lastInfo.copy(
            trackId = resolved.trackId,
            layoutId = resolved.layout,
            trackName = buildDisplayName(p.physicsTrackName, resolved.layout)
                ?: p.gameStartedTrackName
                ?: resolved.trackId,
            carModel = newCar,
            driverName = p.driverName,
            driverSteamId = p.driverSteamId,
            sessionType = p.sessionType ?: EvoSessionType.UNKNOWN,
            playerCarUuid = playerCarUuid
        )

        trackIdSource = resolved.source
    }

    private fun readNewLines(file: File): List<String> {
        val r = raf ?: return emptyList()
        val len = file.length()

        if (len < lastPos) {
            reopen(file, reason = "truncated")
            return emptyList()
        }

        if (len == lastPos) return emptyList()

        r.seek(lastPos)
        val toRead = min((len - lastPos).toInt(), MAX_READ_BYTES)
        val buf = ByteArray(toRead)
        val read = r.read(buf)
        if (read <= 0) return emptyList()

        lastPos += read
        openedLastModified = file.lastModified()

        val text = pending + buf.decodeToString(endIndex = read)
        val parts = text.split('\n').map { it.trimEnd('\r') }
        pending = if (text.endsWith("\n")) "" else parts.lastOrNull().orEmpty()

        return (if (text.endsWith("\n")) parts else parts.dropLast(1))
            .filter { it.isNotBlank() }
    }

    private fun bumpEpoch(reason: String) {
        sessionEpoch++
        lastInfo = lastInfo.copy(
            sessionEpoch = sessionEpoch,
            sessionType = EvoSessionType.UNKNOWN
        )
        trackIdSource = TrackIdSource.NONE
        carSource = CarSource.NONE
        penaltyGroupId = null
        penaltyGroupMs = 0L
        logger.info { "EvoFileInfo epoch++ -> $sessionEpoch ($reason)" }
    }

    private fun parseLines(lines: List<String>, includePenalties: Boolean = true): Parsed {
        var hardBoundary = false
        var gameStarted = false
        var physicsTrackName: String? = null
        var gameStartedTrackName: String? = null
        var slugBase: String? = null
        var slugLayout: String? = null
        var containerFolder: String? = null
        var containerLayout: String? = null

        var bestCar: String? = null
        var bestCarSource: CarSource = CarSource.NONE

        var driverName: String? = null
        var driverSteamId: String? = null
        var penalty: Penalty? = null
        var sessionType: EvoSessionType? = null

        for (line in lines) {
            if (isHardBoundary(line)) {
                hardBoundary = true
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
                if (tokens.size >= 2) {
                    slugBase = tokens.dropLast(1).joinToString("_")
                    slugLayout = tokens.last()
                }
            }

            if (containerFolder == null) {
                RE_CONTAINER.find(line)?.let {
                    containerFolder = it.groupValues[1]
                    containerLayout = it.groupValues[2]
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
            gameStarted = gameStarted,
            physicsTrackName = physicsTrackName,
            gameStartedTrackName = gameStartedTrackName,
            slugBase = slugBase,
            slugLayout = slugLayout,
            containerFolder = containerFolder,
            containerLayout = containerLayout,
            carModel = bestCar,
            carSource = bestCarSource,
            driverName = driverName,
            driverSteamId = driverSteamId,
            penalty = penalty,
            sessionType = sessionType
        )
    }

    private fun chooseCarModel(p: Parsed): String? {
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

    private fun resolveTrackId(p: Parsed): ResolvedTrack {
        val baseFromPhysics = p.physicsTrackName?.let(::normalize)
        val baseFromSlug = p.slugBase?.let(::normalize)
        val baseFromGameStarted = p.gameStartedTrackName?.let(::normalize)
        val baseFromFolder = p.containerFolder?.let(::normalize)

        val base = baseFromPhysics ?: baseFromSlug ?: baseFromGameStarted ?: baseFromFolder

        val layoutFromSlug = p.slugLayout?.let(::normalize)
        val layoutFromContainer = p.containerLayout?.let { normalize(mapContainerLayout(it)) }
        val layout = layoutFromSlug ?: layoutFromContainer

        val candidateId = buildTrackId(base, layout)
        val candidateSource = when {
            layoutFromSlug != null -> TrackIdSource.SLUG
            layoutFromContainer != null -> TrackIdSource.CONTAINER
            baseFromGameStarted != null -> TrackIdSource.GAME_STARTED
            else -> TrackIdSource.NONE
        }

        val stableId = lastInfo.trackId?.takeIf { it.isNotBlank() }
        val stableSource = trackIdSource
        val stableLayout = lastInfo.layoutId?.takeIf { it.isNotBlank() }

        if (stableId != null && stableLayout != null && layout.isNullOrBlank()) {
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
            layout = if (effectiveId == candidateId) layout else lastInfo.layoutId,
            source = effectiveSource
        )
    }

    private fun buildTrackId(base: String?, layout: String?): String? {
        val b = base?.takeIf { it.isNotBlank() } ?: return null
        val l = layout?.takeIf { it.isNotBlank() } ?: return b
        return if (b.endsWith("_$l")) b else "${b}_$l"
    }

    private fun buildDisplayName(physicsName: String?, layout: String?): String? {
        val n = physicsName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val l = layout?.trim()?.takeIf { it.isNotBlank() } ?: return n
        return "$n ${l.uppercase()}"
    }

    private fun normalize(raw: String): String =
        raw.lowercase().trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

    private fun mapContainerLayout(raw: String): String {
        return when (normalize(raw)) {
            "gp_circuit" -> "gp"
            "gp_circuit_shortcut", "gp_circuit_short", "gp_shortcut" -> "gp_short"
            else -> raw
        }
    }

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

        const val PRIME_TAIL_BYTES = 4 * 1024 * 1024
        const val MAX_READ_BYTES = 256 * 1024
        const val PENALTY_GROUP_WINDOW_MS = 3_000L

        val TS_FORMAT: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendLiteral('.')
            .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 9, false)
            .optionalEnd()
            .toFormatter(Locale.US)
    }
}
