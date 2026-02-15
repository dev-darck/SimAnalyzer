package com.project.analyzer.telemetry.ac.api.model.trackmap

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import kotlinx.serialization.Serializable

@Serializable
public data class TrackMapPoint(
    val x: Float,
    val y: Float,
    val leftWidthMeters: Float = 0f,
    val rightWidthMeters: Float = 0f
) {

    public fun toVec2(): Vec2 = Vec2(x, y)

    public companion object {

        public fun from(
            v: Vec2,
            leftWidthMeters: Float = 0f,
            rightWidthMeters: Float = 0f
        ): TrackMapPoint = TrackMapPoint(
            x = v.x,
            y = v.y,
            leftWidthMeters = leftWidthMeters,
            rightWidthMeters = rightWidthMeters
        )
    }
}

@Serializable
public data class TrackMapBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {

    public fun width(): Float = maxX - minX
    public fun height(): Float = maxY - minY
}

@Serializable
public data class TrackMap(
    val gameId: String = "",
    val trackId: String,
    val trackName: String,
    val layoutId: String? = null,
    val createdAtEpochMs: Long,
    val referencePoint: ReferencePoint = ReferencePoint.FRONT_AXLE,
    val points: List<TrackMapPoint>,
    val pitPoints: List<TrackMapPoint> = emptyList(),
    val bounds: TrackMapBounds? = null,
    val pitEntryIndex: Int = -1,
    val pitExitIndex: Int = -1,
)
