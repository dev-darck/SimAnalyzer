package com.analyzer.session.domain.model

data class SessionListPage(
    val stats: SessionListDomainStats = SessionListDomainStats(),
    val gameOptions: List<SessionFilterOption> = emptyList(),
    val trackOptions: List<SessionFilterOption> = emptyList(),
    val carOptions: List<SessionFilterOption> = emptyList(),
    val dateOptions: List<SessionFilterOption> = emptyList(),
    val sortOptions: List<SessionFilterOption> = emptyList(),
    val rows: List<SessionListDomainItem> = emptyList(),
    val page: Int = 1,
    val pageCount: Int = 1,
    val error: String? = null,
)
