package com.analyzer.session.details.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SessionDetailCompareSessionPickerUi(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val title: String = "",
    val supportingText: String = "",
    val statusMessage: String? = null,
    val emptyMessage: String? = null,
    val candidates: ImmutableList<SessionDetailCompareSessionCandidateUi> = persistentListOf(),
)
