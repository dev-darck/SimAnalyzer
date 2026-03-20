package com.analyzer.session.data.model

import com.analyzer.session.data.model.RecordedSessionSummary

internal data class SessionBundleLocation(
    val summary: RecordedSessionSummary,
    val locations: List<SessionLocation>,
)
