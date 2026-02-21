package com.project.analyzer.ac.telemetry.impl.fallback.logfile

import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoFileInfo
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.EvoSessionType
import com.project.analyzer.ac.telemetry.impl.fallback.logfile.model.Parsed
import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.utils.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@Inject
@SingleIn(SessionScope::class)
class AcEvoFileInfoExtractor(private val locator: AcEvoLogLocator) : EvoFileInfoSource {

    private val parser = AcEvoLogParser()

    private var raf: RandomAccessFile? = null
    private var openedFile: File? = null
    private var openedFileKey: Any? = null
    private var openedLastModified: Long = 0L
    private var lastFileKeyCheckMs: Long = 0L
    private var lastPos: Long = 0L
    private var pending: String = ""
    private var sessionEpoch: Long = 0L
    private var lastInfo: EvoFileInfo = EvoFileInfo()
    private var lastGameStartedBumpMs: Long = 0L
    private var lastHardBoundaryBumpMs: Long = 0L
    private var lastGameStartedTsMs: Long? = null
    private var lastHardBoundaryTsMs: Long? = null

    override fun poll(): EvoFileInfo {
        val file = locator.locateLogFile() ?: return lastInfo

        ensureOpenOrReopenIfRotated(file)

        val lines = readNewLines(file)
        if (lines.isEmpty()) return lastInfo

        val parsed = parser.parseLines(lines)

        maybeBumpEpoch(parsed)
        mergeParsedIntoLastInfo(parsed, includePenalties = true)

        return lastInfo
    }

    override fun clearPenalty() {
        parser.clearPenaltyGroup()
        lastInfo = lastInfo.copy(
            hasPenalty = false,
            penaltyReason = null,
            penaltyId = null,
            penaltyTimestamp = null,
        )
    }

    override fun clear() {
        runCatching { raf?.close() }
        raf = null
        openedFile = null
        openedFileKey = null
        openedLastModified = 0L
        lastFileKeyCheckMs = 0L
        lastPos = 0L
        pending = ""
        sessionEpoch = 0L
        lastInfo = EvoFileInfo()
        lastGameStartedBumpMs = 0L
        lastHardBoundaryBumpMs = 0L
        lastGameStartedTsMs = null
        lastHardBoundaryTsMs = null
        parser.clear()
        locator.clear()
    }

    private fun ensureOpenOrReopenIfRotated(file: File) {
        val currentLm = file.lastModified()

        val samePath = openedFile?.absolutePath == file.absolutePath
        val nowMs = System.currentTimeMillis()
        val shouldCheckKey =
            openedFileKey != null && (nowMs - lastFileKeyCheckMs) >= FILE_KEY_CHECK_INTERVAL_MS
        val currentKey = if (shouldCheckKey) {
            lastFileKeyCheckMs = nowMs
            fileKey(file)
        } else {
            null
        }

        val keyChanged = (openedFileKey != null && currentKey != null && openedFileKey != currentKey)
        val lmWentBack = (openedLastModified != 0L && currentLm != 0L && currentLm < openedLastModified)

        if (raf == null || openedFile == null || !samePath || keyChanged || lmWentBack) {
            reopen(
                file,
                reason = when {
                    !samePath -> "pathChanged"
                    keyChanged -> "fileKeyChanged"
                    lmWentBack -> "lastModifiedWentBack"
                    else -> "init"
                },
            )
        }
    }

