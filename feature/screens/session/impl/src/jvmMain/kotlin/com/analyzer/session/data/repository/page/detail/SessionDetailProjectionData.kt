package com.analyzer.session.data.repository.page.detail

import com.analyzer.session.data.model.LapSummary

internal data class SessionDetailProjectionData(
    val laps: List<LapSummary>,
    val error: String?,
)
