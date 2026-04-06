package com.analyzer.session.analysis.presentation.builder.coach

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.utils.ext.averageOrNull
import kotlin.math.hypot

internal fun computeLineStats(
    samples: List<SessionAnalysisSample>,
    referenceProfile: ReferenceLapProfile,
    trackMap: SessionAnalysisTrackMap?,
): LineStats {
    val gaps = samples.mapNotNull { sample ->
        val trackPosition = sample.trackPosition ?: return@mapNotNull null
        val x = sample.trackX ?: return@mapNotNull null
        val y = sample.trackY ?: return@mapNotNull null
        val referencePoint = referenceProfile.pointAt(trackPosition) ?: return@mapNotNull null
        val gapMeters = hypot(x - referencePoint.first, y - referencePoint.second)
        val trackWidth = trackMap?.widthAt(trackPosition)
        gapMeters to trackWidth
    }
    if (gaps.isEmpty()) return LineStats()

    val averageGapMeters = gaps.map { gap -> gap.first }.averageOrNull()
    val normalizedGap = gaps.mapNotNull { (gapMeters, trackWidth) ->
        val width = trackWidth?.takeIf { value -> value > 1f } ?: return@mapNotNull null
        (gapMeters / (width * 0.5f)).coerceIn(0f, 1f)
    }.averageOrNull()
    return LineStats(
        averageGapMeters = averageGapMeters,
        normalizedGap = normalizedGap,
    )
}

private fun SessionAnalysisTrackMap.widthAt(trackPosition: Float): Float? {
    if (points.isEmpty()) return null
    val scaledIndex = trackPosition.coerceIn(0f, 1f) * (points.size - 1)
    val lowerIndex = scaledIndex.toInt().coerceIn(0, points.lastIndex)
    val upperIndex = (lowerIndex + 1).coerceAtMost(points.lastIndex)
    val lowerWidth = points[lowerIndex].leftWidthMeters?.plus(points[lowerIndex].rightWidthMeters ?: 0f)
    val upperWidth = points[upperIndex].leftWidthMeters?.plus(points[upperIndex].rightWidthMeters ?: 0f)
    return interpolateCoachValue(
        startPos = lowerIndex.toFloat(),
        endPos = upperIndex.toFloat(),
        startValue = lowerWidth,
        endValue = upperWidth,
        valuePos = scaledIndex,
    )?.takeIf { width -> width > 1f }
}
