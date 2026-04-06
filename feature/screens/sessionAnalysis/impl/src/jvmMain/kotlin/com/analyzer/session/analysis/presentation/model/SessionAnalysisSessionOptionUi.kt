package com.analyzer.session.analysis.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisSessionOptionUi(
    val segmentId: Long,
    val primaryLabel: String,
    val supportingLabel: String,
)
