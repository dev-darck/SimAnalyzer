package com.analyzer.session.details.domain.usecase

import com.analyzer.session.details.domain.model.SessionDetailPageResult
import com.analyzer.session.details.domain.model.SessionDetailQuery

interface SessionDetailDataUseCase {

    suspend fun loadPage(
        sessionId: Long,
        query: SessionDetailQuery,
        forceRefresh: Boolean = false,
    ): SessionDetailPageResult?
}
