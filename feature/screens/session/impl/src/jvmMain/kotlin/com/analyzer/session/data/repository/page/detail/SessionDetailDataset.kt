package com.analyzer.session.data.repository.page.detail

import com.analyzer.session.data.model.LapSummary
import com.analyzer.session.data.model.RecordedSessionDetailStats
import com.analyzer.session.data.model.RecordedSessionOption
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation

internal data class SessionDetailDataset(
    val source: SessionBundleLocation,
    val summary: RecordedSessionSummary,
    val laps: List<LapSummary>,
    val stats: RecordedSessionDetailStats,
    val sessionTypeOptions: List<RecordedSessionOption>,
    val firstLapNumber: Int?,
)
