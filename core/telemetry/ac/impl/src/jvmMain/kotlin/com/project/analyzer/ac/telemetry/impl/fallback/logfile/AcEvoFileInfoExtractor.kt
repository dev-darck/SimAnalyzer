package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.RandomAccessFile
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
) {

    private var raf: RandomAccessFile? = null
    private var openedFile: File? = null
    private var lastPos: Long = 0L
    private var pending: String = ""

    private var sessionEpoch: Long = 0L
    private var lastInfo: EvoFileInfo = EvoFileInfo()
    private var trackIdSource: TrackIdSource = TrackIdSource.NONE

    private var penaltyGroupId: String? = null
    private var penaltyGroupMs: Long = 0L

    private enum class TrackIdSource { NONE,
        GAME_STARTED,
        CONTAINER,
        SLUG
    }

    fun poll(): EvoFileInfo {
        val file = locator.locateLogFile() ?: return lastInfo
        ensureOpen(file)

        val lines = readNewLines(file)
        if (lines.isEmpty()) return lastInfo

        val p = parseLines(lines)

        if (p.hardBoundary || p.gameStarted) {
            bumpEpoch()
        }

        val resolved = resolveTrackId(p)

        val oldTrackId = lastInfo.trackId
        if (oldTrackId != null && resolved.trackId != null && resolved.trackId != oldTrackId) {
            bumpEpoch()
        }

        val trackName = buildDisplayName(p.physicsTrackName, resolved.layout)
            ?: p.gameStartedTrackName
            ?: lastInfo.trackName
            ?: resolved.trackId

        lastInfo = lastInfo.copy(
            trackName = trackName,
            trackId = resolved.trackId ?: lastInfo.trackId,
            layoutId = resolved.layout ?: lastInfo.layoutId,
            carModel = p.carModel ?: lastInfo.carModel,
            driverName = p.driverName ?: lastInfo.driverName,
            driverSteamId = p.driverSteamId ?: lastInfo.driverSteamId,
            hasPenalty = p.penalty != null || lastInfo.hasPenalty,
            penaltyId = p.penalty?.id ?: lastInfo.penaltyId,
            penaltyReason = p.penalty?.reason ?: lastInfo.penaltyReason,
            penaltyTimestamp = p.penalty?.timestamp ?: lastInfo.penaltyTimestamp,
            sessionEpoch = sessionEpoch
        )

        return lastInfo
    }

    fun clearPenalty() {
        penaltyGroupId = null
        penaltyGroupMs = 0L
        lastInfo = lastInfo.copy(
            hasPenalty = false,
            penaltyReason = null,
            penaltyId = null,
            penaltyTimestamp = null
        )
    }

    fun clear() {
        runCatching { raf?.close() }
        raf = null
        openedFile = null
        lastPos = 0L
        pending = ""
        sessionEpoch = 0L
        lastInfo = EvoFileInfo()
        trackIdSource = TrackIdSource.NONE
        penaltyGroupId = null
        penaltyGroupMs = 0L
        locator.clear()
    }

    private fun ensureOpen(file: File) {
        if (openedFile?.absolutePath == file.absolutePath && raf != null) return

        runCatching { raf?.close() }
        openedFile = file
        raf = RandomAccessFile(file, "r")
        pending = ""
        lastPos = file.length()

        bumpEpoch()
        primeFromTail(file)
    }

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

        lastInfo = lastInfo.copy(
            trackId = resolved.trackId,
            layoutId = resolved.layout,
            trackName = buildDisplayName(p.physicsTrackName, resolved.layout)
                ?: p.gameStartedTrackName
                ?: resolved.trackId,
            carModel = p.carModel,
            driverName = p.driverName,
            driverSteamId = p.driverSteamId
        )

        trackIdSource = resolved.source
    }

    private fun readNewLines(file: File): List<String> {
        val r = raf ?: return emptyList()
        val len = file.length()

        if (len < lastPos) {
            pending = ""
            lastPos = len
            bumpEpoch()
            primeFromTail(file)
            return emptyList()
        }

        if (len == lastPos) return emptyList()

        r.seek(lastPos)
        val toRead = min((len - lastPos).toInt(), MAX_READ_BYTES)
        val buf = ByteArray(toRead)
        val read = r.read(buf)
        if (read <= 0) return emptyList()

        lastPos += read

        val text = pending + buf.decodeToString(endIndex = read)
        val parts = text.split('\n').map { it.trimEnd('\r') }
        pending = if (text.endsWith("\n")) "" else parts.lastOrNull().orEmpty()

        return (if (text.endsWith("\n")) parts else parts.dropLast(1))
            .filter { it.isNotBlank() }
    }

    private fun bumpEpoch() {
        sessionEpoch++
        lastInfo = EvoFileInfo(sessionEpoch = sessionEpoch)
        trackIdSource = TrackIdSource.NONE
        penaltyGroupId = null
        penaltyGroupMs = 0L
    }

    private data class Parsed(
        val hardBoundary: Boolean = false,
        val gameStarted: Boolean = false,
        val physicsTrackName: String? = null,
        val gameStartedTrackName: String? = null,
        val slugBase: String? = null,
        val slugLayout: String? = null,
        val containerFolder: String? = null,
        val containerLayout: String? = null,
        val carModel: String? = null,
        val driverName: String? = null,
        val driverSteamId: String? = null,
        val penalty: Penalty? = null
    )

    private data class Penalty(val id: String, val reason: String, val timestamp: String)

    private fun parseLines(lines: List<String>, includePenalties: Boolean = true): Parsed {
        var hardBoundary = false
        var gameStarted = false
        var physicsTrackName: String? = null
        var gameStartedTrackName: String? = null
        var slugBase: String? = null
        var slugLayout: String? = null
        var containerFolder: String? = null
        var containerLayout: String? = null
        var carModel: String? = null
        var driverName: String? = null
        var driverSteamId: String? = null
        var penalty: Penalty? = null

        for (line in lines) {
            if (isHardBoundary(line)) {
                hardBoundary = true
            }

            if (line.contains("Game Started!", ignoreCase = true)) {
                gameStarted = true
                val parts = line.split("|").map { it.trim() }
                parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
                    gameStartedTrackName = cleanGameStartedTrack(it)
                }
                parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.let { carModel = it }
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

            RE_CAR.find(line)?.let { carModel = it.groupValues[1] }

            RE_DRIVER.find(line)?.let {
                driverName = it.groupValues[1].trim()
                driverSteamId = it.groupValues[2].takeIf { s -> s.isNotBlank() }
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
            carModel = carModel,
            driverName = driverName,
            driverSteamId = driverSteamId,
            penalty = penalty
        )
    }

    private data class ResolvedTrack(
        val trackId: String?,
        val layout: String?,
        val source: TrackIdSource
    )

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
        val cuts = listOf(" time attack", " practice", " qualifying", " race", " hotlap")
        val cutIdx = cuts.mapNotNull { lower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull()
        return (if (cutIdx != null) noDate.substring(0, cutIdx) else noDate)
            .replace(Regex("\\s+"), " ").trim()
    }

    private fun isHardBoundary(line: String): Boolean {
        val s = line.lowercase()
        return s.contains("reset session") ||
            s.contains("session reset") ||
            s.contains("restart session") ||
            s.contains("resetting session") ||
            s.contains("session ended") ||
            s.contains("end_session") ||
            s.contains("terminatesession")
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

        val RE_TIMESTAMP = Regex("""^\s*\[(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+)]""")
        val RE_PHYSICS_TRACK = Regex("""Creating physics track:\s*(.+)$""", RegexOption.IGNORE_CASE)
        val RE_TRACK_SLUG = Regex("""\bTRACK NAME\b\s+(.+)$""", RegexOption.IGNORE_CASE)
        val RE_CONTAINER = Regex(
            """content[\\/]+tracks[\\/]+([^\\/]+)[\\/]+containers[\\/]+layout_([^\\/.]+)\.scene""",
            RegexOption.IGNORE_CASE
        )

        val RE_CAR = Regex(
            """(?:Creating car:|CarDisplay\.init:|connected on car\s+)\s*(\S+)""",
            RegexOption.IGNORE_CASE
        )
        val RE_DRIVER = Regex(
            """connecting gamecar.*\((.+?)\s*\|\s*(\d*)\)""",
            RegexOption.IGNORE_CASE
        )
        val RE_PENALTY_KEY = Regex("""\{PENALTY_ADDED_KEY}\s*#(\d+)""", RegexOption.IGNORE_CASE)
    }
}
