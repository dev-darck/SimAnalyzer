package com.project.analyzer.impl.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.max

@Composable
internal fun rememberHudRenderProfiler(): HudRenderProfiler? {
    val enabled = remember { isHudRenderProfilerEnabled() }
    return if (enabled) remember { HudRenderProfiler() } else null
}

@Composable
internal fun HudRenderFrameProfilerEffect(profiler: HudRenderProfiler?) {
    if (profiler == null) return

    LaunchedEffect(profiler) {
        while (isActive) {
            withFrameNanos { frameTimeNs ->
                profiler.onFrame(frameTimeNs)
            }
        }
    }
}

internal class HudRenderProfiler {
    private val logger = logger()

    private val drawSamples = LongArray(SAMPLE_CAPACITY)
    private val frameSamples = LongArray(SAMPLE_CAPACITY)

    private var drawWriteIndex = 0
    private var frameWriteIndex = 0
    private var drawTotalCount = 0
    private var frameTotalCount = 0

    private var recompositionsSinceLog = 0
    private var visiblePanelsCount = 0
    private var lastFrameTimeNs = 0L
    private var lastLogTimeNs = 0L

    fun onRecomposition(visiblePanelsCount: Int) {
        this.visiblePanelsCount = visiblePanelsCount
        recompositionsSinceLog++
    }

    fun onDrawCompleted(drawNs: Long) {
        drawSamples[drawWriteIndex] = drawNs
        drawWriteIndex = (drawWriteIndex + 1) % drawSamples.size
        drawTotalCount++
    }

    fun onFrame(frameTimeNs: Long) {
        if (lastFrameTimeNs != 0L) {
            val delta = (frameTimeNs - lastFrameTimeNs).coerceAtLeast(0L)
            frameSamples[frameWriteIndex] = delta
            frameWriteIndex = (frameWriteIndex + 1) % frameSamples.size
            frameTotalCount++
        }
        lastFrameTimeNs = frameTimeNs

        if (lastLogTimeNs == 0L) {
            lastLogTimeNs = frameTimeNs
            return
        }

        if (frameTimeNs - lastLogTimeNs < LOG_PERIOD_NS) return
        lastLogTimeNs = frameTimeNs
        logSnapshot()
        recompositionsSinceLog = 0
    }

    private fun logSnapshot() {
        val frameValues = snapshot(frameSamples, frameTotalCount, frameWriteIndex)
        val drawValues = snapshot(drawSamples, drawTotalCount, drawWriteIndex)

        if (frameValues.isEmpty() && drawValues.isEmpty()) return

        val frameStats = stats(frameValues)
        val drawStats = stats(drawValues)

        val over8Ms = frameValues.count { it >= FRAME_120HZ_BUDGET_NS }
        val over16Ms = frameValues.count { it >= FRAME_60HZ_BUDGET_NS }

        logger.atInfo(RATE_LIMITED) {
            message = buildString {
                append("hud-profiler ")
                append("panels=").append(visiblePanelsCount).append(' ')
                append("recompositions=").append(recompositionsSinceLog).append(' ')
                append("frames=").append(frameValues.size).append(' ')
                append("frameMs(avg/p95/max)=")
                append(formatMs(frameStats.avgNs)).append('/')
                append(formatMs(frameStats.p95Ns)).append('/')
                append(formatMs(frameStats.maxNs)).append(' ')
                append("drawMs(avg/p95/max)=")
                append(formatMs(drawStats.avgNs)).append('/')
                append(formatMs(drawStats.p95Ns)).append('/')
                append(formatMs(drawStats.maxNs)).append(' ')
                append("frame>8.3ms=").append(over8Ms).append(' ')
                append("frame>16.7ms=").append(over16Ms)
            }
        }
    }

    private fun snapshot(buffer: LongArray, totalCount: Int, writeIndex: Int): LongArray {
        val size = minOf(totalCount, buffer.size)
        if (size == 0) return LongArray(0)

        val out = LongArray(size)
        val start = if (totalCount >= buffer.size) writeIndex else 0
        for (i in 0 until size) {
            out[i] = buffer[(start + i) % buffer.size]
        }
        return out
    }

    private fun stats(values: LongArray): NsStats {
        if (values.isEmpty()) return NsStats()

        var sum = 0L
        var maxNs = 0L
        for (value in values) {
            sum += value
            maxNs = max(maxNs, value)
        }

        val sorted = values.copyOf().apply { sort() }
        val p95Index = ((sorted.size - 1) * 95) / 100

        return NsStats(
            avgNs = sum.toDouble() / sorted.size.toDouble(),
            p95Ns = sorted[p95Index].toDouble(),
            maxNs = maxNs.toDouble(),
        )
    }

    private fun formatMs(ns: Double): String = String.format(Locale.US, "%.2f", ns / 1_000_000.0)

    private data class NsStats(val avgNs: Double = 0.0, val p95Ns: Double = 0.0, val maxNs: Double = 0.0)

    private companion object {
        private const val SAMPLE_CAPACITY = 1024
        private const val LOG_PERIOD_NS = 5_000_000_000L
        private const val FRAME_120HZ_BUDGET_NS = 8_333_333L
        private const val FRAME_60HZ_BUDGET_NS = 16_666_667L
    }
}

private fun isHudRenderProfilerEnabled(): Boolean =
    parseBoolFlag(System.getProperty(HUD_PROFILE_PROP)) || parseBoolFlag(System.getenv(HUD_PROFILE_ENV))

private fun parseBoolFlag(value: String?): Boolean {
    val normalized = value?.trim()?.lowercase(Locale.US) ?: return false
    return normalized == "1" || normalized == "true" || normalized == "yes" || normalized == "on"
}

private const val HUD_PROFILE_PROP = "simanalyzer.hud.profile"
private const val HUD_PROFILE_ENV = "SIMANALYZER_HUD_PROFILE"
