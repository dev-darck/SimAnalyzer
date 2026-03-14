package com.analyzer.session.presentation.model

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

data class SessionFilterUiModel(
    val kind: SessionFilterKind,
    val selectedId: String,
    val options: PersistentList<SessionFilterOptionUi> = persistentListOf(),
)
