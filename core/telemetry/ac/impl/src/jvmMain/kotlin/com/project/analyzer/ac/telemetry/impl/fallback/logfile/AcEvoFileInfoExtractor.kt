package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.api.di.SessionScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.RandomAccessFile
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
    private var lastInfo: EvoFileInfo = EvoFileInfo(sessionEpoch = sessionEpoch)

    private var currentPenalty: Boolean = false
    private var penaltyReason: String? = null
    private var penaltyTimestamp: String? = null

    fun poll(): EvoFileInfo {
        val file = locator.locateLogFile() ?: return lastInfo
        ensureOpen(file)

        val lines = readNewLines(file)
        if (lines.isEmpty()) return lastInfo

        var bumpEpoch = false

        var trackIdFromSlug: String? = null
        var layoutFromSlug: String? = null
        var trackIdFromContainer: String? = null
        var layoutFromContainer: String? = null
        var trackIdFromGameStarted: String? = null

        var trackNameFromPhysics: String? = null
        var trackNameFromGameStarted: String? = null

        var carModel: String? = null
        var driverName: String? = null
        var driverSteamId: String? = null

        for (line in lines) {
            if (isSessionBoundaryLine(line)) {
                bumpEpoch = true
                currentPenalty = false
                penaltyReason = null
                penaltyTimestamp = null
            }

            parseTrackNameSlug(line)?.let { parsed ->
                trackIdFromSlug = normalizeTrackId("${parsed.baseSlug}_${parsed.layout}")
                layoutFromSlug = parsed.layout
            }

            parseTrackFromContainer(line)?.let { (folder, layout) ->
                if (trackIdFromContainer == null) {
                    trackIdFromContainer = normalizeTrackId("${folder}_${layout}")
                    layoutFromContainer = layout
                }
            }

            parsePhysicsTrackDisplayName(line)?.let { human ->
                trackNameFromPhysics = human
            }

            parseGameStarted(line)?.let { parsed ->
                parsed.trackName?.let { tn ->
                    val cleanName = cleanupGameStartedTrackName(tn)
                    trackNameFromGameStarted = cleanName
                    trackIdFromGameStarted = normalizeTrackId(cleanName)
                }
                parsed.carModel?.let { cm -> carModel = cm }
                bumpEpoch = true
            }

            parseCarModel(line)?.let { cm ->
                carModel = cm
            }

            parseDriver(line)?.let { (dn, sid) ->
                driverName = dn
                driverSteamId = sid
            }

            parsePenalty(line)?.let { (reason, timestamp) ->
                currentPenalty = true
                penaltyReason = reason
                penaltyTimestamp = timestamp
            }

            if (isLapInvalidLine(line)) {
                val timestamp = parseTimestamp(line)
                currentPenalty = true
                penaltyReason = "Lap invalidated"
                penaltyTimestamp = timestamp
            }
        }

        val baseInfo = if (bumpEpoch) {
            EvoFileInfo(sessionEpoch = sessionEpoch + 1L)
        } else {
            lastInfo
        }

        val newTrackId =
            trackIdFromSlug
                ?: trackIdFromContainer
                ?: trackIdFromGameStarted
                ?: baseInfo.trackId

        val newTrackName =
            buildTrackNameDisplay(
                physicsName = trackNameFromPhysics,
                layout = layoutFromSlug ?: layoutFromContainer
            )
                ?: trackNameFromGameStarted
                ?: baseInfo.trackName
                ?: newTrackId

        val newCarModel = carModel ?: baseInfo.carModel
        val newDriverName = driverName ?: baseInfo.driverName
        val newDriverSteamId = driverSteamId ?: baseInfo.driverSteamId

        val trackChanged = (newTrackId != null && newTrackId != baseInfo.trackId)
        if (trackChanged) bumpEpoch = true

        val changed =
            (newTrackId != baseInfo.trackId) ||
                (newTrackName != baseInfo.trackName) ||
                (newCarModel != baseInfo.carModel) ||
                (newDriverName != baseInfo.driverName) ||
                (newDriverSteamId != baseInfo.driverSteamId)

        if (changed || penaltyTimestamp != baseInfo.penaltyTimestamp) {
            lastInfo = baseInfo.copy(
                trackName = newTrackName,
                trackId = newTrackId,
                carModel = newCarModel,
                driverName = newDriverName,
                driverSteamId = newDriverSteamId,
                hasPenalty = currentPenalty,
                penaltyReason = penaltyReason,
                penaltyTimestamp = penaltyTimestamp
            )
        }

        if (bumpEpoch) {
            sessionEpoch += 1L
            lastInfo = lastInfo.copy(sessionEpoch = sessionEpoch)
        } else if (lastInfo.sessionEpoch != sessionEpoch) {
            lastInfo = lastInfo.copy(sessionEpoch = sessionEpoch)
        }

        return lastInfo
    }

    fun clearPenalty() {
        currentPenalty = false
        penaltyReason = null
        lastInfo = lastInfo.copy(hasPenalty = false, penaltyReason = null)
    }

    fun clear() {
        runCatching { raf?.close() }
        raf = null
        openedFile = null
        lastPos = 0L
        pending = ""
        sessionEpoch = 0L
        lastInfo = EvoFileInfo(sessionEpoch = sessionEpoch)
        locator.clear()
        currentPenalty = false
        penaltyReason = null
        penaltyTimestamp = null
    }

    private fun ensureOpen(file: File) {
        if (openedFile?.absolutePath == file.absolutePath && raf != null) return

        runCatching { raf?.close() }
        openedFile = file
        raf = RandomAccessFile(file, "r")
        lastPos = 0L
        pending = ""
    }

    private fun readNewLines(file: File): List<String> {
        val r = raf ?: return emptyList()

        val len = file.length()
        if (len < lastPos) lastPos = 0L
        if (len == lastPos) return emptyList()

        r.seek(lastPos)

        val toRead = min((len - lastPos).toInt(), 256 * 1024)
        val buf = ByteArray(toRead)
        val read = r.read(buf)
        if (read <= 0) return emptyList()

        lastPos += read.toLong()

        val text = pending + buf.decodeToString(endIndex = read)
        val parts = text.split('\n').map { it.trimEnd('\r') }

        pending = if (text.endsWith("\n")) "" else (parts.lastOrNull().orEmpty())

        return if (text.endsWith("\n")) {
            parts.filter { it.isNotBlank() }
        } else {
            parts.dropLast(1).filter { it.isNotBlank() }
        }
    }

    private data class TrackSlugParsed(val baseSlug: String, val layout: String)

    private fun parseTrackNameSlug(line: String): TrackSlugParsed? {
        val m = RE_TRACK_NAME_SLUG.find(line) ?: return null
        val raw = m.groupValues[1].trim()
        if (raw.isBlank()) return null

        val tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.size < 2) return null

        val layout = tokens.last()
        val base = tokens.dropLast(1).joinToString("_")
        if (base.isBlank() || layout.isBlank()) return null

        return TrackSlugParsed(baseSlug = base, layout = layout)
    }

    private fun parseTrackFromContainer(line: String): Pair<String, String>? {
        val m = RE_TRACK_CONTAINER.find(line) ?: return null
        val folder = m.groupValues[1]
        val layout = m.groupValues[2]
        return folder to layout
    }

    private fun parsePhysicsTrackDisplayName(line: String): String? {
        val m = RE_PHYSICS_TRACK.find(line) ?: return null
        return m.groupValues[1].trim().takeIf { it.isNotBlank() }
    }

    private fun parseCarModel(line: String): String? {
        RE_CAR_CREATING.find(line)?.let { return it.groupValues[1] }
        RE_CAR_CONNECTED.find(line)?.let { return it.groupValues[1] }
        RE_CAR_DISPLAY_INIT.find(line)?.let { return it.groupValues[1] }
        return null
    }

    private fun parseDriver(line: String): Pair<String, String?>? {
        val m = RE_DRIVER.find(line) ?: return null
        val name = m.groupValues[1].trim().takeIf { it.isNotBlank() } ?: return null
        val steam = m.groupValues[2].trim().ifBlank { null }
        return name to steam
    }

    private data class ParsedStarted(val trackName: String?, val carModel: String?)

    private fun parseGameStarted(line: String): ParsedStarted? {
        if (!line.contains("Game Started!", ignoreCase = true)) return null
        val parts = line.split("|").map { it.trim() }
        val rawTrack = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
        val rawCar = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        return ParsedStarted(trackName = rawTrack, carModel = rawCar)
    }

    private fun cleanupGameStartedTrackName(raw: String): String {
        val s0 = raw.substringBefore("@").trim()
        val lower = s0.lowercase()

        val cuts = listOf(" time attack", " practice", " qualifying", " race", " hotlap")
        val idx = cuts
            .map { lower.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        val s1 = if (idx != null) s0.substring(0, idx).trim() else s0
        return s1.replace(Regex("""\s+"""), " ").trim()
    }

    private fun buildTrackNameDisplay(physicsName: String?, layout: String?): String? {
        val n = physicsName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val l = layout?.trim()?.takeIf { it.isNotBlank() } ?: return n
        return "$n ${l.uppercase()}"
    }

    private fun normalizeTrackId(raw: String): String {
        val lower = raw.lowercase().trim()
        val ws = lower.replace(Regex("\\s+"), "_")
        val cleaned = ws.replace(Regex("[^a-z0-9_]"), "_")
        val collapsed = cleaned.replace(Regex("_+"), "_")
        return collapsed.trim('_')
    }

    private fun isSessionBoundaryLine(line: String): Boolean {
        val s = line.lowercase()

        return s.contains("reset session") ||
            s.contains("session reset") ||
            s.contains("restart session") ||
            s.contains("resetting session") ||
            s.contains("return to pits") ||
            s.contains("returning to pits") ||
            s.contains("back to pits") ||
            s.contains("game started!")
    }

    private fun parsePenalty(line: String): Pair<String, String>? {
        val lower = line.lowercase()

        val penaltyPatterns = listOf(
            "penalty" to "Penalty",
            "track limits" to "Track limits",
            "cut detected" to "Corner cut",
            "cutting" to "Corner cut",
            "invalid lap" to "Invalid lap",
            "lap invalidated" to "Lap invalidated",
            "disqualified" to "Disqualified",
            "drive through" to "Drive through penalty",
            "stop and go" to "Stop and go penalty",
            "time penalty" to "Time penalty"
        )

        for ((pattern, reason) in penaltyPatterns) {
            if (lower.contains(pattern)) {
                val timestamp = parseTimestamp(line)
                return reason to timestamp
            }
        }

        return null
    }

    private fun parseTimestamp(line: String): String {
        // Извлекаем [2026-01-13 21:58:26.219] из начала строки
        val match = RE_TIMESTAMP.find(line)
        return match?.groupValues?.get(1) ?: System.currentTimeMillis().toString()
    }

    private fun isLapInvalidLine(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("lap invalid") ||
            lower.contains("invalidating lap") ||
            lower.contains("lap cancelled") ||
            lower.contains("lap deleted")
    }

    private companion object {

        private val RE_TIMESTAMP = Regex(
            "^\\s*\\[([0-9]{4}-[0-9]{2}-[0-9]{2}\\s+[0-9]{2}:[0-9]{2}:[0-9]{2}\\.[0-9]+)]"
        )

        private val RE_TRACK_NAME_SLUG = Regex(
            "\\bTRACK NAME\\b\\s+(.+)$",
            RegexOption.IGNORE_CASE
        )

        private val RE_TRACK_CONTAINER = Regex(
            "content[\\\\/]+tracks[\\\\/]+([^\\\\/]+)[\\\\/]+containers[\\\\/]+layout_([^\\\\/.]+)\\.scene",
            RegexOption.IGNORE_CASE
        )

        private val RE_PHYSICS_TRACK = Regex(
            "Creating physics track:\\s*(.+)$",
            RegexOption.IGNORE_CASE
        )

        private val RE_CAR_CREATING = Regex(
            "\\bCreating car:\\s*([^\\s]+)",
            RegexOption.IGNORE_CASE
        )

        private val RE_CAR_CONNECTED = Regex(
            "\\bconnected on car\\s+([^\\s,]+)",
            RegexOption.IGNORE_CASE
        )

        private val RE_CAR_DISPLAY_INIT = Regex(
            "\\bCarDisplay\\.init:([^\\s]+)",
            RegexOption.IGNORE_CASE
        )

        private val RE_DRIVER = Regex(
            "connecting gamecar.*\\((.+?)\\s*\\|\\s*([0-9]+)\\)",
            RegexOption.IGNORE_CASE
        )
    }
}
