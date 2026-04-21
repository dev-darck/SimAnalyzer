package com.analyzer.trackmap.data.live.engine

import com.analyzer.trackmap.data.live.geometry.TrackMapLivePositionProjector
import com.analyzer.trackmap.data.live.model.TRACK_MISMATCH_GRACE_PERIOD_NS
import com.analyzer.trackmap.data.live.model.TRACK_REANCHOR_MIN_JUMP_METERS
import com.analyzer.trackmap.data.live.model.TRACK_REANCHOR_MIN_SPEED_KMH
import com.analyzer.trackmap.data.live.model.TrackMapLivePositionCandidates
import com.analyzer.trackmap.data.live.model.TrackMapLivePositionMatchResult
import com.analyzer.trackmap.data.live.model.TrackMapLivePositionSource
import com.analyzer.trackmap.data.live.model.TrackMapLivePositionState
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.TrackIdentityAliasMatcher
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger

internal class TrackMapLivePositionResolver(private val item: TrackMapLibraryItem) {

    private val livePositionLogger = logger()
    private val projector = TrackMapLivePositionProjector(item)

    private var state = TrackMapLivePositionState()

    fun resolve(frame: TelemetryFrame): Vec2? {
        val nowNs = System.nanoTime()
        val speedKmh = frame.car?.speedKmh ?: 0f
        val candidates = projector.buildCandidates(
            frame = frame,
            lastVisiblePosition = state.position,
            speedKmh = speedKmh,
        )

        state = when (matchTrack(frame)) {
            TrackMapLivePositionMatchResult.MATCH -> resolveMatchedState(
                frame = frame,
                candidates = candidates,
                speedKmh = speedKmh,
                nowNs = nowNs,
            )

            TrackMapLivePositionMatchResult.AMBIGUOUS -> resolveAmbiguousState(
                frame = frame,
                candidates = candidates,
                nowNs = nowNs,
            )

            TrackMapLivePositionMatchResult.MISMATCH -> resolveMismatchState(
                geometryFallbackPosition = candidates.geometryFallbackPosition,
                nowNs = nowNs,
            )
        }

        return state.position
    }

    private fun matchTrack(frame: TelemetryFrame): TrackMapLivePositionMatchResult {
        val trackInfo = frame.session?.track ?: return TrackMapLivePositionMatchResult.AMBIGUOUS

        return if (
            TrackIdentityAliasMatcher.areEquivalent(
                trackId = item.map.trackId,
                layoutId = item.map.layoutId,
                otherTrackId = trackInfo.trackId,
                otherLayoutId = trackInfo.layoutId,
            )
        ) {
            TrackMapLivePositionMatchResult.MATCH
        } else {
            livePositionLogger.atDebug(RATE_LIMITED) {
                message = buildString {
                    append("TrackMap live mismatch ")
                    append("itemTrack=").append(item.map.trackId)
                    append(" itemLayout=").append(item.map.layoutId.orEmpty())
                    append(" frameTrack=").append(trackInfo.trackId.orEmpty())
                    append(" frameLayout=").append(trackInfo.layoutId.orEmpty())
                }
            }
            TrackMapLivePositionMatchResult.MISMATCH
        }
    }

    private fun resolveMatchedState(
        frame: TelemetryFrame,
        candidates: TrackMapLivePositionCandidates,
        speedKmh: Float,
        nowNs: Long,
    ): TrackMapLivePositionState = when {
        candidates.acceptedPosition != null -> state.matched(
            position = candidates.acceptedPosition,
            source = TrackMapLivePositionSource.RAW,
            nowNs = nowNs,
        )

        shouldReanchor(candidates = candidates, speedKmh = speedKmh) -> {
            logReanchoredPosition(
                frame = frame,
                speedKmh = speedKmh,
                rawPosition = candidates.rawPosition,
                projectedPosition = candidates.rawProjection!!.point,
            )
            state.matched(
                position = candidates.rawProjection.point,
                source = TrackMapLivePositionSource.RAW,
                nowNs = nowNs,
            )
        }

        candidates.lapProgressPosition != null -> state.matched(
            position = candidates.lapProgressPosition,
            source = TrackMapLivePositionSource.LAP_PROGRESS,
            nowNs = nowNs,
        )

        candidates.rawPosition != null -> {
            logRejectedJump(
                frame = frame,
                speedKmh = speedKmh,
                candidates = candidates,
            )
            state
        }

        else -> {
            logMissingMatchedPosition(
                frame = frame,
                speedKmh = speedKmh,
                candidates = candidates,
            )
            state.takeMatchedGrace(nowNs = nowNs)
        }
    }

