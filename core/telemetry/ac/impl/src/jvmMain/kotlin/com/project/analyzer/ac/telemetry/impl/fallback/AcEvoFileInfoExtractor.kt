package com.project.analyzer.ac.telemetry.impl.fallback

import dev.zacsweers.metro.Inject
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.min

data class EvoFileInfo(
    val trackName: String? = null,
    val trackId: String? = null,
    val carModel: String? = null,
    val sessionEpoch: Long = 0L,
)

@Inject
class AcEvoFileInfoExtractor(
    private val locator: AcEvoLogLocator,
    private val calibrationLoader: TrackCalibrationLoader,
) {

    private var raf: RandomAccessFile? = null
    private var openedFile: File? = null
    private var lastPos: Long = 0L
    private var pending: String = ""

    private var sessionEpoch: Long = 0L
    private var lastInfo: EvoFileInfo = EvoFileInfo(sessionEpoch = sessionEpoch)

    fun poll(): EvoFileInfo {
        val file = locator.locateLogFile() ?: return lastInfo

        ensureOpen(file)

        val lines = readNewLines(file)
        var bumpEpoch = false
        var newTrackName: String? = null
        var newCarModel: String? = null

        for (line in lines) {
            if (isSessionBoundaryLine(line)) {
                bumpEpoch = true
            }

            parseGameStarted(line)?.let { parsed ->
                newTrackName = parsed.trackName
                newCarModel = parsed.carModel
                bumpEpoch = true
            }
        }

        if (newTrackName != null || newCarModel != null) {
            val tn = newTrackName?.takeIf { it.isNotBlank() } ?: lastInfo.trackName
            val cm = newCarModel?.takeIf { it.isNotBlank() } ?: lastInfo.carModel
            val tid = tn?.let { calibrationLoader.normalizeTrackId(it) } ?: lastInfo.trackId

            lastInfo = lastInfo.copy(
                trackName = tn,
                trackId = tid,
                carModel = cm,
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

    fun clear() {
        runCatching { raf?.close() }
        raf = null
        openedFile = null
        lastPos = 0L
        pending = ""
        sessionEpoch = 0L
        lastInfo = EvoFileInfo(sessionEpoch = sessionEpoch)
        locator.clear()
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

    private data class ParsedStarted(val trackName: String?, val carModel: String?)

    private fun parseGameStarted(line: String): ParsedStarted? {
        if (!line.contains("Game Started!", ignoreCase = true)) return null
        val parts = line.split("|").map { it.trim() }
        val track = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
        val car = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        return ParsedStarted(trackName = track, carModel = car)
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
            s.contains("loading") && s.contains("track") ||
            s.contains("unloading") && s.contains("track")
    }
}
