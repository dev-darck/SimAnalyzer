package com.analyzer.session.domain.model

data class SessionListDataset(
    val items: List<SessionListDomainItem> = emptyList(),
    val stats: SessionListDomainStats = SessionListDomainStats(),
    val gameOptions: List<SessionFilterOption> = emptyList(),
    val trackOptions: List<SessionFilterOption> = emptyList(),
    val carOptions: List<SessionFilterOption> = emptyList(),
    val dateOptions: List<SessionFilterOption> = emptyList(),
    val sortOptions: List<SessionFilterOption> = emptyList(),
)
