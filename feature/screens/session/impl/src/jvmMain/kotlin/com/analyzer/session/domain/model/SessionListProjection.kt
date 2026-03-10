package com.analyzer.session.domain.model

data class SessionListProjection(
    val rows: List<SessionListDomainItem>,
    val visibleRows: List<SessionListDomainItem>,
    val page: Int,
    val pageCount: Int,
    val error: String?,
)
