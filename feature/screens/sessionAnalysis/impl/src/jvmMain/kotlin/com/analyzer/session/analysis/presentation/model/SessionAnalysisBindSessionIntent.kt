package com.analyzer.session.analysis.presentation.model

data class SessionAnalysisBindSessionIntent(
    val sessionId: Long,
    val segmentId: Long? = null,
    val lapNumber: Int? = null,
    val referenceSessionId: Long? = null,
    val referenceSegmentId: Long? = null,
    val referenceLapNumber: Int? = null,
) : SessionAnalysisIntent
