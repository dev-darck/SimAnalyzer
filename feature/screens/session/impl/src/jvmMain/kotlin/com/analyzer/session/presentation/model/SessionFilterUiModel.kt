package com.analyzer.session.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SessionFilterUiModel(
    val kind: SessionFilterKind,
    val selectedId: String,
    val options: ImmutableList<SessionFilterOptionUi> = persistentListOf(),
)
