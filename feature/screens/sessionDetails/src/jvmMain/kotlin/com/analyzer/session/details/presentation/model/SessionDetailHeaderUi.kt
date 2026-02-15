package com.analyzer.session.details.presentation.model

data class SessionDetailHeaderUi(
    val title: String = "Session",
    val subtitle: String = "",
    val chips: List<String> = emptyList(),
)
