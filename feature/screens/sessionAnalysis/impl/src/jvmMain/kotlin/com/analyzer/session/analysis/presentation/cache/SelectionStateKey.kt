package com.analyzer.session.analysis.presentation.cache

internal data class SelectionStateKey(
    val segmentId: Long?,
    val lapNumber: Int?,
    val referenceSessionId: Long?,
    val referenceSegmentId: Long?,
    val referenceLapNumber: Int?,
)
