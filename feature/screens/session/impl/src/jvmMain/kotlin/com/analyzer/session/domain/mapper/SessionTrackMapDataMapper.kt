package com.analyzer.session.domain.mapper

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.ui.components.TrackMapBounds
import com.project.analyzer.ui.components.TrackMapData
import com.project.analyzer.ui.components.TrackMapPoint
import com.project.analyzer.utils.trackmap.TrackMapPreparationUtil
import com.project.analyzer.utils.trackmap.TrackMapPreparedBounds
import com.project.analyzer.utils.trackmap.TrackMapPreparedPoint
import kotlinx.collections.immutable.toImmutableList
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint as SourceTrackMapPoint
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint as SourceSessionTrackMapPoint

internal fun TrackMap.toTrackMapData(trackMapPreparationUtil: TrackMapPreparationUtil): TrackMapData? {
    val minimapPoints = idealLinePoints.takeIf { points -> points.hasRenderableSourcePoints() } ?: points
    val prepared = trackMapPreparationUtil.prepare(
        points = minimapPoints.map(SourceTrackMapPoint::toPreparedPoint),
        pitPoints = emptyList(),
        bounds = null,
    ) ?: return null

    return TrackMapData(
        points = prepared.points.map(TrackMapPreparedPoint::toUiPoint).toImmutableList(),
        pitPoints = emptyList<TrackMapPoint>().toImmutableList(),
        bounds = prepared.bounds.toUiBounds(),
    )
}

internal fun SessionAnalysisTrackMap.toTrackMapData(trackMapPreparationUtil: TrackMapPreparationUtil): TrackMapData? {
    val minimapPoints = idealPoints.takeIf { points -> points.hasRenderableSessionPoints() } ?: points
    val prepared = trackMapPreparationUtil.prepare(
        points = minimapPoints.map(SourceSessionTrackMapPoint::toPreparedPoint),
        pitPoints = pitPoints.map(SourceSessionTrackMapPoint::toPreparedPoint),
        bounds = TrackMapPreparedBounds(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        ),
    ) ?: return null

    return TrackMapData(
        points = prepared.points.map(TrackMapPreparedPoint::toUiPoint).toImmutableList(),
        pitPoints = prepared.pitPoints.map(TrackMapPreparedPoint::toUiPoint).toImmutableList(),
        bounds = prepared.bounds.toUiBounds(),
    )
}

private fun SourceTrackMapPoint.toPreparedPoint(): TrackMapPreparedPoint = TrackMapPreparedPoint(
    x = x,
    y = y,
    leftWidthMeters = leftWidthMeters,
    rightWidthMeters = rightWidthMeters,
)

private fun SourceSessionTrackMapPoint.toPreparedPoint(): TrackMapPreparedPoint = TrackMapPreparedPoint(
    x = x,
    y = y,
    leftWidthMeters = leftWidthMeters ?: 0f,
    rightWidthMeters = rightWidthMeters ?: 0f,
)

private fun TrackMapPreparedPoint.toUiPoint(): TrackMapPoint = TrackMapPoint(
    x = x,
    y = y,
    leftWidthMeters = leftWidthMeters,
    rightWidthMeters = rightWidthMeters,
)

private fun TrackMapPreparedBounds.toUiBounds(): TrackMapBounds = TrackMapBounds(
    minX = minX,
    minY = minY,
    maxX = maxX,
    maxY = maxY,
)

private fun List<SourceTrackMapPoint>.hasRenderableSourcePoints(): Boolean =
    count { point -> point.x.isFinite() && point.y.isFinite() } >= 2

private fun List<SourceSessionTrackMapPoint>.hasRenderableSessionPoints(): Boolean =
    count { point -> point.x.isFinite() && point.y.isFinite() } >= 2
