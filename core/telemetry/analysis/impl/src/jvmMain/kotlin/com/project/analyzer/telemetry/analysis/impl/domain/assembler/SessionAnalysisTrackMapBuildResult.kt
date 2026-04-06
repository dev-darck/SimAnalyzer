package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap

internal data class SessionAnalysisTrackMapBuildResult(
    val trackMap: SessionAnalysisTrackMap?,
    val sourceLapNumber: Int?,
) {

    companion object {

        val EMPTY = SessionAnalysisTrackMapBuildResult(
            trackMap = null,
            sourceLapNumber = null,
        )
    }
}