    private fun resolveAmbiguousState(
        frame: TelemetryFrame,
        candidates: TrackMapLivePositionCandidates,
        nowNs: Long,
    ): TrackMapLivePositionState = when {
        candidates.geometryFallbackPosition != null -> state.matched(
            position = candidates.geometryFallbackPosition,
            source = TrackMapLivePositionSource.GEOMETRY_FALLBACK,
            nowNs = nowNs,
        )

        candidates.lapProgressPosition != null -> state.matched(
            position = candidates.lapProgressPosition,
            source = TrackMapLivePositionSource.LAP_PROGRESS,
            nowNs = nowNs,
        )

        else -> {
            livePositionLogger.atDebug(RATE_LIMITED) {
                message = buildString {
                    append("TrackMap live ambiguous frame ")
                    append("itemTrack=").append(item.map.trackId)
                    append(" itemLayout=").append(item.map.layoutId.orEmpty())
                    append(" frameTrack=").append(frame.session?.track?.trackId.orEmpty())
                    append(" frameLayout=").append(frame.session?.track?.layoutId.orEmpty())
                    append(" lap=").append(frame.session?.track?.normalizedLapPosition)
                    append(" world=").append(frame.car?.worldPosition)
                }
            }
            state
        }
    }

    private fun resolveMismatchState(geometryFallbackPosition: Vec2?, nowNs: Long): TrackMapLivePositionState {
        if (geometryFallbackPosition != null) {
            return state.matched(
                position = geometryFallbackPosition,
                source = TrackMapLivePositionSource.GEOMETRY_FALLBACK,
                nowNs = nowNs,
            )
        }

        val keepVisible = state.position != null &&
            (state.lastMismatchAtNs == 0L || nowNs - state.lastMismatchAtNs <= TRACK_MISMATCH_GRACE_PERIOD_NS)

        return if (keepVisible) {
            state.copy(lastMismatchAtNs = nowNs)
        } else {
            state.cleared(lastMismatchAtNs = nowNs)
        }
    }

    private fun shouldReanchor(candidates: TrackMapLivePositionCandidates, speedKmh: Float): Boolean {
        val projection = candidates.rawProjection ?: return false
        val previous = state.position ?: return false
        if (!projector.isNearTrack(projection)) return false
        if (state.source != TrackMapLivePositionSource.RAW) return true
        if (speedKmh < TRACK_REANCHOR_MIN_SPEED_KMH) return false
        return projection.point.distanceTo(previous) >= TRACK_REANCHOR_MIN_JUMP_METERS
    }

    private fun logReanchoredPosition(
        frame: TelemetryFrame,
        speedKmh: Float,
        rawPosition: Vec2?,
        projectedPosition: Vec2,
    ) {
        livePositionLogger.atDebug(RATE_LIMITED) {
            message = buildString {
                append("TrackMap live reanchored ")
                append("trackId=").append(frame.session?.track?.trackId.orEmpty())
                append(" layoutId=").append(frame.session?.track?.layoutId.orEmpty())
                append(" speed=").append(speedKmh)
                append(" raw=").append(rawPosition)
                append(" projected=").append(projectedPosition)
                append(" last=").append(state.position)
            }
        }
    }

    private fun logRejectedJump(frame: TelemetryFrame, speedKmh: Float, candidates: TrackMapLivePositionCandidates) {
        livePositionLogger.atDebug(RATE_LIMITED) {
            message = buildString {
                append("TrackMap live rejected jump ")
                append("trackId=").append(frame.session?.track?.trackId.orEmpty())
                append(" layoutId=").append(frame.session?.track?.layoutId.orEmpty())
                append(" speed=").append(speedKmh)
                append(" raw=").append(candidates.rawPosition)
                append(" projected=").append(candidates.rawProjection?.point)
                append(" last=").append(state.position)
                append(" world=").append(candidates.worldPosition)
                append(" wheel=").append(candidates.wheelReferencePosition)
            }
        }
    }

    private fun logMissingMatchedPosition(
        frame: TelemetryFrame,
        speedKmh: Float,
        candidates: TrackMapLivePositionCandidates,
    ) {
        livePositionLogger.atDebug(RATE_LIMITED) {
            message = buildString {
                append("TrackMap live position missing on matched frame ")
                append("trackId=").append(frame.session?.track?.trackId.orEmpty())
                append(" layoutId=").append(frame.session?.track?.layoutId.orEmpty())
                append(" speed=").append(speedKmh)
                append(" lap=").append(frame.session?.track?.normalizedLapPosition)
                append(" world=").append(frame.car?.worldPosition)
                append(" wheel=").append(candidates.wheelReferencePosition ?: candidates.worldPosition)
            }
        }
    }
}
