package com.analyzer.session.data.analysis

import com.analyzer.session.data.model.LapSummary

internal data class IndexAnalysis(
    val laps: List<LapSummary>,
    val distanceKm: Double,
)
