package com.analyzer.session.analysis.domain.trackmap

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.toImmutableList

/**
 * Converts stored track-map assets into the telemetry-analysis map format used by the workspace.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class TrackMapSessionAnalysisMapper {

    fun map(trackMap: TrackMap): SessionAnalysisTrackMap? {
        val allPoints = trackMap.points + trackMap.pitPoints + trackMap.idealLinePoints
        if (allPoints.size < 2) return null

        val minX = trackMap.bounds?.minX ?: allPoints.minOf(TrackMapPoint::x)
        val minY = trackMap.bounds?.minY ?: allPoints.minOf(TrackMapPoint::y)
        val maxX = trackMap.bounds?.maxX ?: allPoints.maxOf(TrackMapPoint::x)
        val maxY = trackMap.bounds?.maxY ?: allPoints.maxOf(TrackMapPoint::y)
        if (maxX <= minX || maxY <= minY) return null

        return SessionAnalysisTrackMap(
            points = trackMap.points.map(::mapPoint).toImmutableList(),
            pitPoints = trackMap.pitPoints.map(::mapPoint).toImmutableList(),
            idealPoints = trackMap.idealLinePoints.map(::mapPoint).toImmutableList(),
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    private fun mapPoint(point: TrackMapPoint): SessionAnalysisTrackMapPoint = SessionAnalysisTrackMapPoint(
        x = point.x,
        y = point.y,
        leftWidthMeters = point.leftWidthMeters,
        rightWidthMeters = point.rightWidthMeters,
    )
}
