package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.repository.RecordedSessionSummarySort

internal data class SessionListProjectionKey(
    val gameId: String?,
    val trackId: String?,
    val carId: String?,
    val dateId: String?,
    val searchQuery: String,
    val sort: RecordedSessionSummarySort,
)
