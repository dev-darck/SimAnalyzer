package com.analyzer.session.analysis.presentation

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData

internal data class SessionBinding(
    val requestedSessionId: Long? = null,
    val loadedSessionId: Long? = null,
    val data: SessionAnalysisWorkspaceData? = null,
)
