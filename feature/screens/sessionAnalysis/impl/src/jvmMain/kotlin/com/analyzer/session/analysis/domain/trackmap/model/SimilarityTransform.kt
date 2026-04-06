package com.analyzer.session.analysis.domain.trackmap.model

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint

internal data class SimilarityTransform(
    val scale: Float,
    val rotationCos: Float,
    val rotationSin: Float,
    val translateX: Float,
    val translateY: Float,
) {

    fun map(x: Float, y: Float): Pair<Float, Float> {
        val rotatedX = rotationCos * x - rotationSin * y
        val rotatedY = rotationSin * x + rotationCos * y
        return rotatedX * scale + translateX to rotatedY * scale + translateY
    }

    fun mapPoint(point: SessionAnalysisTrackMapPoint): SessionAnalysisTrackMapPoint {
        val mapped = map(x = point.x, y = point.y)
        return point.copy(x = mapped.first, y = mapped.second)
    }
}
