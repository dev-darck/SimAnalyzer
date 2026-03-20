package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource

internal fun TrackCalibration.normalizeForStorage(source: TrackCalibrationSource): TrackCalibration {
    val normalizedLayoutId = normalizeCalibrationLayoutId(layoutId)
    val normalizedTrackId = canonicalCalibrationTrackId(trackId = trackId, layoutId = normalizedLayoutId)
    return copy(
        trackId = normalizedTrackId,
        layoutId = normalizedLayoutId,
        source = source,
    )
}

internal fun TrackCalibration.normalizeForRead(): TrackCalibration {
    val normalizedLayoutId = normalizeCalibrationLayoutId(layoutId)
    val normalizedTrackId = canonicalCalibrationTrackId(trackId = trackId, layoutId = normalizedLayoutId)
    return copy(
        trackId = normalizedTrackId,
        layoutId = normalizedLayoutId,
    )
}

internal fun TrackCalibration.withResolvedLayout(layoutId: String?): TrackCalibration {
    val resolvedLayoutId = layoutId ?: normalizeCalibrationLayoutId(this.layoutId)
    return if (resolvedLayoutId == this.layoutId) {
        this
    } else {
        copy(layoutId = resolvedLayoutId)
    }
}

internal fun normalizeCalibrationLayoutId(layoutId: String?): String? =
    TrackIdNormalizer.normalizeLayoutId(layoutId)

private fun canonicalCalibrationTrackId(trackId: String, layoutId: String?): String {
    val normalizedTrackId = trackId.trim()
    return TrackCalibrationFileNameResolver.resolveStorageTrackId(
        trackId = normalizedTrackId,
        layoutId = layoutId,
    ).ifBlank { normalizedTrackId }
}
