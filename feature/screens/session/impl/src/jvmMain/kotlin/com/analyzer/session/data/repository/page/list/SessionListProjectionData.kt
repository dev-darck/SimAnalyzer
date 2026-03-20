package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.model.RecordedSessionSummary

internal data class SessionListProjectionData(
    val items: List<RecordedSessionSummary>,
    val error: String?,
)
