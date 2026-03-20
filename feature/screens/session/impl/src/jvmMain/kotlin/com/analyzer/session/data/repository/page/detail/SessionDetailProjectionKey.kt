package com.analyzer.session.data.repository.page.detail

import com.analyzer.session.data.repository.RecordedSessionLapShow
import com.analyzer.session.data.repository.RecordedSessionLapSort

internal data class SessionDetailProjectionKey(
    val sort: RecordedSessionLapSort,
    val show: RecordedSessionLapShow,
    val selectedSessionTypeId: String?,
)
