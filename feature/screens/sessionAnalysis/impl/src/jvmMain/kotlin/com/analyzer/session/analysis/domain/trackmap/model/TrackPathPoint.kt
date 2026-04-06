package com.analyzer.session.analysis.domain.trackmap.model

import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint

internal data class TrackPathPoint(val fraction: Float, val point: SessionAnalysisTrackMapPoint)
