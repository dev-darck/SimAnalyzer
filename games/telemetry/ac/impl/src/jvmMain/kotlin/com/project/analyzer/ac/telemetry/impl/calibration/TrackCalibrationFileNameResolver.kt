package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer

internal object TrackCalibrationFileNameResolver {

    const val DIRECTORY_NAME: String = "track_calibrations"
    const val INDEX_FILE_NAME: String = "index.txt"

    fun resolveFileName(trackId: String, layoutId: String?): String {
        val storageTrackId = resolveStorageTrackId(trackId = trackId, layoutId = layoutId)
        return "$storageTrackId.json"
    }

    fun resolveStorageTrackId(trackId: String, layoutId: String?): String {
        val normalizedTrackId = trackId.trim().takeIf { it.isNotBlank() } ?: return ""
        val normalized = TrackIdNormalizer.normalize(
            track = normalizedTrackId,
            layout = TrackIdNormalizer.normalizeLayoutId(layoutId),
        )
        return normalized.takeIf { it.isNotBlank() } ?: normalizedTrackId
    }

    fun buildTrackIdCandidates(trackId: String, layoutId: String?): List<String> {
        val normalizedTrackId = trackId.trim().takeIf { it.isNotBlank() } ?: return emptyList()
        val candidates = linkedSetOf(normalizedTrackId)
        val normalizedLayoutId = TrackIdNormalizer.normalizeLayoutId(layoutId)
        val combinedTrackId = resolveStorageTrackId(
            trackId = normalizedTrackId,
            layoutId = normalizedLayoutId,
        )
        if (combinedTrackId.isNotBlank()) {
            candidates += combinedTrackId
        }
        if (normalizedLayoutId != null && normalizedTrackId.endsWith("_$normalizedLayoutId")) {
            normalizedTrackId
                .removeSuffix("_$normalizedLayoutId")
                .trimEnd('_')
                .takeIf { it.isNotBlank() }
                ?.let(candidates::add)
        }
        return candidates.toList()
    }

    fun buildCacheKey(trackId: String, layoutId: String?): String {
        val storageTrackId = resolveStorageTrackId(trackId = trackId, layoutId = layoutId)
            .ifBlank { trackId.trim() }
        val normalizedLayoutId = TrackIdNormalizer.normalizeLayoutId(layoutId).orEmpty()
        return "$storageTrackId|$normalizedLayoutId"
    }
}
