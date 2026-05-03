package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData

internal data class SessionBinding(
    val requestedSessionId: Long? = null,
    val loadedSessionId: Long? = null,
    val referenceSessionId: Long? = null,
    val referenceSegmentId: Long? = null,
    val selectedSegmentId: Long? = null,
    val selectedLapNumber: Int? = null,
    val referenceLapNumber: Int? = null,
    val data: SessionAnalysisWorkspaceData? = null,
)
