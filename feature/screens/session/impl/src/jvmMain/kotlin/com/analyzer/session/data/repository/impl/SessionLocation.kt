package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.RecordedSessionSummary
import java.io.File

internal data class SessionLocation(
    val summary: RecordedSessionSummary,
    val dir: File,
    val metadata: RecordedSessionMetadata,
    val analysis: IndexAnalysis? = null,
)

internal data class SessionBundleLocation(val summary: RecordedSessionSummary, val locations: List<SessionLocation>)
