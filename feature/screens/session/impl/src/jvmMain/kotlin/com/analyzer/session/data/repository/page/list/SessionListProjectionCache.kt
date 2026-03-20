package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.model.RecordedSessionSummary

internal data class SessionListProjectionCache(
    val source: List<RecordedSessionSummary>,
    val key: SessionListProjectionKey,
    val data: SessionListProjectionData,
)
