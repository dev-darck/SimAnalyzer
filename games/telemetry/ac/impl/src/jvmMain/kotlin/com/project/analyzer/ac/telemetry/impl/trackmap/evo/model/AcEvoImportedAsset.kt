package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import kotlinx.serialization.Serializable

@Serializable
internal data class AcEvoImportedAsset(
    val relativePath: String,
    val kind: AcEvoImportedAssetKind,
    val trackFolder: String? = null,
    val layoutId: String? = null,
    val trackId: String? = null,
)
