package com.analyzer.session.analysis.presentation.builder.coach

internal data class BalanceStats(
    val label: String,
    val description: String,
    val dominantRatio: Float? = null,
    val deltaToReference: Float = 0f,
)
