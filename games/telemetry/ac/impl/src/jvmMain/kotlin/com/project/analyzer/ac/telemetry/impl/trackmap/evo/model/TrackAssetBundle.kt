package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import java.nio.file.Path

internal data class TrackAssetBundle(
    val trackId: String,
    val trackFolder: String,
    val layoutId: String?,
    val splineJsonPath: Path?,
    val idealLinePath: Path?,
    val pitlanePath: Path?,
    val controlPointsPath: Path?,
    val trackLayoutPath: Path?,
    val trackMapSvgPath: Path?,
)
