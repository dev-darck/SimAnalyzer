package com.analyzer.session.details.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SessionDetailFilterUiModel(
    val kind: SessionDetailFilterKind,
    val selectedId: String,
    val options: ImmutableList<SessionDetailFilterOptionUi> = persistentListOf(),
)
