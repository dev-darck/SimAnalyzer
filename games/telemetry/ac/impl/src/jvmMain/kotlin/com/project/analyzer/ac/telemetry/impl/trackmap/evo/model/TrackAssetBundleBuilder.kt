package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import java.nio.file.Path

internal data class TrackAssetBundleBuilder(
    val trackId: String,
    val trackFolder: String,
    val layoutId: String?,
    var splineJsonPath: Path? = null,
    var idealLinePath: Path? = null,
    var pitlanePath: Path? = null,
    var controlPointsPath: Path? = null,
    var trackLayoutPath: Path? = null,
    var trackMapSvgPath: Path? = null,
) {

    fun toImmutable(): TrackAssetBundle = TrackAssetBundle(
        trackId = trackId,
        trackFolder = trackFolder,
        layoutId = layoutId,
        splineJsonPath = splineJsonPath,
        idealLinePath = idealLinePath,
        pitlanePath = pitlanePath,
        controlPointsPath = controlPointsPath,
        trackLayoutPath = trackLayoutPath,
        trackMapSvgPath = trackMapSvgPath,
    )
}
