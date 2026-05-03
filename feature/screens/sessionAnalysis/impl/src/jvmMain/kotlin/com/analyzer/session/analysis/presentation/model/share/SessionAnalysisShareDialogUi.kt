package com.analyzer.session.analysis.presentation.model.share

internal data class SessionAnalysisShareDialogUi(
    val isVisible: Boolean = false,
    val title: String = "",
    val supportingText: String = "",
    val summaryText: String = "",
    val reportFileName: String = "",
)
