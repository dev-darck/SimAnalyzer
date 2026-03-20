package com.analyzer.session.data.model

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.RecordedSessionSummary
import java.io.File

internal data class SessionLocation(
    val summary: RecordedSessionSummary,
    val dir: File,
    val metadata: RecordedSessionMetadata,
    val analysis: IndexAnalysis? = null,
)
