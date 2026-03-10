package com.analyzer.session.presentation.model

data class SessionFilterUiModel(
    val kind: SessionFilterKind,
    val selectedId: String,
    val options: List<SessionFilterOptionUi> = emptyList(),
)
