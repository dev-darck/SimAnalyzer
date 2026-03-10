package com.analyzer.trackmap.builder.recording.point

import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.project.analyzer.math.Vec2
import kotlin.math.cos

internal class TrackMapPointFilter(
    private val stats: TrackMapStatsCalculator,
    private val teleportDistanceMeters: Float,
    private val refinementSpacingMultiplier: Float,
    private val minRefinedSpacingMeters: Float,
) {

    fun evaluate(
        point: Vec2,
        snapshot: TrackMapRecorderState,
        runtime: TrackMapRecorderRuntime,
    ): TrackMapPointDecision {
        if (!point.isFinite()) return TrackMapPointDecision.REJECTED

        val previous = runtime.lastLapAccepted
        if (previous == null) {
            runtime.recordLapPoint(point, 0f, stats)
            return TrackMapPointDecision.ACCEPTED
        }

        val (minSpacing, maxSpacing) = effectiveSpacing(snapshot, runtime)
        val delta = point - previous
        val dist = delta.len()
        if (dist < minSpacing) return TrackMapPointDecision.REJECTED
        if (dist > teleportDistanceMeters) return TrackMapPointDecision.TELEPORT

        val dir = delta.safeNormalized(Vec2.Up)
        val cosThreshold = cos(Math.toRadians(snapshot.minAngleDeg.toDouble())).toFloat()
        val turnEnough = runtime.lastLapDir?.dot(dir)?.let { it <= cosThreshold } ?: true
        val longEnough = dist >= maxSpacing

        if (!turnEnough && !longEnough) return TrackMapPointDecision.REJECTED

        runtime.recordLapPoint(point, dist, stats)
        runtime.lastLapDir = dir

        return TrackMapPointDecision.ACCEPTED
    }

    private fun effectiveSpacing(
        snapshot: TrackMapRecorderState,
        runtime: TrackMapRecorderRuntime,
    ): Pair<Float, Float> {
        val factor = if (runtime.lapsRecorded <= 0) 1f else refinementSpacingMultiplier
        val minSpacing = (snapshot.minSpacingMeters * factor).coerceAtLeast(minRefinedSpacingMeters)
        val maxSpacing = (snapshot.maxSpacingMeters * factor).coerceAtLeast(minSpacing)
        return minSpacing to maxSpacing
    }

    private fun Vec2.isFinite(): Boolean = x.isFinite() && y.isFinite()
}
