package com.analyzer.session.details.presentation.model

data class SessionDetailShareDialogUi(
    val isVisible: Boolean = false,
    val title: String = "",
    val supportingText: String = "",
    val summaryText: String = "",
    val reportFileName: String = "",
)
