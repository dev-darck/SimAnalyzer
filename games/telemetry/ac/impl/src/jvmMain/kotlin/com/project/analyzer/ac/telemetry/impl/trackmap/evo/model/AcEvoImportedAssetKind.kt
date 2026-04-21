package com.project.analyzer.ac.telemetry.impl.trackmap.evo.model

import kotlinx.serialization.Serializable

@Serializable
internal enum class AcEvoImportedAssetKind {

    SPLINEDATA_JSON,
    IDEAL_LINE_AI,
    PITLANE_AI,
    TRACK_CONTROL_POINTS,
    TRACK_LAYOUT,
    TRACKMAP_SVG,
}
