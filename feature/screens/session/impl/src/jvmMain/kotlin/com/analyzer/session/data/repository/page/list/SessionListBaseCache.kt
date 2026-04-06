package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.model.RecordedSessionSummary

internal data class SessionListBaseCache(val source: List<RecordedSessionSummary>, val data: SessionListBaseData)
