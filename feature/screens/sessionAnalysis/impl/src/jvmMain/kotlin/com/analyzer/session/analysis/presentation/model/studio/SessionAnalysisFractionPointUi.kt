package com.analyzer.session.analysis.presentation.model.studio

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionAnalysisFractionPointUi(
    val fraction: Float,
    val x: Float,
    val y: Float,
    val frameId: Long? = null,
)
