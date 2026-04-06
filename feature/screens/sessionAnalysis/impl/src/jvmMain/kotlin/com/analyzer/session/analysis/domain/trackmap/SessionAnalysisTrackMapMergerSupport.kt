package com.analyzer.session.analysis.domain.trackmap

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.hypot

/**
 * Support steps for the track-map merger keep fitting, trimming, and validation rules in one place.
 */
internal fun List<SessionAnalysisTrackMapPoint>.resolveClosureThreshold(): Float {
    val segmentLengths = buildList<Float> {
        for (index in 1..this@resolveClosureThreshold.lastIndex) {
            val length = this@resolveClosureThreshold.distanceBetween(
                this@resolveClosureThreshold[index - 1],
                this@resolveClosureThreshold[index],
            )
            if (length.isFinite() && length > 0.0001f) add(length)
        }
    }
    val medianSegment = segmentLengths.sorted()
        .let { sorted ->
            if (sorted.isEmpty()) return@let 0f
            val middle = sorted.size / 2
            if (sorted.size % 2 == 0) {
                (sorted[middle - 1] + sorted[middle]) * 0.5f
            } else {
                sorted[middle]
            }
        }
    return maxOf(1.5f, medianSegment * 3.5f)
}

internal fun List<SessionAnalysisTrackMapPoint>.distanceBetween(
    first: SessionAnalysisTrackMapPoint,
    second: SessionAnalysisTrackMapPoint,
): Float = hypot(
    second.x - first.x,
    second.y - first.y,
)

internal fun squaredDistance(first: SessionAnalysisTrackMapPoint, second: SessionAnalysisTrackMapPoint): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return dx * dx + dy * dy
}
