package com.analyzer.session.analysis.domain.model

internal data class SessionAnalysisWorkspaceRequest(
    val sessionId: Long,
    val referenceSessionId: Long? = null,
)
