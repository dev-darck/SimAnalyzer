package com.analyzer.trackmap.builder.recording.engine

import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.calibration.ReferencePointPoseExtractor
import com.project.analyzer.telemetry.api.contract.SimStatus
import com.project.analyzer.telemetry.api.model.TelemetryFrame

internal data class TrackMapFrameContext(
    val safeSpeed: Float?,
    val sectorCount: Int?,
    val currentSectorIndex: Int?,
    val inPitLaneAuto: Boolean,
    val lapIndex: Int?,
    val timestampNs: Long,
    val currentPos: Vec2?,
    val trackId: String?,
    val trackName: String?,
    val layoutId: String?,
)

internal fun TelemetryFrame.toTrackMapFrameContext(poseExtractor: ReferencePointPoseExtractor): TrackMapFrameContext? {
    val session = session
    if (session?.status == SimStatus.REPLAY) return null

    val resolvedTimestampNs = timestampNs.takeIf { it > 0L } ?: System.nanoTime()
    val track = session?.track
    val sectorCount = lap?.sectorCount ?: track?.sectorCount
    return TrackMapFrameContext(
        safeSpeed = car?.speedKmh?.takeIf { it.isFinite() },
        sectorCount = sectorCount,
        currentSectorIndex = lap?.currentSectorIndex
            ?.takeIf { sectorCount == null || it in 0 until sectorCount.coerceAtLeast(1) },
        inPitLaneAuto = session?.pit?.isInPitLane == true || session?.pit?.isInPit == true,
        lapIndex = lap?.currentLapIndex
            ?: lap?.completedLaps?.let { it + 1 }
            ?: session?.completedLaps?.let { it + 1 },
        timestampNs = resolvedTimestampNs,
        currentPos = poseExtractor.extract(this, resolvedTimestampNs)
            ?.pos
            ?.takeIf { it.isFinite() },
        trackId = track?.trackId,
        trackName = track?.trackName,
        layoutId = track?.layoutId,
    )
}

private fun Vec2.isFinite(): Boolean = x.isFinite() && y.isFinite()