    private fun reopen(file: File, reason: String) {
        runCatching { raf?.close() }

        openedFile = file
        openedFileKey = fileKey(file)
        openedLastModified = file.lastModified()
        lastFileKeyCheckMs = System.currentTimeMillis()

        raf = RandomAccessFile(file, "r")
        pending = ""
        lastPos = file.length()
        lastGameStartedBumpMs = 0L
        lastHardBoundaryBumpMs = 0L
        lastGameStartedTsMs = null
        lastHardBoundaryTsMs = null

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

        val tailText = buf.decodeToString(endIndex = read)
        val safeText = if (start > 0L) tailText.substringAfter('\n', missingDelimiterValue = "") else tailText
        val lines = safeText.split('\n').map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        val parsed = parser.parseLines(lines, includePenalties = false)
        mergeParsedIntoLastInfo(parsed, includePenalties = false)
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

    private fun maybeBumpEpoch(parsed: Parsed) {
        val nowMs = System.currentTimeMillis()

        if (shouldBumpHardBoundary(parsed, nowMs)) {
            bumpEpoch("hardBoundary")
            return
        }

        if (shouldBumpGameStarted(parsed, nowMs)) {
            bumpEpoch("gameStarted")
        }
    }

    private fun shouldBumpHardBoundary(parsed: Parsed, nowMs: Long): Boolean {
        if (!parsed.hardBoundary) return false

        val ts = parsed.hardBoundaryTimestampMs
        if (ts != null) {
            val lastTs = lastHardBoundaryTsMs
            if (lastTs == null || ts > lastTs) {
                lastHardBoundaryTsMs = ts
                return true
            }
            return false
        }

        if (nowMs - lastHardBoundaryBumpMs > HARD_BOUNDARY_DEBOUNCE_MS) {
            lastHardBoundaryBumpMs = nowMs
            return true
        }

        return false
    }

    private fun shouldBumpGameStarted(parsed: Parsed, nowMs: Long): Boolean {
        if (!parsed.gameStarted) return false

        val ts = parsed.gameStartedTimestampMs
        if (ts != null) {
            val lastTs = lastGameStartedTsMs
            if (lastTs == null || ts > lastTs) {
                lastGameStartedTsMs = ts
                return true
            }
            return false
        }

        if (nowMs - lastGameStartedBumpMs > GAME_STARTED_DEBOUNCE_MS) {
            lastGameStartedBumpMs = nowMs
            return true
        }

        return false
    }

    private fun bumpEpoch(reason: String) {
        sessionEpoch++
        lastInfo = lastInfo.copy(
            sessionEpoch = sessionEpoch,
            sessionType = EvoSessionType.UNKNOWN,
            hasPenalty = false,
            penaltyId = null,
            penaltyReason = null,
            penaltyTimestamp = null,
        )
        parser.resetForEpoch()
        logger.info { "EvoFileInfo epoch++ -> $sessionEpoch ($reason)" }
    }

    private companion object {

        const val PRIME_TAIL_BYTES = 4 * 1024 * 1024
        const val MAX_READ_BYTES = 256 * 1024
        val FILE_KEY_CHECK_INTERVAL_MS = 2.seconds.inWholeMilliseconds
        val HARD_BOUNDARY_DEBOUNCE_MS = 5.seconds.inWholeMilliseconds
        val GAME_STARTED_DEBOUNCE_MS = 8.seconds.inWholeMilliseconds
    }

    private fun buildFallbackTrackId(trackName: String?, layout: String?): String? {
        val name = trackName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return TrackIdNormalizer.normalize(
            track = name,
            layout = layout,
        ).takeIf { it.isNotBlank() }
    }

    private fun mergeParsedIntoLastInfo(parsed: Parsed, includePenalties: Boolean) {
        val resolved = parser.resolveTrackId(parsed, lastInfo)
        val effectiveLayout = resolved.layout ?: lastInfo.layoutId

        val rawTrackName = resolveTrackName(
            parsed = parsed,
            effectiveTrackId = resolved.trackId ?: lastInfo.trackId,
            effectiveLayout = effectiveLayout,
        )
        val trackName = rawTrackName?.trim()?.takeIf { it.isNotBlank() }

        val fallbackTrackId = buildFallbackTrackId(trackName, effectiveLayout)
        val effectiveTrackId = when {
            !resolved.trackId.isNullOrBlank() -> resolved.trackId
            !fallbackTrackId.isNullOrBlank() -> fallbackTrackId
            else -> lastInfo.trackId
        }

        val newCar = parser.chooseCarModel(parsed, lastInfo)
        val effectiveSessionType = parsed.sessionType ?: lastInfo.sessionType

        val hasPenalty = if (includePenalties) {
            parsed.penalty != null || lastInfo.hasPenalty
        } else {
            lastInfo.hasPenalty
        }

        lastInfo = lastInfo.copy(
            trackName = trackName,
            trackId = effectiveTrackId,
            layoutId = effectiveLayout,
            carModel = newCar ?: lastInfo.carModel,
            driverName = parsed.driverName ?: lastInfo.driverName,
            driverSteamId = parsed.driverSteamId ?: lastInfo.driverSteamId,
            hasPenalty = hasPenalty,
            penaltyId = if (includePenalties) parsed.penalty?.id ?: lastInfo.penaltyId else lastInfo.penaltyId,
            penaltyReason = if (includePenalties) {
                parsed.penalty?.reason
                    ?: lastInfo.penaltyReason
            } else {
                lastInfo.penaltyReason
            },
            penaltyTimestamp = if (includePenalties) {
                parsed.penalty?.timestamp
                    ?: lastInfo.penaltyTimestamp
            } else {
                lastInfo.penaltyTimestamp
            },
            sessionEpoch = sessionEpoch,
            sessionType = effectiveSessionType,
            playerCarUuid = parser.currentPlayerCarUuid ?: lastInfo.playerCarUuid,
        )
    }

    private fun resolveTrackName(parsed: Parsed, effectiveTrackId: String?, effectiveLayout: String?): String? {
        parser.buildDisplayName(parsed.physicsTrackName, effectiveLayout)?.let { return it }
        parser.buildDisplayName(parsed.gameStartedTrackName, effectiveLayout)?.let { return it }

        val previousName = lastInfo.trackName?.trim()?.takeIf { it.isNotBlank() }
        val previousTrackId = lastInfo.trackId?.trim()?.takeIf { it.isNotBlank() }
        val newTrackId = effectiveTrackId?.trim()?.takeIf { it.isNotBlank() }

        if (previousName != null) {
            if (newTrackId == null || previousTrackId == null || newTrackId == previousTrackId) {
                return previousName
            }
        }

        return newTrackId ?: previousName
    }
}
