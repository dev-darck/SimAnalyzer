package com.analyzer.trackmap.builder.recording.runtime

import com.project.analyzer.math.Vec2
import com.project.analyzer.utils.toSlugId

internal fun shouldRefreshSessionInfo(
    timestampNs: Long,
    lastInfoUpdateNs: Long,
    snapshot: TrackMapRecorderState,
    trackId: String?,
    trackName: String?,
    layoutId: String?,
    infoUpdateIntervalNs: Long,
): Boolean {
    if ((timestampNs - lastInfoUpdateNs) >= infoUpdateIntervalNs) return true

    val normalizedTrackId = trackId?.trim().takeUnless { it.isNullOrBlank() }
    if (normalizedTrackId != null && normalizedTrackId != snapshot.trackId) return true

    val normalizedTrackName = trackName?.trim().takeUnless { it.isNullOrBlank() }
    if (normalizedTrackName != null && normalizedTrackName != snapshot.trackName) return true

    return layoutId != null && layoutId != snapshot.layoutId
}

internal fun applySessionInfo(
    current: TrackMapRecorderState,
    trackId: String?,
    trackName: String?,
    layoutId: String?,
    speed: Float?,
    position: Vec2?,
): TrackMapRecorderState {
    val rawTrackId = trackId?.trim().orEmpty()
    val rawTrackName = trackName?.trim().orEmpty()

    val resolvedTrackName = when {
        rawTrackName.isNotBlank() -> rawTrackName
        current.trackName.isNotBlank() -> current.trackName
        rawTrackId.isNotBlank() -> rawTrackId
        else -> ""
    }

    val resolvedTrackId = when {
        rawTrackId.isNotBlank() -> rawTrackId
        current.trackId.isNotBlank() -> current.trackId
        resolvedTrackName.isNotBlank() -> slugifyTrackId(resolvedTrackName)
        else -> ""
    }

    val resolvedLayoutId = layoutId ?: current.layoutId

    if (
        resolvedTrackId == current.trackId &&
        resolvedTrackName == current.trackName &&
        resolvedLayoutId == current.layoutId &&
        speed == current.currentSpeedKmh &&
        position == current.currentPosition
    ) {
        return current
    }

    return current.copy(
        trackId = resolvedTrackId,
        trackName = resolvedTrackName,
        layoutId = resolvedLayoutId,
        currentSpeedKmh = speed,
        currentPosition = position,
    )
}

internal fun resolveTrackIdForSave(state: TrackMapRecorderState): String {
    val fromTelemetry = state.trackId.trim()
    if (fromTelemetry.isNotBlank()) return fromTelemetry
    return slugifyTrackId(state.trackName)
}

private fun slugifyTrackId(text: String): String = text.toSlugId()
