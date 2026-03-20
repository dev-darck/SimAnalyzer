package com.analyzer.session.data.repository.page.list

import com.analyzer.session.data.model.RecordedSessionListStats
import com.analyzer.session.data.model.RecordedSessionOption

internal data class SessionListBaseData(
    val stats: RecordedSessionListStats,
    val gameOptions: List<RecordedSessionOption>,
    val trackOptions: List<RecordedSessionOption>,
    val carOptions: List<RecordedSessionOption>,
    val dateOptions: List<RecordedSessionOption>,
)
