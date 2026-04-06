package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

internal data class AcEvoImportedTrackMapCacheKey(
    val packagePath: String,
    val packageSizeBytes: Long,
    val packageLastModifiedEpochMs: Long,
    val assetCount: Int,
)
