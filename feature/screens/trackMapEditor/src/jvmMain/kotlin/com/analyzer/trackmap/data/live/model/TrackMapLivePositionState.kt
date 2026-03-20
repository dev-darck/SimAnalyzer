package com.analyzer.trackmap.data.live.model

import com.project.analyzer.math.Vec2

internal data class TrackMapLivePositionState(
    val position: Vec2? = null,
    val source: TrackMapLivePositionSource? = null,
    val lastMatchedAtNs: Long = 0L,
    val lastMismatchAtNs: Long = 0L,
) {

    fun matched(
        position: Vec2,
        source: TrackMapLivePositionSource,
        nowNs: Long,
    ): TrackMapLivePositionState = copy(
        position = position,
        source = source,
        lastMatchedAtNs = nowNs,
        lastMismatchAtNs = 0L,
    )

    fun takeMatchedGrace(nowNs: Long): TrackMapLivePositionState {
        val keepVisible = position != null && nowNs - lastMatchedAtNs <= TRACK_MATCH_GRACE_PERIOD_NS
        return if (keepVisible) {
            this
        } else {
            cleared()
        }
    }

    fun cleared(lastMismatchAtNs: Long = 0L): TrackMapLivePositionState = copy(
        position = null,
        source = null,
        lastMatchedAtNs = 0L,
        lastMismatchAtNs = lastMismatchAtNs,
    )
}

internal const val TRACK_MATCH_GRACE_PERIOD_NS = 3_000_000_000L
internal const val TRACK_MISMATCH_GRACE_PERIOD_NS = 5_000_000_000L
internal const val TRACK_REANCHOR_MIN_JUMP_METERS = 12f
internal const val TRACK_REANCHOR_MIN_SPEED_KMH = 15f
